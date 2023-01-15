package berlin.yuna.natsserver.logic;

import berlin.yuna.clu.logic.SystemUtil;
import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.natsserver.config.NatsConfig;
import berlin.yuna.natsserver.model.MapValue;
import berlin.yuna.natsserver.model.ValueSource;
import berlin.yuna.natsserver.model.exception.NatsStartException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.PortUnreachableException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static berlin.yuna.clu.logic.SystemUtil.OS;
import static berlin.yuna.clu.model.OsType.OS_WINDOWS;
import static berlin.yuna.natsserver.config.NatsConfig.*;
import static berlin.yuna.natsserver.logic.NatsUtils.*;
import static berlin.yuna.natsserver.model.MapValue.mapValueOf;
import static berlin.yuna.natsserver.model.ValueSource.DEFAULT;
import static berlin.yuna.natsserver.model.ValueSource.DSL;
import static berlin.yuna.natsserver.model.ValueSource.ENV;
import static berlin.yuna.natsserver.model.ValueSource.FILE;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Logger.getLogger;

/**
 * {@link Nats}
 *
 * @author Yuna Morgenstern
 * @see SystemUtil
 * @see NatsInterface
 * @since 1.0
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "java:S1133"})
public class Nats extends NatsInterface {

    protected Terminal terminal;
    protected final String name;
    //TODO: Make final after removing start(timeoutMs)
    protected long timeoutMs;
    protected final Map<NatsConfig, MapValue> configMap = new ConcurrentHashMap<>();
    private static final String TMP_DIR = "java.io.tmpdir";
    public static final String NATS_PREFIX = "NATS_";

    /**
     * @throws IOException              if {@link Nats} is not found or unsupported on the {@link SystemUtil}
     * @throws BindException            if port is already taken
     * @throws PortUnreachableException if {@link Nats} is not starting cause port is not free
     */
    public Nats() throws Exception {
        this(NatsOptionsImpl.builder().autoStart(false).build());
    }

    /**
     * Starts the server if {@link NatsOptionsImpl#autoStart()} == true
     *
     * @param port the port to start on or &lt;=0 to use an automatically allocated port
     * @throws IOException              if {@link Nats} is not found or unsupported on the {@link SystemUtil}
     * @throws BindException            if port is already taken
     * @throws PortUnreachableException if {@link Nats} is not starting cause port is not free
     */
    public Nats(final int port) throws Exception {
        this(NatsOptionsImpl.builder().port(port).build());
    }

    /**
     * Starts the server if {@link NatsOptionsImpl#autoStart()} == true
     *
     * @throws IOException              if {@link Nats} is not found or unsupported on the {@link SystemUtil}
     * @throws BindException            if port is already taken
     * @throws PortUnreachableException if {@link Nats} is not starting cause port is not free
     */
    public Nats(final NatsOptions natsOptions) throws Exception {
        super(natsOptions);
        long timeoutMsTmp = -1;
        if (natsOptions instanceof NatsOptionsImpl) {
            ((NatsOptionsImpl) natsOptions).configMap().forEach(this::addConfig);
            timeoutMsTmp = ((NatsOptionsImpl) natsOptions).timeoutMs();
        }
        setDefaultConfig();
        setEnvConfig();
        setConfigFromProperties();
        setConfigFromNatsOptions(natsOptions);
        this.name = getValue(NATS_LOG_NAME);
        this.timeoutMs = timeoutMsTmp > -1 ? timeoutMsTmp : SECONDS.toMillis(10);
        if (natsOptions instanceof NatsOptionsImpl && ((NatsOptionsImpl) natsOptions).autoStart()) {
            start();
        }
    }

    /**
     * Starts the server if not already started e.g. by {@link NatsOptionsImpl#autoStart()} == true <br />
     *
     * @return {@link Nats}
     * @throws IOException              if {@link Nats} is not found or unsupported on the {@link SystemUtil}
     * @throws BindException            if port is already taken
     * @throws PortUnreachableException if {@link Nats} is not starting cause port is not free
     * @deprecated use {@link NatsOptionsImpl#timeoutMs()}
     */
    @Deprecated(since = "2.9.11", forRemoval = true)
    public Nats start(final long timeoutMs) throws Exception {
        this.timeoutMs = timeoutMs;
        return start();
    }

    /**
     * Starts the server if not already started e.g. by {@link NatsOptionsImpl#autoStart()} == true <br />
     * Throws all exceptions as {@link NatsStartException} which is a {@link RuntimeException}
     *
     * @return {@link Nats}
     */
    public Nats tryStart() {
        try {
            return start();
        } catch (Exception e) {
            throw new NatsStartException(e);
        }
    }

    /**
     * Starts the server if not already started e.g. by {@link NatsOptionsImpl#autoStart()} == true <br />
     * Throws all exceptions as {@link RuntimeException} <br />
     *
     * @param timeoutMs defines the start-up timeout {@code -1} no timeout, else waits until port up
     * @return {@link Nats}
     * @deprecated use {@link NatsOptionsImpl#timeoutMs()}
     */
    @Deprecated(since = "2.9.11", forRemoval = true)
    public Nats tryStart(final long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return tryStart();
    }

    /**
     * Starts the server if not already started e.g. by {@link NatsOptionsImpl#autoStart()} == true
     *
     * @return {@link Nats}
     * @throws IOException              if {@link Nats} is not found or unsupported on the {@link SystemUtil}
     * @throws BindException            if port is already taken
     * @throws PortUnreachableException if {@link Nats} is not starting cause port is not free
     */
//    @SuppressWarnings({"java:S899"})
    public synchronized Nats start() throws Exception {
        if (terminal != null && terminal.running()) {
            logger.severe(() -> format("[%s] is already running", logger.getName()));
            return this;
        }

        final int port = setNextFreePort();
        validatePort(port, timeoutMs, true, () -> new BindException("Address already in use [" + port + "]"));
        //TODO set binary file
        final Path binaryPath = downloadNats();
        logger.fine(() -> format("Starting [%s] port [%s] version [%s]", name, port, getValue(NATS_SYSTEM)));

        terminal = new Terminal()
                .consumerInfoStream(logger::info)
                .consumerErrorStream(logger::severe)
                .timeoutMs(timeoutMs > 0 ? timeoutMs : 10000)
                .breakOnError(false)
                .execute(prepareCommand(), null);

        validatePort(port, timeoutMs, false, () -> new PortUnreachableException(name + " failed to start with port [" + port + "]"));
        logger.info(() -> format("Started [%s] port [%s] version [%s] pid [%s]", name, port, getValue(NATS_SYSTEM), pid()));
        return this;
    }

    /**
     * Stops the {@link Process} and kills the {@link Nats} <br />
     * Only a log error will occur if the {@link Nats} were never started <br />
     *
     * @param timeoutMs defines the tear down timeout, {@code -1} no timeout, else waits until port is free again
     * @return {@link Nats}
     * @deprecated use {@link Nats#close()}
     */
    @Deprecated(since = "2.9.11", forRemoval = true)
    public Nats stop(final long timeoutMs) {
        this.timeoutMs = timeoutMs;
        close();
        return this;
    }


    /**
     * Stops the {@link Process} and kills the {@link Nats} <br />
     * Only a log error will occur if the {@link Nats} were never started <br />
     *
     * @return {@link Nats}
     * @deprecated use {@link Nats#close()}
     */
    @Deprecated(since = "2.9.11", forRemoval = true)
    public Nats stop() {
        close();
        return this;
    }

    /**
     * @return Path to binary file <br/>
     * see {@link NatsConfig#NATS_BINARY_PATH}
     */
    @Override
    public Path binary() {
        return Paths.get(getValue(NATS_BINARY_PATH, () -> Paths.get(
                getEnv(TMP_DIR),
                getValue(NATS_LOG_NAME).toLowerCase(),
                getValue(NATS_LOG_NAME).toLowerCase() + "_" + getValue(NATS_SYSTEM) + (OS == OS_WINDOWS ? ".exe" : "")
        ).toString()));
    }

    /**
     * @return Path to binary file
     * @deprecated use {@link Nats#binary()} <br/>
     * see {@link NatsConfig#NATS_BINARY_PATH}
     */
    @Deprecated(since = "2.9.11", forRemoval = true)
    public Path binaryFile() {
        return binary();
    }

    /**
     * @return Port (if &lt;=0, the port will be visible after {@link Nats#start()} - see also {@link NatsOptionsImpl#autoStart()}) <br/>
     * see {@link NatsConfig#PORT}
     */
    @Override
    public int port() {
        return parseInt(getValue(PORT));
    }

    /**
     * @return true if Jetstream is enabled <br/>
     * see {@link NatsConfig#JETSTREAM}
     */
    @Override
    public boolean jetStream() {
        return parseBoolean(getValue(PORT));
    }

    /**
     * @return true if "DV", "DVV" or "DEBUG" is set <br/>
     * see {@link NatsConfig#DV}
     * see {@link NatsConfig#DVV}
     * see {@link NatsConfig#DEBUG}
     */
    @Override
    public boolean debug() {
        return parseBoolean(getValue(DV)) || parseBoolean(getValue(DVV)) || parseBoolean(getValue(DEBUG));
    }

    /**
     * @return custom nats config file <br/>
     * see {@link NatsConfig#CONFIG}
     */
    @Override
    public Path configFile() {
        return ofNullable(getValue(CONFIG, () -> null)).filter(Objects::nonNull).map(Path::of).orElse(null);
    }

    /**
     * @return custom property config file <br/>
     * see {@link NatsConfig#NATS_CONFIG_FILE}
     */
    public Path configPropertyFile() {
        return ofNullable(getValue(NATS_CONFIG_FILE, () -> null)).filter(Objects::nonNull).map(Path::of).orElse(null);
    }

    @Override
    public void close() {
        shutdown();
    }

    /**
     * Gets resolved config value from key
     *
     * @param key config key
     * @return config key value
     */
    public String getValue(final NatsConfig key) {
        return getValue(key, () -> key.valueRaw() == null ? null : String.valueOf(key.valueRaw()));
    }

    /**
     * Gets resolved config value from key
     *
     * @param key config key
     * @param or  lazy loaded fallback value
     * @return config key value
     */
    public String getValue(final NatsConfig key, final Supplier<String> or) {
        return resolveEnvs(ofNullable(configMap.get(key)).map(MapValue::value).orElseGet(or), configMap);
    }

    /**
     * get process id
     *
     * @return process id or -1 if process is not running
     */
    public int pid() {
        try {
            return Integer.parseInt(String.join(" ", Files.readAllLines(pidFile(), StandardCharsets.UTF_8)).trim());
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * get process id file which only exists when the process is running
     *
     * @return process id file path
     */
    public Path pidFile() {
        return Paths.get(getValue(PID, () -> Paths.get(
                getEnv(TMP_DIR),
                getValue(NATS_LOG_NAME).toLowerCase(),
                port() + ".pid"
        ).toString()));
    }

    /**
     * Nats download url which is usually a zip file <br />
     *
     * @return nats download url
     * @deprecated use {@link Nats#getValue(NatsConfig)} instead
     */
    @Deprecated(since = "2.9.11", forRemoval = true)
    public String downloadUrl() {
        return getValue(NATS_DOWNLOAD_URL);
    }

    /**
     * nats server URL from bind to host address
     *
     * @return nats server url
     */
    public String url() {
        return "nats://" + getValue(ADDR) + ":" + port();
    }

    protected void setConfigFromNatsOptions(final NatsOptions natsOptions) {
        addConfig(DV, natsOptions.debug());
        addConfig(CONFIG, natsOptions.configFile());
        addConfig(PORT, natsOptions.port());
        addConfig(JETSTREAM, natsOptions.jetStream());
        addConfig(NATS_LOG_NAME, ofNullable(natsOptions.logger()).map(Logger::getName).orElse(getValue(NATS_LOG_NAME)));
    }

    protected void setConfigFromProperties() {
        getConfigFile().ifPresent(path -> {
            final Properties prop = new Properties();
            try (final InputStream inputStream = new FileInputStream(path.toFile())) {
                prop.load(inputStream);
            } catch (IOException e) {
                getLogger(getValue(NATS_LOG_NAME)).severe("Unable to read property file [" + path.toUri() + "] cause of [" + e.getMessage() + "]");
            }
            prop.forEach((key, value) -> addConfig(FILE, NatsConfig.valueOf(String.valueOf(key).toUpperCase()), removeQuotes((String) value)));
        });
    }

    protected void setDefaultConfig() {
        for (NatsConfig cfg : NatsConfig.values()) {
            final String value = cfg.value();
            addConfig(DEFAULT, cfg, value);
        }
        addConfig(DEFAULT, NATS_SYSTEM, NatsUtils.getSystem());
    }

    protected void setEnvConfig() {
        for (NatsConfig cfg : NatsConfig.values()) {
            addConfig(ENV, cfg, getEnv(cfg.name().startsWith(NATS_PREFIX) ? cfg.name() : NATS_PREFIX + cfg.name()));
        }
    }

    protected Optional<Path> getConfigFile() {
        for (Supplier<Path> supplier : createPathSuppliers(ofNullable(getValue(NATS_CONFIG_FILE)).filter(file -> !isEmpty(file)).orElse("nats.properties"))) {
            final Path path = supplier.get();
            if (path != null && Files.exists(path)) {
                return Optional.of(path);
            }
        }
        return Optional.empty();
    }

    protected void addConfig(final NatsConfig key, final Object value) {
        if (value != null && !String.valueOf(getValue(key)).equals(String.valueOf(value))) {
            addConfig(DSL, key, String.valueOf(value));
        }
    }

    protected void addConfig(final ValueSource source, final NatsConfig key, final String value) {
        if (value != null) {
            configMap.put(key, configMap.computeIfAbsent(key, val -> mapValueOf(source, value)).update(source, value));
        }
    }

    protected int setNextFreePort() {
        if (ofNullable(getValue(PORT, () -> null)).map(Integer::parseInt).orElse(-1) <= 0) {
            addConfig(configMap.get(PORT).source(), PORT, String.valueOf(getNextFreePort((int) PORT.valueRaw())));
        }
        return port();
    }

    @SuppressWarnings({"java:S899"})
    protected Path downloadNats() throws IOException {
        final Path binaryPath = binary();
        Files.createDirectories(binaryPath.getParent());
        if (Files.notExists(binaryPath)) {
            final URL source = new URL(getValue(NATS_DOWNLOAD_URL));
            unzip(download(source, Paths.get(binary().toString() + ".zip")), binaryPath);
        }
        binaryPath.toFile().setExecutable(true);
        SystemUtil.setFilePermissions(binaryPath, OWNER_EXECUTE, OTHERS_EXECUTE, OWNER_READ, OTHERS_READ, OWNER_WRITE, OTHERS_WRITE);
        return binaryPath;
    }

    protected String prepareCommand() {
        final StringBuilder command = new StringBuilder();
        setDefaultConfig();
        setEnvConfig();
        setConfigFromProperties();
        addConfig(DSL, PID, pidFile().toString());
        command.append(binary().toString());
        configMap.forEach((key, mapValue) -> {
            if (!key.name().startsWith(NATS_PREFIX) && mapValue != null && !isEmpty(mapValue.value())) {
                if (key.isWritableValue() || !"false".equals(mapValue.value())) {
                    command.append(" ");
                    command.append(key.key());
                }
                if (key.isWritableValue()) {
                    command.append("=");
                    command.append(mapValue.value().trim().toLowerCase());
                }
            }
        });
        command.append(stream(customArgs).collect(Collectors.joining(" ", " ", "")));
        command.append(stream(getValue(NATS_ARGS, () -> "").split("\\,")).map(String::trim).collect(Collectors.joining(" ", " ", "")));
        return command.toString();
    }

    protected synchronized void shutdown() {
        try {
            sendStopSignal();
            waitForShutDown(timeoutMs);
            terminal.process().destroy();
            terminal.process().waitFor();
        } catch (NullPointerException | InterruptedException ignored) {
            logger.warning(() -> format("Could not find process to stop [%s]", name));
            Thread.currentThread().interrupt();
        } finally {
            if (port() > -1) {
                waitForPort(port(), timeoutMs, true);
                logger.info(() -> format("Stopped [%s]", name));
            }
        }
        deletePidFile();
    }

    protected void sendStopSignal() {
        logger.info(() -> format("Stopping [%s]", name));
        if (pid() != -1) {
            new Terminal()
                    .consumerInfoStream(logger::info)
                    .consumerErrorStream(logger::severe)
                    .breakOnError(false)
                    .execute(binary() + " " + SIGNAL.key() + " stop=" + pid());
        }
    }

    protected void waitForShutDown(final long timeoutMs) {
        Optional.of(port()).filter(port -> port > 0).ifPresent(port -> {
            logger.info(() -> format("Stopped [%s]", name));
            waitForPort(port, timeoutMs, true);
        });
    }

    protected void deletePidFile() {
        ignoreException(run -> {
            Files.deleteIfExists(pidFile());
            return run;
        });
    }

    @Override
    public String toString() {
        return "Nats{" +
                "name=" + name +
                ", pid='" + pid() + '\'' +
                ", port=" + port() +
                ", configs=" + configMap.size() +
                ", customArgs=" + customArgs.length +
                '}';
    }
}

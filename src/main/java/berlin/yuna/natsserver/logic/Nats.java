package berlin.yuna.natsserver.logic;

import berlin.yuna.clu.logic.SystemUtil;
import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.natsserver.config.NatsConfig;
import berlin.yuna.natsserver.config.NatsOptions;
import berlin.yuna.natsserver.config.NatsOptionsBuilder;
import berlin.yuna.natsserver.model.MapValue;
import berlin.yuna.natsserver.model.ValueSource;
import berlin.yuna.natsserver.model.exception.NatsStartException;
import io.nats.commons.NatsInterface;

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
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static berlin.yuna.clu.logic.SystemUtil.OS;
import static berlin.yuna.clu.model.OsType.OS_WINDOWS;
import static berlin.yuna.natsserver.config.NatsConfig.*;
import static berlin.yuna.natsserver.config.NatsOptions.natsBuilder;
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
public class Nats implements NatsInterface {

    protected final String name;
    protected final Long timeoutMs;
    private final Logger logger;
    protected final Map<NatsConfig, MapValue> configMap = new ConcurrentHashMap<>();
    protected final AtomicReference<Terminal> terminal = new AtomicReference<>(null);
    public static final String NATS_PREFIX = "NATS_";
    private static final String TMP_DIR = "java.io.tmpdir";

    /**
     * Throws all exceptions as {@link NatsStartException} which is a {@link RuntimeException} <br />
     * Possible wrapped exceptions: <br />
     * {@link IOException} if {@link Nats} is not found or unsupported on the {@link SystemUtil}  <br />
     * {@link BindException} if port is already taken  <br />
     * {@link PortUnreachableException} if {@link Nats} is not starting cause port is not free  <br />
     */
    public Nats() {
        this(natsBuilder().autostart(true).build());
    }

    /**
     * Starts the server if {@link NatsConfig#NATS_AUTOSTART} == true
     * Throws all exceptions as {@link NatsStartException} which is a {@link RuntimeException} <br />
     * Possible wrapped exceptions: <br />
     * {@link IOException} if {@link Nats} is not found or unsupported on the {@link SystemUtil}  <br />
     * {@link BindException} if port is already taken  <br />
     * {@link PortUnreachableException} if {@link Nats} is not starting cause port is not free  <br />
     *
     * @param port the port to start on or &lt;=0 to use an automatically allocated port
     */
    public Nats(final int port) {
        this(natsBuilder().port(port).build());
    }

    /**
     * Starts the server if {@link NatsConfig#NATS_AUTOSTART} == true
     * Throws all exceptions as {@link NatsStartException} which is a {@link RuntimeException} <br />
     * Possible wrapped exceptions: <br />
     * {@link IOException} if {@link Nats} is not found or unsupported on the {@link SystemUtil}  <br />
     * {@link BindException} if port is already taken  <br />
     * {@link PortUnreachableException} if {@link Nats} is not starting cause port is not free  <br />
     *
     * @param natsOptions nats options
     */
    public Nats(final NatsOptionsBuilder natsOptions) {
        this(natsOptions.build());
    }

    /**
     * Starts the server if {@link NatsConfig#NATS_AUTOSTART} == true
     * Throws all exceptions as {@link NatsStartException} which is a {@link RuntimeException} <br />
     * Possible wrapped exceptions: <br />
     * {@link IOException} if {@link Nats} is not found or unsupported on the {@link SystemUtil}  <br />
     * {@link BindException} if port is already taken  <br />
     * {@link PortUnreachableException} if {@link Nats} is not starting cause port is not free  <br />
     *
     * @param natsOptions nats options
     */
    public Nats(final io.nats.commons.NatsOptions natsOptions) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        final var timeoutMsTmp = new AtomicLong(-1);
        if (natsOptions instanceof NatsOptions) {
            ((NatsOptions) natsOptions).config().forEach(this::addConfig);
        }
        setDefaultConfig();
        setEnvConfig();
        setConfigFromProperties();
        setConfigFromNatsOptions(natsOptions);
        this.name = getValue(NATS_LOG_NAME);
        this.timeoutMs = Long.parseLong(getValue(NATS_TIMEOUT_MS));
        this.logger = ofNullable(natsOptions.logger()).orElse(Logger.getLogger(name));
        ofNullable(natsOptions.logLevel()).ifPresent(logger::setLevel);
        ofNullable(getValue(NATS_AUTOSTART)).filter(Boolean::valueOf).ifPresent(autostart -> start());
    }

    /**
     * Starts the server if not already started e.g. by {@link NatsConfig#NATS_AUTOSTART} == true <br />
     * TThrows all exceptions as {@link NatsStartException} which is a {@link RuntimeException} <br />
     * Possible wrapped exceptions: <br />
     * {@link IOException} if {@link Nats} is not found or unsupported on the {@link SystemUtil}  <br />
     * {@link BindException} if port is already taken  <br />
     * {@link PortUnreachableException} if {@link Nats} is not starting cause port is not free  <br />
     *
     * @return {@link Nats}
     */
    public synchronized Nats start() {
        try {
            if (terminal.get() != null && terminal.get().running()) {
                logger.severe(() -> format("[%s] is already running", logger.getName()));
                return this;
            }
            downloadNats();
            final int port = setNextFreePort();
            validatePort(port, timeoutMs, true, () -> new BindException("Address already in use [" + port + "]"), () -> false);
            final String command = prepareCommand();
            logger.info(() -> format("Starting [%s] port [%s] version [%s] command [%s]", name, port, getValue(NATS_SYSTEM), command));
            startProcess(command);
            validatePort(port, timeoutMs, false, () -> new PortUnreachableException(name + " failed to start with port [" + port + "]"), () -> terminal.get() == null);
            logger.info(() -> format("Started [%s] port [%s] version [%s] pid [%s]", name, port, getValue(NATS_SYSTEM), pid()));
        } catch (Exception e) {
            throw new NatsStartException(e);
        }
        return this;
    }

    @Override
    public Process process() {
        return ofNullable(terminal.get()).map(Terminal::process).orElse(null);
    }

    @Override
    public String[] customArgs() {
        return ofNullable(getValue(NATS_ARGS, () -> null)).map(args -> args.split(ARGS_SEPARATOR)).orElseGet(() -> new String[0]);
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public Level loggingLevel() {
        return logger.getLevel();
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
     * @return Port (if &lt;=0, the port will be visible after {@link Nats#start()} - see also {@link NatsConfig#NATS_AUTOSTART}) <br/>
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
        return parseBoolean(getValue(JETSTREAM));
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
        return ofNullable(getValue(CONFIG, () -> null)).map(Path::of).orElse(null);
    }

    /**
     * @return custom property config file <br/>
     * see {@link NatsConfig#NATS_PROPERTY_FILE}
     */
    public Path configPropertyFile() {
        return ofNullable(getValue(NATS_PROPERTY_FILE, () -> null)).map(Path::of).orElse(null);
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
        return getValue(key, () -> key.defaultValue() == null ? null : String.valueOf(key.defaultValue()));
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
     */
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

    /**
     * @return nats configuration
     */
    public Map<NatsConfig, String> config() {
        return configMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().value()));
    }

    protected void setConfigFromNatsOptions(final io.nats.commons.NatsOptions natsOptions) {
        ofNullable(natsOptions.debug()).ifPresent(debug -> addConfig(DV, debug));
        ofNullable(natsOptions.configFile()).ifPresent(config -> addConfig(CONFIG, config));
        ofNullable(natsOptions.port()).ifPresent(port -> addConfig(PORT, port));
        ofNullable(natsOptions.jetStream()).ifPresent(jetstream -> addConfig(JETSTREAM, jetstream));
        ofNullable(natsOptions.logger()).map(Logger::getName).ifPresent(loggerName -> addConfig(NATS_LOG_NAME, loggerName));
    }

    protected void setConfigFromProperties() {
        getPropertyFiles(ofNullable(getValue(NATS_PROPERTY_FILE)).filter(NatsUtils::isNotEmpty).orElse("nats.properties")).forEach(path -> {
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
            final String value = cfg.defaultValueStr();
            addConfig(DEFAULT, cfg, value);
        }
        addConfig(DEFAULT, NATS_SYSTEM, NatsUtils.getSystem());
    }

    protected void setEnvConfig() {
        for (NatsConfig cfg : NatsConfig.values()) {
            addConfig(ENV, cfg, getEnv(cfg.name().startsWith(NATS_PREFIX) ? cfg.name() : NATS_PREFIX + cfg.name()));
        }
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
            addConfig(configMap.get(PORT).source(), PORT, String.valueOf(getNextFreePort((int) PORT.defaultValue())));
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
        //noinspection ResultOfMethodCallIgnored
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
            if (!key.name().startsWith(NATS_PREFIX) && mapValue != null && isNotEmpty(mapValue.value())) {
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
        command.append(stream(customArgs()).collect(Collectors.joining(" ", " ", "")));
        command.append(stream(getValue(NATS_ARGS, () -> "").split(ARGS_SEPARATOR)).map(String::trim).collect(Collectors.joining(" ", " ", "")));
        return command.toString();
    }

    protected synchronized void shutdown() {
        try {
            sendStopSignal();
            waitForShutDown(timeoutMs);
            if (terminal.get() != null) {
                terminal.get().process().destroy();
                terminal.get().process().waitFor();
            }
        } catch (InterruptedException ignored) {
            logger.warning(() -> format("Could not find process to stop [%s]", name));
            Thread.currentThread().interrupt();
        } finally {
            if (port() > -1) {
                waitForPort(port(), timeoutMs, true);
                logger.info(() -> format("Stopped [%s]", name));
            }
            terminal.set(null);
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

    protected void startProcess(final String command) {
        terminal.set(new Terminal()
                .timeoutMs(timeoutMs)
                .breakOnError(false)
                .consumerErrorStream(logger::info)
                .consumerInfoStream(serve -> {
                    logger.severe(serve);
                    terminal.set(null);
                })
                .execute(command, null)
        );
    }

    @Override
    public String toString() {
        return "Nats{" +
                "name=" + name +
                ", pid='" + pid() + '\'' +
                ", port=" + port() +
                ", configs=" + configMap.size() +
                '}';
    }
}

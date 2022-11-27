package berlin.yuna.natsserver.logic;

import berlin.yuna.clu.logic.SystemUtil;
import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.natsserver.config.NatsConfig;
import berlin.yuna.natsserver.model.MapValue;
import berlin.yuna.natsserver.model.exception.NatsStartException;

import java.io.IOException;
import java.net.BindException;
import java.net.PortUnreachableException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static berlin.yuna.natsserver.config.NatsConfig.NATS_SYSTEM;
import static berlin.yuna.natsserver.config.NatsConfig.PORT;
import static berlin.yuna.natsserver.config.NatsConfig.SIGNAL;
import static berlin.yuna.natsserver.logic.NatsUtils.validatePort;
import static berlin.yuna.natsserver.logic.NatsUtils.waitForPort;
import static berlin.yuna.natsserver.model.ValueSource.DSL;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * {@link Nats}
 *
 * @author Yuna Morgenstern
 * @see SystemUtil
 * @see Nats
 * @since 1.0
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Nats extends NatsBase {

    /**
     * Create {@link Nats} without any start able configuration
     */
    public Nats() {
        super(new ArrayList<>());
    }

    /**
     * Create {@link Nats} with custom args
     */
    public Nats(final List<String> customArgs) {
        super(customArgs);
    }

    /**
     * Create {@link Nats} with the simplest start able configuration
     *
     * @param port start port, -1 = random, default is 4222
     */
    public Nats(final int port) {
        this();
        config(PORT, String.valueOf(port));
    }

    /**
     * Create custom {@link Nats} with the simplest configuration {@link Nats#config(String...)}
     *
     * @param kv passes the original parameters to the server. example: port:4222, user:admin, password:admin
     */
    public Nats(final String... kv) {
        this();
        this.config(kv);
    }

    /**
     * GetNatServerConfig
     *
     * @return the {@link Nats} configuration
     */
    public Map<NatsConfig, MapValue> config() {
        return config;
    }

    /**
     * Configures the nats server
     *
     * @return the {@link Nats} configuration
     */
    public Nats config(final NatsConfig key, final String value) {
        config.remove(key);
        if (key.desc().startsWith("[/]")) {
            if (value.equals("true")) {
                addConfig(DSL, key, value);
            }
        } else {
            addConfig(DSL, key, value);
        }
        return this;
    }

    /**
     * Configures the nats server
     *
     * @param config passes the original parameters to the server.
     * @return {@link Nats}
     * @see Nats#config(String...)
     * @see NatsConfig
     */
    public Nats config(final Map<NatsConfig, String> config) {
        config.forEach((key, value) -> addConfig(DSL, key, value));
        return this;
    }

    /**
     * Configures the nats server
     *
     * @param kv example: port, 4222, user, admin, password, admin
     * @return {@link Nats}
     * @see NatsConfig
     */
    public Nats config(final String... kv) {
        boolean isKey = true;
        String key = null;
        for (String property : kv) {
            if (isKey) {
                key = property;
            } else {
                config(NatsConfig.valueOf(key.toUpperCase().replace("-", "")), property);
            }
            isKey = !isKey;
        }
        return this;
    }

    /**
     * Starts the server in {@link ProcessBuilder} with the given parameterConfig {@link Nats#config(String...)}
     * Throws all exceptions as {@link RuntimeException}
     *
     * @return {@link Nats}
     */
    public Nats tryStart() {
        return tryStart(SECONDS.toMillis(10));
    }

    /**
     * Starts the server in {@link ProcessBuilder} with the given config {@link Nats#config(String...)}
     * Throws all exceptions as {@link RuntimeException}
     *
     * @param timeoutMs defines the start-up timeout {@code -1} no timeout, else waits until port up
     * @return {@link Nats}
     */
    public Nats tryStart(final long timeoutMs) {
        try {
            start(timeoutMs);
            return this;
        } catch (Exception e) {
            throw new NatsStartException(e);
        }
    }

    /**
     * Starts the server in {@link ProcessBuilder} with the given config {@link Nats#config(String...)}
     *
     * @return {@link Nats}
     * @throws IOException              if {@link Nats} is not found or unsupported on the {@link SystemUtil}
     * @throws BindException            if port is already taken
     * @throws PortUnreachableException if {@link Nats} is not starting cause port is not free
     */
    public Nats start() throws Exception {
        return start(SECONDS.toMillis(10));
    }

    /**
     * Starts the server in {@link ProcessBuilder} with the given config {@link Nats#config(String...)}
     *
     * @param timeoutMs defines the start-up timeout {@code -1} no timeout, else waits until port up
     * @return {@link Nats}
     * @throws IOException              if {@link Nats} is not found or unsupported on the {@link SystemUtil}
     * @throws BindException            if port is already taken
     * @throws PortUnreachableException if {@link Nats} is not starting cause port is not free
     */
    @SuppressWarnings({"java:S899"})
    public synchronized Nats start(final long timeoutMs) throws Exception {
        if (terminal != null && terminal.running()) {
            logger.severe(() -> format("[%s] is already running", name));
            return this;
        }

        final int port = setNextFreePort().port();
        validatePort(port, timeoutMs, true, () -> new BindException("Address already in use [" + port + "]"));

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

    @Override
    public void close() {
        this.stop();
    }

    /**
     * Stops the {@link ProcessBuilder} and kills the {@link Nats}
     * Only a log error will occur if the {@link Nats} were never started
     *
     * @return {@link Nats}
     */
    public Nats stop() {
        return stop(-1);
    }

    /**
     * Stops the {@link ProcessBuilder} and kills the {@link Nats}
     * Only a log error will occur if the {@link Nats} were never started
     *
     * @param timeoutMs defines the tear down timeout, {@code -1} no timeout, else waits until port is free again
     * @return {@link Nats}
     */
    public synchronized Nats stop(final long timeoutMs) {
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
        return this;
    }

    private void waitForShutDown(final long timeoutMs) {
        Optional.of(port()).filter(port -> port > 0).ifPresent(port -> {
            logger.info(() -> format("Stopped [%s]", name));
            waitForPort(port, timeoutMs, true);
        });
    }

    private void sendStopSignal() {
        logger.info(() -> format("Stopping [%s]", name));
        if (pid() != -1) {
            new Terminal()
                    .consumerInfoStream(logger::info)
                    .consumerErrorStream(logger::severe)
                    .breakOnError(false)
                    .execute(binaryFile() + " " + SIGNAL.key() + " stop=" + pid());
        }
    }
}

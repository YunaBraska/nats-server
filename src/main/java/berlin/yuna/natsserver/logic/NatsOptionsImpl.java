package berlin.yuna.natsserver.logic;

import berlin.yuna.natsserver.config.NatsConfig;
import berlin.yuna.natsserver.model.MapValue;
import lombok.AccessLevel;
import lombok.Builder;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NatsOptionsImpl extends NatsOptions {

    private final Map<NatsConfig, String> configMap;
    private final boolean autoStart;
    private final long timeoutMs;

    /**
     * Construct and start the Nats Server with options
     *
     * @param port         the port to start on or &lt;=0 to use an automatically allocated port
     * @param jetStream    whether to enable JetStream
     * @param debug        whether to start the server with the -DV flags
     * @param configFile   path to a custom config file
     * @param customArgs   any custom args to add to the command line
     * @param loggingLevel log level
     * @param logger       custom logger
     * @param configMap    set configs
     * @param autoStart    start with constructor - else need to run {@link Nats#start()}
     * @param timeoutMs    defines the start-up timeout in milliseconds (-1 = default)
     */
    @Builder(access = AccessLevel.PUBLIC)
    public NatsOptionsImpl(
            final int port,
            final boolean jetStream,
            final boolean debug,
            final Path configFile,
            final String[] customArgs,
            final Level loggingLevel,
            final Logger logger,
            final Map<NatsConfig, String> configMap,
            final int timeoutMs,
            final boolean autoStart
    ) {
        super(port, jetStream, debug, configFile, customArgs, loggingLevel, logger);
        this.configMap = configMap == null ? new EnumMap<>(NatsConfig.class) : new EnumMap<>(configMap);
        this.autoStart = autoStart;
        this.timeoutMs = timeoutMs;
    }

    public Map<NatsConfig, String> configMap() {
        return configMap;
    }

    public boolean autoStart() {
        return autoStart;
    }

    public long timeoutMs() {
        return timeoutMs;
    }

    public NatsOptionsImpl config(final NatsConfig key, final String value) {
        configMap.put(key, value);
        return this;
    }

    /**
     * Configures the nats server
     *
     * @param kv example: port, 4222, user, admin, password, admin
     * @return {@link Nats}
     * @see NatsConfig
     */
    public NatsOptionsImpl config(final String... kv) {
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

    public static NatsOptionsImpl defaultConfig() {
        return NatsOptionsImpl.builder().build();
    }
}

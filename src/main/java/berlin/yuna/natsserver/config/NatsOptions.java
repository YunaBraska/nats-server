package berlin.yuna.natsserver.config;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static berlin.yuna.natsserver.config.NatsConfig.ARGS_SEPARATOR;
import static berlin.yuna.natsserver.config.NatsOptionsBuilder.getValue;
import static berlin.yuna.natsserver.config.NatsOptionsBuilder.getValueB;
import static berlin.yuna.natsserver.config.NatsOptionsBuilder.getValueI;

@SuppressWarnings("java:S2176")
public class NatsOptions implements io.nats.commons.NatsOptions {

    protected final Logger logger;
    protected final Map<NatsConfig, String> config;

    public NatsOptions(final Logger logger, final Map<NatsConfig, String> config) {
        this.logger = logger;
        this.config = config == null ? new EnumMap<>(NatsConfig.class) : new EnumMap<>(config);
    }

    /**
     * @return Nats version
     * @see NatsConfig#NATS_VERSION
     */
    public String version() {
        return config.get(NatsConfig.NATS_VERSION);
    }

    /**
     * @return The port to start on or &lt;=0 to use an automatically allocated port
     * @see NatsConfig#PORT
     */
    @Override
    public Integer port() {
        return getValueI(config, NatsConfig.PORT);
    }

    /**
     * @return true if JetStream is enabled
     * @see NatsConfig#JETSTREAM
     */
    @Override
    public Boolean jetStream() {
        return getValueB(config, NatsConfig.JETSTREAM);
    }

    /**
     * @return true if debug is enabled
     * @see NatsConfig#DV
     */
    @Override
    public Boolean debug() {
        return getValueB(config, NatsConfig.DV);
    }

    /**
     * @return path to a custom config file
     * @see NatsConfig#CONFIG
     */
    @Override
    public Path configFile() {
        return getValue(config, Path::of, NatsConfig.CONFIG);
    }

    /**
     * @return custom args to add to the command line
     * @see NatsConfig#NATS_ARGS
     */
    @Override
    public String[] customArgs() {
        return getValue(config, args -> args.split(ARGS_SEPARATOR), NatsConfig.NATS_ARGS);
    }

    /**
     * @return custom logger
     */
    @Override
    public Logger logger() {
        return logger;
    }

    /**
     * @return custom LogLevel
     */
    @Override
    public Level logLevel() {
        return getValue(config, NatsConfig::logLevelOf, NatsConfig.NATS_LOG_LEVEL);
    }

    /**
     * @return configMap
     * @see NatsConfig
     */
    public Map<NatsConfig, String> config() {
        return config;
    }

    public static NatsOptionsBuilder natsBuilder() {
        return new NatsOptionsBuilder();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final NatsOptions that = (NatsOptions) o;
        return Objects.equals(logger, that.logger) && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logger, config);
    }
}

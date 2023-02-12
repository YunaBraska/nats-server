package berlin.yuna.natsserver.config;

import berlin.yuna.natsserver.logic.Nats;
import berlin.yuna.natsserver.logic.NatsUtils;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static berlin.yuna.natsserver.config.NatsConfig.ARGS_SEPARATOR;
import static java.util.Optional.ofNullable;

@SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
public class NatsOptionsBuilder {

    protected Logger logger;
    protected Map<NatsConfig, String> configMap = new EnumMap<>(NatsConfig.class);

    protected NatsOptionsBuilder() {
    }

    /**
     * @return immutable config for {@link Nats}
     */
    public NatsOptions build() {
        return new NatsOptions(logger, configMap);
    }

    /**
     * @return {@link Nats} build nats server from config
     */
    public Nats nats() {
        return new Nats(this);
    }

    /**
     * @return Nats version
     * @see NatsConfig#NATS_VERSION
     */
    public String version() {
        return configMap.get(NatsConfig.NATS_VERSION);
    }

    /**
     * @param version Sets the nats version
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig#NATS_VERSION
     */
    public NatsOptionsBuilder version(final String version) {
        configMap.put(
                NatsConfig.NATS_VERSION,
                ofNullable(version).filter(NatsUtils::isNotEmpty).map(v -> v.toLowerCase().startsWith("v") ? v : "v" + v).orElse(null)
        );
        return this;
    }

    /**
     * @param version Sets the nats version
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig#NATS_VERSION
     */
    public NatsOptionsBuilder version(final NatsVersion version) {
        configMap.put(NatsConfig.NATS_VERSION, version != null ? version.value() : null);
        return this;
    }

    /**
     * @return The port to start on or &lt;=0 to use an automatically allocated port
     * @see NatsConfig#PORT
     */
    public Integer port() {
        return getValueI(configMap, NatsConfig.PORT);
    }

    /**
     * @param port The port to start on or &lt;=0 to use an automatically allocated port
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig#PORT
     */
    public NatsOptionsBuilder port(final Integer port) {
        setValueI(configMap, NatsConfig.PORT, port);
        return this;
    }

    /**
     * @return true if JetStream is enabled
     * @see NatsConfig#JETSTREAM
     */
    public Boolean jetStream() {
        return getValueB(configMap, NatsConfig.JETSTREAM);
    }

    /**
     * @param jetStream whether to enable JetStream
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig#JETSTREAM
     */
    public NatsOptionsBuilder jetStream(final Boolean jetStream) {
        setValueB(configMap, NatsConfig.JETSTREAM, jetStream);
        return this;
    }

    /**
     * @return true if debug is enabled
     * @see NatsConfig#DV
     */
    public Boolean debug() {
        return getValueB(configMap, NatsConfig.DV);
    }

    /**
     * @param debug whether to start the server with the debug flag
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig#DV
     */
    public NatsOptionsBuilder debug(final Boolean debug) {
        setValueB(configMap, NatsConfig.DV, debug);
        return this;
    }

    /**
     * @return path to a custom config file
     * @see NatsConfig#CONFIG
     */
    public Path configFile() {
        return getValue(configMap, Path::of, NatsConfig.CONFIG);
    }

    /**
     * @param configFile path to a custom config file
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig#CONFIG
     */
    public NatsOptionsBuilder configFile(final Path configFile) {
        setValue(configMap, Path::toString, NatsConfig.CONFIG, configFile);
        return this;
    }

    /**
     * @return path to a custom config property file
     * @see NatsConfig#NATS_PROPERTY_FILE
     */
    public Path configPropertyFile() {
        return getValue(configMap, Path::of, NatsConfig.NATS_PROPERTY_FILE);
    }

    /**
     * @param configFile path to a custom config property file
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig#NATS_PROPERTY_FILE
     */
    public NatsOptionsBuilder configPropertyFile(final Path configFile) {
        setValue(configMap, Path::toString, NatsConfig.NATS_PROPERTY_FILE, configFile);
        return this;
    }

    /**
     * @return custom args to add to the command line
     * @see NatsConfig#NATS_ARGS
     */
    public String[] customArgs() {
        return getValue(configMap, args -> args.split(ARGS_SEPARATOR), NatsConfig.NATS_ARGS);
    }

    /**
     * @param customArgs custom args to set
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig#NATS_ARGS
     */
    public NatsOptionsBuilder customArgs(final String... customArgs) {
        ofNullable(customArgs).ifPresent(value -> configMap.put(NatsConfig.NATS_ARGS, String.join(ARGS_SEPARATOR, value)));
        return this;
    }

    /**
     * @param customArgs custom args to add
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig#NATS_ARGS
     */
    public NatsOptionsBuilder addArgs(final String... customArgs) {
        final var args = configMap.get(NatsConfig.NATS_ARGS);
        ofNullable(customArgs).ifPresent(value -> {
            if (args != null) {
                configMap.put(NatsConfig.NATS_ARGS, String.join(ARGS_SEPARATOR, args, String.join(ARGS_SEPARATOR, value)));
            } else {
                configMap.put(NatsConfig.NATS_ARGS, String.join(ARGS_SEPARATOR, value));
            }
        });
        return this;
    }

    /**
     * @return custom logger
     */
    public Logger logger() {
        return logger;
    }

    /**
     * @param logger custom logger
     * @return self {@link NatsOptionsBuilder}
     */
    public NatsOptionsBuilder logger(final Logger logger) {
        this.logger = logger;
        return this;
    }


    /**
     * @return custom LogLevel
     */
    public Level logLevel() {
        return getValue(configMap, NatsConfig::logLevelOf, NatsConfig.NATS_LOG_LEVEL);
    }

    /**
     * @param level custom logLevel
     * @return self {@link NatsOptionsBuilder}
     */
    public NatsOptionsBuilder logLevel(final Level level) {
        setValue(configMap, Level::getName, NatsConfig.NATS_LOG_LEVEL, level);
        return this;
    }

    /**
     * @return true = auto closable, false manual use `.start()` method
     * @see NatsConfig#NATS_AUTOSTART
     */
    public Boolean autostart() {
        return getValueB(configMap, NatsConfig.NATS_AUTOSTART);
    }


    /**
     * @param autostart true = auto closable, false manual use `.start()` method
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig#NATS_AUTOSTART
     */
    public NatsOptionsBuilder autostart(final Boolean autostart) {
        setValueB(configMap, NatsConfig.NATS_AUTOSTART, autostart);
        return this;
    }

    /**
     * @return defines the start-up timeout in milliseconds (-1 == default)
     */
    public Long timeoutMs() {
        return getValue(configMap, Long::parseLong, NatsConfig.NATS_TIMEOUT_MS);
    }

    /**
     * @param timeoutMs defines the start-up timeout in milliseconds (-1 == default)
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig#NATS_TIMEOUT_MS
     */
    public NatsOptionsBuilder timeoutMs(final Number timeoutMs) {
        setValue(configMap, number -> String.valueOf(number.longValue()), NatsConfig.NATS_TIMEOUT_MS, timeoutMs);
        return this;
    }

    /**
     * @return configMap
     * @see NatsConfig
     */
    public Map<NatsConfig, String> configMap() {
        return configMap;
    }

    /**
     * configMap
     *
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig
     */
    public NatsOptionsBuilder configMap(final Map<NatsConfig, String> configMap) {
        this.configMap = new EnumMap<>(configMap);
        return this;
    }

    /**
     * Adds additional {@link NatsConfig} <br />
     *
     * @param key   example: {@link NatsConfig#PORT}
     * @param value example: "4222"
     * @return self {@link NatsOptionsBuilder}
     * @see NatsConfig
     */
    public NatsOptionsBuilder config(final NatsConfig key, final String value) {
        configMap.put(key, value);
        return this;
    }

    /**
     * Adds additional {@link NatsConfig} <br />
     * The Key is caseInsensitive. Key doesn't need to have the prefix '-' or '--'
     *
     * @param kv example: port, 4222, user, admin, password, admin
     * @return self {@link NatsOptionsBuilder}
     */
    public NatsOptionsBuilder config(final String... kv) {
        for (int i = 0; i < kv.length - 1; i += 2) {
            config(NatsConfig.valueOf(kv[i].toUpperCase().replace("-", "")), kv[i + 1]);
        }
        return this;
    }

    protected static Integer getValueI(final Map<NatsConfig, String> config, final NatsConfig key) {
        return getValue(config, Integer::parseInt, key);
    }

    protected static Boolean getValueB(final Map<NatsConfig, String> config, final NatsConfig key) {
        return getValue(config, Boolean::parseBoolean, key);
    }

    protected static <T> T getValue(final Map<NatsConfig, String> config, final Function<String, T> map, final NatsConfig key) {
        return ofNullable(config.get(key)).map(map).orElse(null);
    }

    protected static void setValueI(final Map<NatsConfig, String> config, final NatsConfig key, final Integer value) {
        setValue(config, Object::toString, key, value);
    }

    protected static void setValueB(final Map<NatsConfig, String> config, final NatsConfig key, final Boolean value) {
        setValue(config, Object::toString, key, value);
    }

    protected static <T> void setValue(final Map<NatsConfig, String> config, final Function<T, String> map, final NatsConfig key, final T value) {
        ofNullable(value).map(map).ifPresent(val -> config.put(key, val));
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final NatsOptionsBuilder that = (NatsOptionsBuilder) o;
        return Objects.equals(logger, that.logger) && Objects.equals(configMap, that.configMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logger, configMap);
    }
}

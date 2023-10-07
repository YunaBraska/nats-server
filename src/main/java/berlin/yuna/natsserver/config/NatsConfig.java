package berlin.yuna.natsserver.config;

import berlin.yuna.natsserver.logic.Nats;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public enum NatsConfig {

    // Server Options
    NET("--net", "0.0.0.0", String.class, "Bind to host address (default: 0.0.0.0)"),
    PORT("--port", 4222, Integer.class, "Use port for clients (default: 4222)"),
    SERVER_NAME("--server_name", null, String.class, "Server name (default: auto)"),
    PID("--pid", null, Path.class, "File to store PID"),
    HTTP_PORT("--http_port", null, Integer.class, "Use port for http monitoring"),
    HTTPS_PORT("--https_port", null, Integer.class, "Use port for https monitoring"),
    CONFIG("--config", null, Path.class, "Configuration file"),
    TEST_CONFIG("-t", false, SilentBoolean.class, "Test configuration and exit" + System.lineSeparator() + "(default: false)"),
    SIGNAL("--signal", null, String.class, "Send signal to nats-server process (ldm, stop, quit, term, reopen, reload)" + System.lineSeparator() + " <pid> can be either a PID (e.g. 1) or the path to a PID file" + System.lineSeparator() + " (e.g. /var/run/nats-server.pid) e.g. stop=pid"),
    CLIENT_ADVERTISE("--client_advertise", null, String.class, "Client URL to advertise to other servers"),
    PORTS_FILE_DIR("--ports_file_dir", null, Path.class, "Creates a ports file in the specified directory (<executable_name>_<pid>.ports)."),

    // Logging Options
    LOG("--log", null, Path.class, "File to redirect log output"),
    LOG_TIMELOG_TIME("--logtime", null, Boolean.class, "Timestamp log entries (default: true)"),
    SYSLOG("--syslog", false, SilentBoolean.class, "Log to syslog or windows event log" + System.lineSeparator() + "(default: false)"),
    REMOTE_SYSLOG("--remote_syslog", null, String.class, "Syslog server addr (udp://localhost:514)"),
    DEBUG("--debug", false, SilentBoolean.class, "Enable debugging output" + System.lineSeparator() + "(default: false)"),
    TRACE("--trace", false, SilentBoolean.class, "Trace the raw protocol" + System.lineSeparator() + "(default: false)"),
    VV("-VV", false, SilentBoolean.class, "Verbose trace (traces system account as well)" + System.lineSeparator() + "(default: false)"),
    DV("-DV", false, SilentBoolean.class, "Debug and trace" + System.lineSeparator() + "(default: false)"),
    DVV("-DVV", false, SilentBoolean.class, "Debug and verbose trace (traces system account as well)" + System.lineSeparator() + "(default: false)"),
    LOG_SIZE_LIMIT("--log_size_limit", null, Integer.class, "Logfile size limit (default: auto)"),
    MAX_TRACED_MSG_LEN("--max_traced_msg_len", null, Integer.class, "Maximum printable length for traced messages (default: unlimited)"),

    // JetStream Options
    JETSTREAM("--jetstream", false, SilentBoolean.class, "Enable JetStream functionality" + System.lineSeparator() + "(default: false)"),
    STORE_DIR("--store_dir", null, Path.class, "Set the storage directory"),

    // Logging Options
    USER("--user", null, String.class, "User required for connections"),
    PASS("--pass", null, String.class, "Password required for connections"),
    AUTH("--auth", null, String.class, "Authorization token required for connections"),

    // TLS Options
    TLS("--tls", false, SilentBoolean.class, "Enable TLS, do not verify clients (default: false)"),
    TLS_CERT("--tlscert", null, Path.class, "Server certificate file"),
    TLS_KEY("--tlskey", null, Path.class, "Private key for server certificate"),
    TLS_VERIFY("--tlsverify", false, SilentBoolean.class, "Enable TLS, verify client certificates" + System.lineSeparator() + "(default: false)"),
    TLS_CA_CERT("--tlscacert", null, Path.class, "Client certificate CA for verification"),

    // Cluster Options
    ROUTES("--routes", null, String.class, "Routes to solicit and connect" + System.lineSeparator() + "e.g. rurl-1, rurl-2"),
    CLUSTER("--cluster", null, URL.class, "Cluster URL for solicited routes"),
    CLUSTER_NAME("--cluster_name", null, String.class, "Cluster Name, if not set one will be dynamically generated"),
    NO_ADVERTISE("--no_advertise", null, Boolean.class, "Do not advertise known cluster information to clients" + System.lineSeparator() + "(default: false)"),
    LUSTER_ADVERTISE("--cluster_advertise", null, String.class, "Cluster URL to advertise to other servers"),
    CONNECT_RETRIES("--connect_retries", null, Integer.class, "For implicit routes, number of connect retries"),
    CLUSTER_LISTEN("--cluster_listen", null, URL.class, "Cluster url from which members can solicit routes"),

    //Profiling Options
    PROFILE("--profile", null, Integer.class, "Profiling HTTP port"),

    // Common Options
    HELP("--help", false, SilentBoolean.class, "Show this message" + System.lineSeparator() + "(default: false)"),
    HELP_TLS("--help_tls", false, SilentBoolean.class, "TLS help" + System.lineSeparator() + "(default: false)"),

    //WRAPPER configs
    NATS_AUTOSTART(null, true, Boolean.class, "[true] == auto closable, [false] == manual use `.start()` method (default: true)"),
    NATS_LOG_LEVEL(null, null, String.class, "java log level e.g. [OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL]"),
    NATS_TIMEOUT_MS(null, 10000, String.class, "true = auto closable, false manual use `.start()` method"),
    NATS_SYSTEM(null, null, String.class, "suffix for binary path"),

    NATS_LOG_NAME(null, Nats.class.getSimpleName(), String.class, "java wrapper name"),

    NATS_VERSION(null, "v2.10.2", String.class, "Overwrites Nats server version on path"),

    NATS_DOWNLOAD_URL(null, "https://github.com/nats-io/nats-server/releases/download/%" + NATS_VERSION.name() + "%/nats-server-%" + NATS_VERSION.name() + "%-%" + NATS_SYSTEM.name() + "%.zip", URL.class, "Path to Nats binary or zip file"),

    NATS_BINARY_PATH(null, null, Path.class, "Target Path to Nats binary or zip file - auto from " + NATS_DOWNLOAD_URL.name() + ""),

    NATS_PROPERTY_FILE(null, null, Path.class, "Additional config file (properties / KV) same as DSL configs"),

    NATS_ARGS(null, null, String.class, "custom arguments separated by &&");

    public static final String ARGS_SEPARATOR = "&&";
    public static final Level[] ALL_LOG_LEVEL = new Level[]{Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.ALL};

    public static Level logLevelOf(final String level) {
        return Arrays.stream(ALL_LOG_LEVEL).filter(value -> value.getName().equalsIgnoreCase(level)).findFirst().orElse(null);
    }

    private final String key;
    private final Class<?> type;
    private final Object defaultValue;
    private final String description;

    NatsConfig(final String key, final Object defaultValue, final Class<?> type, final String description) {
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public boolean isWritableValue() {
        return type != SilentBoolean.class;
    }


    public Object defaultValue() {
        return defaultValue;
    }

    public String description() {
        return description;
    }

    /**
     * @return value as string
     */
    public String defaultValueStr() {
        return defaultValue == null ? null : defaultValue.toString();
    }

    /**
     * Command line property key
     *
     * @return key for command line
     */
    public String key() {
        return key;
    }

    public Class<?> type() {
        return type;
    }

    @SuppressWarnings("java:S2094")
    public static class SilentBoolean extends AtomicBoolean {
        //DUMMY CLASS
    }
}

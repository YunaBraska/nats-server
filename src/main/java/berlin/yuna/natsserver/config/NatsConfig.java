package berlin.yuna.natsserver.config;

import berlin.yuna.natsserver.logic.Nats;

public enum NatsConfig {

    // Server Options
    NAME(null, "[STRING] Server name (default: auto)"),
    SERVER_NAME(null, "[STRING] Server name (default: auto)"),
    SIGNAL(null, "[STRING] [SIGNAL] Send signal to nats-server process (stop, quit, reopen)"),
    PID(null, "[STRING] File to store PID"),
    CLIENT_ADVERTISE(null, "[STRING] Client URL to advertise to other servers"),

    // Server Clustering Options
    CLUSTER_NAME(null, "[STRING] Cluster Name, if not set one will be dynamically generated"),
    NO_ADVERTISE(null, "[BOOL] Advertise known cluster IPs to clients"),
    CLUSTER_ADVERTISE(null, "[STRING] Cluster URL to advertise to other servers"),
    CONNECT_RETRIES(null, "[INT] For implicit routes, number of connect retries"),
    CLUSTER_LISTEN(null, "[STRING] Cluster url from which members can solicit routes"),

    //NATS Server Options
    ADDR("0.0.0.0", "[STRING] Bind to host address (default: 0.0.0.0)"),
    NET("0.0.0.0", "[STRING] Bind to host address (default: 0.0.0.0)"),
    PORT(4222, "[INT] Use port for clients (default: 4222)"),
    HTTP_PORT(null, "[INT] Use port for http monitoring"),
    HTTPS_PORT(null, "[INT] Use port for https monitoring"),
    CONFIG(null, "[STRING] Configuration file"),
    PORTS_FILE_DIR(null, "[STRING] Creates a ports file in the specified directory (<executable_name>_<pid>.ports)"),

    //NATS Server Logging Options
    LOG(null, "[STRING] File to redirect log output"),
    TRACE(null, "[/] Trace the raw protocol"),
    DEBUG(null, "[/] Enable debugging output"),
    SYSLOG(null, "[/] Log to syslog or windows event log"),
    LOGTIME(null, "[/] Timestamp log entries (default: true)"),
    REMOTE_SYSLOG(null, "[STRING] Syslog server addr (udp://localhost:514)"),
    LOG_SIZE_LIMIT(null, "[INT] Logfile size limit (default: auto)"),
    MAX_TRACED_MSG_LEN(null, "[INT] Maximum printable length for traced messages (default: unlimited)"),

    //JetStream Options,
    JETSTREAM(null, "[/] Enable JetStream functionality"),
    STORE_DIR(null, "[STRING] Set the storage directory"),

    //NATS Server Authorization Options
    USER(null, "[STRING] User required for connections"),
    PASS(null, "[STRING] Password required for connections"),
    AUTH(null, "[STRING] Authorization token required for connections"),

    //TLS Options
    TLS(null, "[/] Enable TLS, do not verify clients (default: false)"),
    TLSCERT(null, "[STRING] Server certificate file"),
    TLSKEY(null, "[STRING] Private key for server certificate"),
    TLSCACERT(null, "[STRING] Client certificate CA for verification"),
    TLSVERIFY(null, "[/] Enable TLS, verify client certificates"),

    //NATS Clustering Options
    ROUTES(null, "[STRING] Routes to solicit and connect"),
    CLUSTER(null, "[STRING] Cluster URL for solicited routes"),

    //Profile Options
    PROFILE(null, "[INT] Profiling HTTP port"),


    //WRAPPER configs
    NATS_SYSTEM(null, "[STRING] suffix for binary path"),
    NATS_LOG_NAME(Nats.class.getSimpleName(), "[STRING] java wrapper name"),
    NATS_VERSION("v2.9.4", "[STRING] Overwrites Nats server version on path"),
    NATS_DOWNLOAD_URL("https://github.com/nats-io/nats-server/releases/download/%" + NATS_VERSION.name() + "%/nats-server-%" + NATS_VERSION.name() + "%-%" + NATS_SYSTEM.name() + "%.zip", "[STRING] Path to Nats binary or zip file"),
    NATS_BINARY_PATH(null, "[STRING] Target Path to Nats binary or zip file - auto from " + NATS_DOWNLOAD_URL.name() + ""),
    NATS_CONFIG_FILE(null, "[STRING] Additional property file with config value"),
    NATS_ARGS(null, "[STRING] custom arguments separated by \\, or just space");

    private final Object defaultValue;
    private final String description;

    NatsConfig(final Object defaultValue, final String description) {
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public Object valueRaw() {
        return defaultValue;
    }

    public String desc() {
        return description;
    }

    /**
     * @return value as string
     */
    public String value() {
        return defaultValue == null ? null : defaultValue.toString();
    }

    /**
     * Command line property key
     *
     * @return key for command line
     */
    public String key() {
        String key = name().toLowerCase();
        key = desc().startsWith("-") ? "-" + key : "--" + key;
        key = desc().startsWith("[BOOL]") ? key + "=" : key + " ";
        return key;
    }
}

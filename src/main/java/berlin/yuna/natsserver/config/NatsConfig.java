package berlin.yuna.natsserver.config;

public enum NatsConfig {

    // Server Options
    SIGNAL(null, "[STRING] [SIGNAL] Send signal to nats-server process (stop, quit, reopen)"),
    PID(null, "[STRING] File to store PID"),
    CLIENT_ADVERTISE(null, "[STRING] Client URL to advertise to other servers"),

    // Server Clustering Options
    NO_ADVERTISE(null, "[BOOL] Advertise known cluster IPs to clients"),
    CLUSTER_ADVERTISE(null, "[STRING] Cluster URL to advertise to other servers"),
    CONNECT_RETRIES(null, "[INT] For implicit routes, number of connect retries"),

    //NATS Server Options
    ADDR("0.0.0.0", "[STRING] Bind to host address (default: 0.0.0.0)"),
    PORT(4222, "[INT] Use port for clients (default: 4222)"),
    HTTP_PORT(null, "[INT] Use port for http monitoring"),
    HTTPS_PORT(null, "[INT] Use port for https monitoring"),
    CONFIG(null, "[STRING] Configuration file"),

    //NATS Server Logging Options
    LOG(null, "[STRING] File to redirect log output"),
    TRACE(null, "[/] Trace the raw protocol"),
    DEBUG(null, "[/] Enable debugging output"),
    SYSLOG(null, "[/] Log to syslog or windows event log"),
    LOGTIME(null, "[/] Timestamp log entries (default: true)"),
    REMOTE_SYSLOG(null, "[STRING] Syslog server addr (udp://localhost:514)"),

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
    CLUSTER(null, "[STRING] Cluster URL for solicited routes");

    private final Object defaultValue;
    private final String description;

    NatsConfig(Object defaultValue, String description) {
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Command line property key
     *
     * @return key for command line
     */
    public String getKey() {
        String key = name().toLowerCase();
        key = getDescription().startsWith("-") ? "-" + key : "--" + key;
        key = getDescription().startsWith("[BOOL]") ? key + "=" : key + " ";
        return key;
    }
}

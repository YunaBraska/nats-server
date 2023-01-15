package berlin.yuna.natsserver.logic;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class NatsOptions {

    protected final int port;
    protected final boolean jetStream;
    protected final boolean debug;
    protected final Path configFile;
    protected final String[] customArgs;
    protected final Logger logger;
    protected final Level loggingLevel;

    /**
     * Construct and start the Nats Server
     */
    public NatsOptions() {
        this(-1, false, false, null, null, null, null);
    }

    /**
     * Construct and start the Nats Server
     *
     * @param port the port to start on or &lt;=0 to use an automatically allocated port
     */
    public NatsOptions(final int port) {
        this(port, false, false, null, null, null, null);
    }

    /**
     * Construct and start the Nats Server
     *
     * @param port      the port to start on or &lt;=0 to use an automatically allocated port
     * @param jetStream whether to enable JetStream
     */
    public NatsOptions(final int port, final boolean jetStream) {
        this(port, jetStream, false, null, null, null, null);
    }

    /**
     * Construct and start the Nats Server
     *
     * @param port       the port to start on or &lt;=0 to use an automatically allocated port
     * @param jetStream  whether to enable JetStream
     * @param configFile path to a custom config file
     */
    public NatsOptions(final int port, final boolean jetStream, final Path configFile) {
        this(port, jetStream, false, configFile, null, null, null);
    }

    /**
     * Construct and start the Nats Server
     *
     * @param port       the port to start on or &lt;=0 to use an automatically allocated port
     * @param jetStream  whether to enable JetStream
     * @param configFile path to a custom config file
     * @param customArgs any custom args to add to the command line
     */
    public NatsOptions(final int port, final boolean jetStream, final Path configFile, final String[] customArgs) {
        this(port, jetStream, false, configFile, customArgs, null, null);
    }

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
     */
    public NatsOptions(
            final int port,
            final boolean jetStream,
            final boolean debug,
            final Path configFile,
            final String[] customArgs,
            final Level loggingLevel,
            final Logger logger
    ) {
        this.port = port;
        this.jetStream = jetStream;
        this.debug = debug;
        this.configFile = configFile;
        this.customArgs = customArgs;
        this.logger = logger;
        this.loggingLevel = loggingLevel;
    }

    public int port() {
        return port;
    }

    public boolean jetStream() {
        return jetStream;
    }

    public boolean debug() {
        return debug;
    }

    public Path configFile() {
        return configFile;
    }

    public String[] customArgs() {
        return customArgs;
    }

    public Logger logger() {
        return logger;
    }

    public Level loggingLevel() {
        return loggingLevel;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final NatsOptions that = (NatsOptions) o;
        return port == that.port && jetStream == that.jetStream && debug == that.debug && Objects.equals(configFile, that.configFile) && Arrays.equals(customArgs, that.customArgs) && Objects.equals(logger, that.logger) && Objects.equals(loggingLevel, that.loggingLevel);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(port, jetStream, debug, configFile, loggingLevel);
        result = 31 * result + Arrays.hashCode(customArgs);
        return result;
    }

    @Override
    public String toString() {
        return "NatsOptions{" +
                "port=" + port +
                ", jetStream=" + jetStream +
                ", debug=" + debug +
                ", configFile=" + configFile +
                ", loggingLevel=" + loggingLevel +
                '}';
    }
}

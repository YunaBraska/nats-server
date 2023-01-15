package berlin.yuna.natsserver.logic;

import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class NatsInterface implements AutoCloseable {

    protected Process process;
    protected Logger logger;
    protected String[] customArgs;

    public NatsInterface(final NatsOptions natsOptions) {
        this.logger = Optional.ofNullable(natsOptions.logger()).orElse(Logger.getLogger(NatsInterface.class.getSimpleName()));
        if (natsOptions.loggingLevel != null) {
            logger.setLevel(natsOptions.loggingLevel);
        }
        customArgs = natsOptions.customArgs != null ? natsOptions.customArgs : new String[]{};
    }

    public Process process() {
        return process;
    }

    public String[] customArgs() {
        return customArgs;
    }

    public Logger logger() {
        return logger;
    }

    public Level loggingLevel() {
        return logger.getLevel();
    }

    /**
     * @return Path to binary file
     */
    public abstract Path binary();

    public abstract String url();

    public abstract int port();

    public abstract boolean jetStream();

    public abstract boolean debug();

    public abstract Path configFile();
}

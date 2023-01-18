package berlin.yuna.natsserver.config;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface NatsInterface extends AutoCloseable {

    Process process();

    String[] customArgs();

    Logger logger();

    Level loggingLevel();

    Path binary();

    String url();

    int port();

    boolean jetStream();

    boolean debug();

    Path configFile();
}

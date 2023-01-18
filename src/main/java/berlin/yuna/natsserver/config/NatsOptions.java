package berlin.yuna.natsserver.config;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface NatsOptions {

    Integer port();

    Boolean jetStream();

    Boolean debug();

    Path configFile();

    String[] customArgs();

    Logger logger();

    Level logLevel();
}

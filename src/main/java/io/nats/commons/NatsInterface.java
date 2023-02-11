package io.nats.commons;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

//Coming soon from nats.io
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

package io.nats.commons;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

//Coming soon from nats.io
public interface NatsOptions {

    Integer port();

    Boolean jetStream();

    Boolean debug();

    Path configFile();

    String[] customArgs();

    Logger logger();

    Level logLevel();
}

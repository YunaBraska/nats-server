package berlin.yuna.natsserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Arrays.stream;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class NatsOptionsBuilderTest {

    @Test
    @DisplayName("Empty Builder Test")
    void testEmptyBuilder() {
        final NatsOptions natsOptions = NatsOptions.natsBuilder().build();
        stream(NatsConfig.values()).forEach(key -> assertThat(natsOptions.config().get(key), is(nullValue())));
    }

    @Test
    void testBuilder() {
        final var logger = Logger.getLogger(NatsOptionsBuilderTest.class.getSimpleName());
        final int port = 1234;
        final var logLevel = Level.FINE;
        final var timeoutMs = 123456L;
        final var options = NatsOptions.natsBuilder();
        final var configFile = Path.of("AA/BB");
        final var customArgs = new String[]{"CC", "DD"};

        //CONFIG_MAP
        assertThat(options.configMap(), is(notNullValue()));
        assertThat(options.configMap().size(), is(0));
        options.configMap(Map.of(NatsConfig.ADDR, "0.0.0.0"));
        assertThat(options.configMap().size(), is(1));
        options.config(NatsConfig.ADDR, "1.2.3.4");
        assertThat(options.configMap().get(NatsConfig.ADDR), is("1.2.3.4"));
        options.config("ADDR", "5.6.7.8");
        assertThat(options.configMap().get(NatsConfig.ADDR), is("5.6.7.8"));

        //PORT
        assertThat(options.port(), is(nullValue()));
        options.port(port);
        assertThat(options.port(), is(equalTo(port)));

        //JETSTREAM
        assertThat(options.jetStream(), is(nullValue()));
        options.jetStream(false);
        assertThat(options.jetStream(), is(equalTo(false)));

        options.jetStream(true);
        assertThat(options.jetStream(), is(equalTo(true)));

        //DEBUG
        assertThat(options.debug(), is(nullValue()));
        options.debug(false);
        assertThat(options.debug(), is(equalTo(false)));

        options.debug(true);
        assertThat(options.debug(), is(equalTo(true)));

        //AUTOSTART
        assertThat(options.autostart(), is(nullValue()));
        options.autostart(false);
        assertThat(options.autostart(), is(equalTo(false)));

        options.autostart(true);
        assertThat(options.autostart(), is(equalTo(true)));

        //CONFIG_FILE
        assertThat(options.configFile(), is(nullValue()));
        options.configFile(configFile);
        assertThat(options.configFile(), is(equalTo(configFile)));

        //CONFIG_PROPERTY_FILE
        assertThat(options.configPropertyFile(), is(nullValue()));
        options.configPropertyFile(configFile);
        assertThat(options.configPropertyFile(), is(equalTo(configFile)));

        //CUSTOM_ARGS
        assertThat(options.customArgs(), is(nullValue()));
        options.addArgs("aa", "bb");
        options.addArgs("cc");
        assertThat(options.customArgs(), is(equalTo(new String[]{"aa", "bb", "cc"})));

        options.customArgs(customArgs);
        assertThat(options.customArgs(), is(equalTo(customArgs)));

        //LOGGER
        assertThat(options.logger(), is(nullValue()));
        options.logger(logger);
        assertThat(options.logger(), is(equalTo(logger)));

        //LOG_LEVEL
        assertThat(options.logLevel(), is(nullValue()));
        options.logLevel(logLevel);
        assertThat(options.logLevel(), is(equalTo(logLevel)));

        //TIMEOUT_MS
        assertThat(options.timeoutMs(), is(nullValue()));
        options.timeoutMs(timeoutMs);
        assertThat(options.timeoutMs(), is(equalTo(timeoutMs)));

        //OPTIONS BUILD
        assertThat(options.configMap().size(), is(10));
        final var build = options.build();
        assertThat(build.config().size(), is(10));

        assertThat(build.port(), is(equalTo(port)));
        assertThat(build.jetStream(), is(equalTo(true)));
        assertThat(build.debug(), is(equalTo(true)));
        assertThat(build.config().get(NatsConfig.NATS_AUTOSTART), is(equalTo("true")));
        assertThat(build.configFile(), is(equalTo(configFile)));
        assertThat(build.config().get(NatsConfig.NATS_PROPERTY_FILE), is(equalTo(configFile.toString())));
        assertThat(build.customArgs(), is(equalTo(customArgs)));
        assertThat(build.logger(), is(equalTo(logger)));
        assertThat(build.logLevel(), is(equalTo(logLevel)));
        assertThat(build.config().get(NatsConfig.NATS_TIMEOUT_MS), is(equalTo(String.valueOf(timeoutMs))));

        //OPTIONS INTERFACE
        assertThat(options.configMap().size(), is(10));
        final var interFace = (io.nats.commons.NatsOptions) options.build();

        assertThat(interFace.port(), is(equalTo(port)));
        assertThat(interFace.jetStream(), is(equalTo(true)));
        assertThat(interFace.debug(), is(equalTo(true)));
        assertThat(interFace.configFile(), is(equalTo(configFile)));
        assertThat(interFace.customArgs(), is(equalTo(customArgs)));
        assertThat(interFace.logger(), is(equalTo(logger)));
        assertThat(interFace.logLevel(), is(equalTo(logLevel)));
    }

    @Test
    @DisplayName("Coverage Test")
    void coverageTest() {
        final var builder1 = NatsOptions.natsBuilder().logger(Logger.getLogger("AA"));
        final var builder2 = NatsOptions.natsBuilder().logger(Logger.getLogger("BB"));
        assertThat(builder1, is(equalTo(builder1)));
        assertThat(builder1, is(not(equalTo(builder2))));
        assertThat(builder1.hashCode(), is(not(0)));

        assertThat(builder1.build(), is(equalTo(builder1.build())));
        assertThat(builder1.build(), is(not(equalTo(builder2.build()))));
        assertThat(builder1.build().hashCode(), is(not(0)));
    }
}

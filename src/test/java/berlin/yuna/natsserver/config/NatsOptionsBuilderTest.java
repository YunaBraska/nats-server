package berlin.yuna.natsserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("UnitTest")
@DisplayName("NatsServer Builder Test")
class NatsOptionsBuilderTest {

    @Test
    @DisplayName("Empty Builder Test")
    void testEmptyBuilder() {
        final NatsOptions natsOptions = NatsOptions.natsBuilder().build();
        stream(NatsConfig.values()).forEach(key -> assertThat(natsOptions.config().get(key)).isNull());
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
        final var version = NatsVersion.values()[0];

        assertThat(options.configMap()).isNotNull().isEmpty();
        options.configMap(Map.of(NatsConfig.NET, "0.0.0.0"));
        assertThat(options.configMap()).hasSize(1);
        options.config(NatsConfig.NET, "1.2.3.4");
        assertThat(options.configMap().get(NatsConfig.NET)).isEqualTo("1.2.3.4");
        options.config("NET", "5.6.7.8");
        assertThat(options.configMap().get(NatsConfig.NET)).isEqualTo("5.6.7.8");

        assertThat(options.port()).isNull();
        options.port(port);
        assertThat(options.port()).isEqualTo(port);

        assertThat(options.version()).isNull();
        options.version("123");
        assertThat(options.version()).isEqualTo("v123");
        options.version(version);
        assertThat(options.version()).isEqualTo(version.value());

        assertThat(options.jetStream()).isNull();
        options.jetStream(false);
        assertThat(options.jetStream()).isFalse();
        options.jetStream(true);
        assertThat(options.jetStream()).isTrue();

        assertThat(options.debug()).isNull();
        options.debug(false);
        assertThat(options.debug()).isFalse();
        options.debug(true);
        assertThat(options.debug()).isTrue();

        assertThat(options.autostart()).isNull();
        options.autostart(false);
        assertThat(options.autostart()).isFalse();

        assertThat(options.shutdownHook()).isNull();
        options.shutdownHook(false);
        assertThat(options.shutdownHook()).isFalse();
        options.shutdownHook(true);
        assertThat(options.shutdownHook()).isTrue();

        options.autostart(true);
        assertThat(options.autostart()).isTrue();

        assertThat(options.configFile()).isNull();
        options.configFile(configFile);
        assertThat(options.configFile()).isEqualTo(configFile);

        assertThat(options.configPropertyFile()).isNull();
        options.configPropertyFile(configFile);
        assertThat(options.configPropertyFile()).isEqualTo(configFile);

        assertThat(options.customArgs()).isNull();
        options.addArgs("aa", "bb");
        options.addArgs("cc");
        assertThat(options.customArgs()).containsExactly("aa", "bb", "cc");

        options.customArgs(customArgs);
        assertThat(options.customArgs()).containsExactly(customArgs);

        assertThat(options.logger()).isNull();
        options.logger(logger);
        assertThat(options.logger()).isSameAs(logger);

        assertThat(options.logLevel()).isNull();
        options.logLevel(logLevel);
        assertThat(options.logLevel()).isEqualTo(logLevel);

        assertThat(options.timeoutMs()).isNull();
        options.timeoutMs(timeoutMs);
        assertThat(options.timeoutMs()).isEqualTo(timeoutMs);

        assertThat(options.configMap()).hasSize(12);
        final var build = options.build();
        assertThat(build.config()).hasSize(12);

        assertThat(build.version()).isEqualTo(version.value());
        assertThat(build.port()).isEqualTo(port);
        assertThat(build.jetStream()).isTrue();
        assertThat(build.debug()).isTrue();
        assertThat(build.config().get(NatsConfig.NATS_AUTOSTART)).isEqualTo("true");
        assertThat(build.configFile()).isEqualTo(configFile);
        assertThat(build.config().get(NatsConfig.NATS_PROPERTY_FILE)).isEqualTo(configFile.toString());
        assertThat(build.customArgs()).containsExactly(customArgs);
        assertThat(build.logger()).isSameAs(logger);
        assertThat(build.logLevel()).isEqualTo(logLevel);
        assertThat(build.config().get(NatsConfig.NATS_TIMEOUT_MS)).isEqualTo(String.valueOf(timeoutMs));

        assertThat(options.configMap()).hasSize(12);
        final var interFace = (io.nats.commons.NatsOptions) options.build();

        assertThat(interFace.port()).isEqualTo(port);
        assertThat(interFace.jetStream()).isTrue();
        assertThat(interFace.debug()).isTrue();
        assertThat(interFace.configFile()).isEqualTo(configFile);
        assertThat(interFace.customArgs()).containsExactly(customArgs);
        assertThat(interFace.logger()).isSameAs(logger);
        assertThat(interFace.logLevel()).isEqualTo(logLevel);
    }

    @Test
    @DisplayName("Coverage Test")
    void coverageTest() {
        final var builder1 = NatsOptions.natsBuilder().logger(Logger.getLogger("AA"));
        final var builder2 = NatsOptions.natsBuilder().logger(Logger.getLogger("BB"));
        assertThat(builder1).isEqualTo(builder1);
        assertThat(builder1).isNotEqualTo(builder2);
        assertThat(builder1.hashCode()).isNotZero();

        assertThat(builder1.build()).isEqualTo(builder1.build());
        assertThat(builder1.build()).isNotEqualTo(builder2.build());
        assertThat(builder1.build().hashCode()).isNotZero();
    }
}
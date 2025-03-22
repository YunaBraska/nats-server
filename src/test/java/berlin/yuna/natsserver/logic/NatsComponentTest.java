package berlin.yuna.natsserver.logic;

import berlin.yuna.natsserver.config.NatsConfig;
import berlin.yuna.natsserver.config.NatsOptions;
import berlin.yuna.natsserver.config.NatsOptionsBuilder;
import berlin.yuna.natsserver.model.exception.NatsDownloadException;
import berlin.yuna.natsserver.model.exception.NatsFileReaderException;
import berlin.yuna.natsserver.model.exception.NatsStartException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import static berlin.yuna.natsserver.config.NatsConfig.*;
import static berlin.yuna.natsserver.config.NatsOptions.natsBuilder;
import static berlin.yuna.natsserver.model.MapValue.mapValueOf;
import static berlin.yuna.natsserver.model.ValueSource.ENV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
@DisplayName("NatsServer plain java")
@SuppressWarnings("resource")
class NatsComponentTest {

    @Test
    @DisplayName("No start without annotation")
    void natsServer_withoutAnnotation_shouldNotBeStarted() {
        assertThrows(
                ConnectException.class,
                () -> new Socket("localhost", 4245).close(),
                "Connection refused"
        );
    }

    @Test
    @DisplayName("Default config")
    void natsServer_withoutConfig_shouldStartWithDefaultValues() {
        final var nats = new Nats();
        assertThat(nats.pid()).isGreaterThan(-1);
    }

    @Test
    @DisplayName("Default config and port")
    void natsServer_withoutConfigAndPort_shouldStartWithDefaultValues() {
        final var nats = new Nats(-1);
        assertThat(nats.pid()).isGreaterThan(-1);
    }

    @Test
    @DisplayName("Default config JetStream")
    void natsServer_withJetStream_shouldStartWithDefaultValues() {
        final var nats = new Nats(testConfig().config(JETSTREAM, "true"));
        assertThat(nats.pid()).isGreaterThan(-1);
    }

    @Test
    @DisplayName("Deactivate JetStream")
    void natsServer_withJetStreamFalse_shouldNotUseJetStream() {
        try (final var nats = new Nats(testConfig().config(JETSTREAM, "true"))) {
            assertThat(nats.getValue(JETSTREAM)).isNotNull();
            assertThat(nats.jetStream()).isTrue();
            assertThat(nats.debug()).isFalse();
        }
        try (final var nats = new Nats(testConfig().config(JETSTREAM, "false"))) {
            assertThat(nats.getValue(JETSTREAM)).isEqualTo("false");
            assertThat(nats.jetStream()).isFalse();
            assertThat(nats.debug()).isFalse();
        }
    }

    @Test
    @DisplayName("Setup config")
    void natsServer_shouldShutdownGracefully() throws Exception {
        final var port = new AtomicInteger(-99);
        try (final var nats = new Nats(testConfig().config("user", "adminUser", "PAss", "adminPw"))) {
            assertThat(nats.port()).isGreaterThan(0);
            port.set(nats.port());
            new Socket("localhost", port.get()).close();
        }
        assertThrows(
                ConnectException.class,
                () -> new Socket("localhost", port.get()).close(),
                "Connection refused"
        );
    }

    @Test
    @DisplayName("Unknown config is ignored")
    void natsServer_invalidConfig_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Nats(testConfig().config("user", "adminUser", "auth", "isValid", "", "password", " "))
        );
    }

    @Test
    @DisplayName("Duplicate starts will be ignored")
    void natsServer_duplicateStart_shouldNotRunIntroExceptionOrInterrupt() {
        final var nats = new Nats(testConfig()).start().start().start();
        assertThat(nats.pid()).isGreaterThan(-1);
    }

    @Test
    @DisplayName("Unknown config [FAIL]")
    void natsServer_withWrongConfig_shouldNotStartAndThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Nats(testConfig().config("unknown", "config", "port", "4232")),
                "No enum constant"
        );
    }

    @Test
    @DisplayName("Duplicate instances [FAIL]")
    void natsServer_asTwoInstances_shouldThrowBindException() {
        final NatsOptions config = natsBuilder().port(4500).timeoutMs(2000).build();
        new Nats(config);
        assertThrows(
                NatsStartException.class,
                () -> new Nats(config),
                "Address already in use [4500]"
        );
    }

    @Test
    @DisplayName("Stop without start will be ignored")
    void natsServer_stopWithoutStart_shouldNotRunIntroException() {
        final var nats = NatsOptions.natsBuilder().autostart(false).nats();
        nats.close();
        assertThat(nats.pid()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Start multiple times")
    void natsServer_multipleTimes_shouldBeOkay() {
        final Nats nats1 = new Nats(-1);
        final int pid1 = nats1.pid();
        nats1.close();
        final Nats nats2 = new Nats(-1);
        final int pid2 = nats2.pid();
        nats2.close();
        final Nats nats3 = new Nats(-1);
        final int pid3 = nats3.pid();
        nats3.close();

        assertThat(pid1).isNotEqualTo(pid2);
        assertThat(pid2).isNotEqualTo(pid3);
        assertThat(pid3).isNotEqualTo(pid1);
    }

    @Test
    @DisplayName("Start in parallel")
    void natsServer_inParallel_shouldBeOkay() {
        final Nats nats1 = new Nats(-1);
        final Nats nats2 = new Nats(-1);
        assertThat(nats1.pid()).isNotEqualTo(nats2.pid());
        assertThat(nats1.port()).isNotEqualTo(nats2.port());
        assertThat(nats1.pidFile()).isNotEqualTo(nats2.pidFile());
        assertThat(Files.exists(nats1.pidFile())).isTrue();
        assertThat(Files.exists(nats2.pidFile())).isTrue();
        nats1.close();
        nats2.close();
    }

    @Test
    @DisplayName("Configure with NULL value should be ignored")
    void natsServer_withNullableConfigValue_shouldNotRunIntroExceptionOrInterrupt() {
        new Nats(testConfig().config(NET, null));
    }

    @Test
    @DisplayName("Configure with invalid config value [FAIL]")
    void natsServer_withInvalidConfigValue_shouldNotRunIntroExceptionOrInterrupt() {
        assertThrows(
                NatsStartException.class,
                () -> new Nats(testConfig().config(PROFILE.name(), "invalidValue", PORT.name(), "4237")),
                "NatsServer failed to start"
        );
    }

    @Test
    @DisplayName("Configure without value param")
    void natsServer_withoutValue() {
        final var nats = new Nats(testConfig().config(TRACE, "true").config(DEBUG, "true"));
        assertThat(nats.pid()).isGreaterThan(-1);
    }

    @Test
    @DisplayName("Cov dummy")
    void covDummy() {
        assertThat(JETSTREAM.type()).isEqualTo(NatsConfig.SilentBoolean.class);
        assertThat(mapValueOf(ENV, "some value").toString()).isNotNull();
        assertThat(new NatsFileReaderException("dummy", new RuntimeException())).isNotNull();
        assertThat(new NatsStartException(new RuntimeException())).isNotNull();
        assertThat(new NatsDownloadException("new RuntimeException()")).isNotNull();
        assertThat(new NatsConfig.SilentBoolean().getAndSet(true)).isFalse();
    }

    private NatsOptionsBuilder testConfig() {
        return natsBuilder().config(PORT, "-1");
    }
}
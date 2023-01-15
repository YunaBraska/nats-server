package berlin.yuna.natsserver.logic;

import berlin.yuna.natsserver.config.NatsConfig;
import berlin.yuna.natsserver.model.exception.NatsDownloadException;
import berlin.yuna.natsserver.model.exception.NatsFileReaderException;
import berlin.yuna.natsserver.model.exception.NatsStartException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.BindException;
import java.net.ConnectException;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static berlin.yuna.natsserver.config.NatsConfig.ADDR;
import static berlin.yuna.natsserver.config.NatsConfig.AUTH;
import static berlin.yuna.natsserver.config.NatsConfig.DEBUG;
import static berlin.yuna.natsserver.config.NatsConfig.JETSTREAM;
import static berlin.yuna.natsserver.config.NatsConfig.PASS;
import static berlin.yuna.natsserver.config.NatsConfig.PORT;
import static berlin.yuna.natsserver.config.NatsConfig.PROFILE;
import static berlin.yuna.natsserver.config.NatsConfig.TRACE;
import static berlin.yuna.natsserver.config.NatsConfig.USER;
import static berlin.yuna.natsserver.logic.NatsOptionsImpl.defaultConfig;
import static berlin.yuna.natsserver.model.MapValue.mapValueOf;
import static berlin.yuna.natsserver.model.ValueSource.ENV;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
@DisplayName("NatsServer plain java")
class NatsComponentTest {

    private Nats nats;

    @BeforeEach
    void setUp() throws Exception {
        nats = new Nats(-1);
    }

    @AfterEach
    void afterEach() {
        nats.close();
    }

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
        nats.tryStart();
        assertThat(nats.pid(), is(greaterThan(-1)));
    }

    @Test
    @DisplayName("Default config JetStream")
    void natsServer_withJetStream_shouldStartWithDefaultValues() throws Exception {
        nats = new Nats(defaultConfig().config(JETSTREAM, "true"));
        nats.tryStart();
        assertThat(nats.pid(), is(greaterThan(-1)));
    }

    @Test
    @DisplayName("Deactivate JetStream")
    void natsServer_withJetStreamFalse_shouldNotUseJetStream() throws Exception {
        nats = new Nats(defaultConfig().config(JETSTREAM, "true"));
        assertThat(nats.getValue(JETSTREAM), is(notNullValue()));
        nats = new Nats(defaultConfig().config(JETSTREAM, "false"));
        assertThat(nats.getValue(JETSTREAM), is("false"));
    }

    @Test
    @DisplayName("Setup config")
    void natsServer_configureConfig_shouldNotOverwriteOldConfig() throws Exception {
        nats = new Nats(defaultConfig().config("user", "adminUser", "PAss", "adminPw"));

        assertThat(nats.getValue(USER), is(equalTo("adminUser")));
        assertThat(nats.getValue(PASS), is(equalTo("adminPw")));

        nats = new Nats(defaultConfig().config("user", "newUser"));
        assertThat(nats.getValue(USER), is(equalTo("newUser")));
        assertThat(nats.getValue(PASS), is("adminPw"));

        final Map<NatsConfig, String> newConfig = new HashMap<>();
        newConfig.put(USER, "oldUser");
        nats = new Nats(NatsOptionsImpl.builder().configMap(newConfig).build());
        assertThat(nats.getValue(USER), is(equalTo("oldUser")));
    }

    @Test
    @DisplayName("Unknown config is ignored")
    void natsServer_invalidConfig_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> new Nats(defaultConfig().config("user", "adminUser", "auth", "isValid", "", "password", " ")));
        assertThat(nats.getValue(AUTH), is(equalTo("isValid")));
    }

    @Test
    @DisplayName("Duplicate starts will be ignored")
    void natsServer_duplicateStart_shouldNotRunIntroExceptionOrInterrupt() throws Exception {
        nats.start();
        nats.start();
        assertThat(nats.pid(), is(greaterThan(-1)));
    }

    @Test
    @DisplayName("Unknown config [FAIL]")
    void natsServer_withWrongConfig_shouldNotStartAndThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Nats(defaultConfig().config("unknown", "config", "port", "4232")),
                "No enum constant"
        );
    }

    @Test
    @DisplayName("Duplicate instances [FAIL]")
    void natsServer_asTwoInstances_shouldThrowBindException() throws Exception {
        final Nats nats_Java_one = new Nats(4500);
        final Nats nats_Java_two = new Nats(4500);
        Exception exception = null;
        try {
            nats_Java_one.start();
            nats_Java_two.start();
        } catch (Exception e) {
            exception = e;
        } finally {
            nats_Java_one.close();
            nats_Java_two.close();
        }
        assertThat(requireNonNull(exception).getClass().getSimpleName(), is(equalTo(BindException.class.getSimpleName())));
    }

    @Test
    @DisplayName("Stop without start will be ignored")
    void natsServer_stopWithoutStart_shouldNotRunIntroException() {
        nats.close();
        assertThat(nats.pid(), is(-1));
    }

    @Test
    @DisplayName("Start multiple times")
    void natsServer_multipleTimes_shouldBeOkay() throws Exception {
        final Nats nats1 = new Nats(-1).start();
        final int pid1 = nats1.pid();
        nats1.close();
        final Nats nats2 = new Nats(-1).start();
        final int pid2 = nats2.pid();
        nats2.close();
        final Nats nats3 = new Nats(-1).start();
        final int pid3 = nats3.pid();
        nats3.close();

        assertThat(pid1, is(not(equalTo(pid2))));
        assertThat(pid2, is(not(equalTo(pid3))));
        assertThat(pid3, is(not(equalTo(pid1))));
    }

    @Test
    @DisplayName("Start in parallel")
    void natsServer_inParallel_shouldBeOkay() throws Exception {
        final Nats nats1 = new Nats(-1).start();
        final Nats nats2 = new Nats(-1).start();
        assertThat(nats1.pid(), is(not(equalTo(nats2.pid()))));
        assertThat(nats1.port(), is(not(equalTo(nats2.port()))));
        assertThat(nats1.pidFile(), is(not(equalTo(nats2.pidFile()))));
        assertThat(Files.exists(nats1.pidFile()), is(true));
        assertThat(Files.exists(nats2.pidFile()), is(true));
        nats1.close();
        nats2.close();
    }

    @Test
    @DisplayName("Configure with NULL value should be ignored")
    void natsServer_withNullableConfigValue_shouldNotRunIntroExceptionOrInterrupt() {
        assertThrows(NullPointerException.class, () -> new Nats(defaultConfig().config(ADDR, null)));
    }

    @Test
    @DisplayName("Configure with invalid config value [FAIL]")
    void natsServer_withInvalidConfigValue_shouldNotRunIntroExceptionOrInterrupt() throws Exception {
        new Nats(defaultConfig().config(PROFILE.name(), "invalidValue", PORT.name(), "4237"));
        assertThrows(
                PortUnreachableException.class,
                () -> nats.close(),
                "NatsServer failed to start"
        );
    }

    @Test
    @DisplayName("Configure without value param")
    void natsServer_withoutValue() throws Exception {
        nats = new Nats(defaultConfig().config(TRACE, "true").config(DEBUG, "true"));
        nats.start();
        assertThat(nats.pid(), is(greaterThan(-1)));
    }


    @Test
    @DisplayName("Cov dummy")
    void covDummy() {
        nats.tryStart();
        nats.close();
        assertThat(mapValueOf(ENV, "some value").toString(), is(notNullValue()));
        assertThat(new NatsFileReaderException("dummy", new RuntimeException()), is(notNullValue()));
        assertThat(new NatsStartException(new RuntimeException()), is(notNullValue()));
        assertThat(new NatsDownloadException(new RuntimeException()), is(notNullValue()));
    }
}

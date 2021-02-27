package berlin.yuna.natsserver.logic;

import berlin.yuna.natsserver.config.NatsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingFormatArgumentException;

import static berlin.yuna.clu.logic.SystemUtil.OperatingSystem.LINUX;
import static berlin.yuna.clu.logic.SystemUtil.OperatingSystem.WINDOWS;
import static berlin.yuna.clu.logic.SystemUtil.getOsType;
import static berlin.yuna.natsserver.config.NatsConfig.ADDR;
import static berlin.yuna.natsserver.config.NatsConfig.AUTH;
import static berlin.yuna.natsserver.config.NatsConfig.PASS;
import static berlin.yuna.natsserver.config.NatsConfig.PORT;
import static berlin.yuna.natsserver.config.NatsConfig.TRACE;
import static berlin.yuna.natsserver.config.NatsConfig.USER;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("UnitTest")
@DisplayName("NatsServer plain java")
class NatsComponentTest {

    private String natsSource;
    private static final String USER_DIR = System.getProperty("user.dir");

    @BeforeEach
    void setUp() {
        natsSource = getOsType().equals(LINUX) ?
                "file://" + USER_DIR + "/src/test/resources/natsserver/linux.zip" :
                "file://" + USER_DIR + "/src/test/resources/natsserver/mac.zip";
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
        Nats nats = new Nats().config(ADDR, "localhost").port(4238).source(natsSource);
        assertThat(nats.source(), is(equalTo(natsSource)));
        nats.tryStart(SECONDS.toMillis(10));
        nats.stop();
        assertThat(nats.toString().length(), is(greaterThan(1)));
    }

    @Test
    @DisplayName("Setup config")
    void natsServer_configureConfig_shouldNotOverwriteOldConfig() {
        Nats nats = new Nats(4240).source(natsSource);
        nats.config("user:adminUser", "PAss:adminPw");

        assertThat(nats.config().get(USER), is(equalTo("adminUser")));
        assertThat(nats.config().get(PASS), is(equalTo("adminPw")));

        nats.config("user:newUser");
        assertThat(nats.config().get(USER), is(equalTo("newUser")));
        assertThat(nats.config().get(PASS), is("adminPw"));

        Map<NatsConfig, String> newConfig = new HashMap<>();
        newConfig.put(USER, "oldUser");
        nats.config(newConfig);
        assertThat(nats.config().get(USER), is(equalTo("oldUser")));
    }

    @Test
    @DisplayName("Unknown config is ignored")
    void natsServer_invalidConfig_shouldNotRunIntroException() {
        Nats nats = new Nats(4244).source(natsSource);
        nats.config("user:adminUser:password", " ", "auth:isValid", "");
        assertThat(nats.config().size(), is(3));
        assertThat(nats.config().get(AUTH), is(equalTo("isValid")));
    }

    @Test
    @DisplayName("Duplicate starts will be ignored")
    void natsServer_duplicateStart_shouldNotRunIntroExceptionOrInterrupt() throws IOException {
        Nats nats = new Nats(4231).source(natsSource);
        nats.start();
        nats.start(SECONDS.toMillis(10));
        nats.stop(SECONDS.toMillis(10));
    }

    @Test
    @DisplayName("Unknown config [FAIL]")
    void natsServer_withWrongConfig_shouldNotStartAndThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Nats("unknown:config", "port:4232").source(natsSource),
                "No enum constant"
        );
    }

    @Test
    @DisplayName("Duplicate instances [FAIL]")
    void natsServer_asTwoInstances_shouldThrowBindException() {
        Nats nats_Java_one = new Nats(4233).source(natsSource);
        Nats nats_Java_two = new Nats(4233).source(natsSource);
        Exception exception = null;
        try {
            nats_Java_one.start();
            nats_Java_two.start();
        } catch (Exception e) {
            exception = e;
        } finally {
            nats_Java_one.stop();
            nats_Java_two.stop();
        }
        assertThat(requireNonNull(exception).getClass().getSimpleName(), is(equalTo(BindException.class.getSimpleName())));
    }

    @Test
    @DisplayName("Stop without start will be ignored")
    void natsServer_stopWithoutStart_shouldNotRunIntroExceptionOrInterrupt() {
        Nats nats = new Nats(4241).source(natsSource);
        nats.stop();
    }

    @Test
    @DisplayName("Start multiple times")
    void natsServer_multipleTimes_shouldBeOkay() throws IOException {
        int pid1 = new Nats(4234).source(natsSource).start(SECONDS.toMillis(10)).stop(SECONDS.toMillis(10)).pid();
        int pid2 = new Nats(4234).source(natsSource).start(SECONDS.toMillis(10)).stop(SECONDS.toMillis(10)).pid();
        int pid3 = new Nats(4234).source(natsSource).start(SECONDS.toMillis(10)).stop(SECONDS.toMillis(10)).pid();
        assertThat(pid1, is(not(equalTo(pid2))));
        assertThat(pid2, is(not(equalTo(pid3))));
        assertThat(pid3, is(not(equalTo(pid1))));
    }

    @Test
    @DisplayName("Start in parallel")
    void natsServer_inParallel_shouldBeOkay() throws IOException {
        Nats nats1 = new Nats(4235).source(natsSource).start();
        Nats nats2 = new Nats(4236).source(natsSource).start();
        assertThat(nats1.pid(), is(not(equalTo(nats2.pid()))));
        assertThat(nats1.port(), is(not(equalTo(nats2.port()))));
        assertThat(nats1.pidFile(), is(not(equalTo(nats2.pidFile()))));
        assertThat(Files.exists(nats1.pidFile()), is(true));
        assertThat(Files.exists(nats2.pidFile()), is(true));
        nats1.stop();
        nats2.stop();
    }

    @Test
    @DisplayName("Config port with NULL [FAIL]")
    void natsServer_withNullablePortValue_shouldThrowMissingFormatArgumentException() {
        Nats nats = new Nats(4243).source(natsSource);
        nats.config().put(PORT, null);
        assertThrows(
                MissingFormatArgumentException.class,
                nats::port,
                "Could not initialise port"
        );
    }

    @Test
    @DisplayName("Configure with NULL value should be ignored")
    void natsServer_withNullableConfigValue_shouldNotRunIntroExceptionOrInterrupt() throws IOException {
        Nats nats = new Nats(4236).source(natsSource);
        nats.config().put(ADDR, null);
        nats.start();
        nats.stop();
    }

    @Test
    @DisplayName("Configure with invalid config value [FAIL]")
    void natsServer_withInvalidConfigValue_shouldNotRunIntroExceptionOrInterrupt() {
        Nats nats = new Nats(ADDR + ":invalidValue", PORT + ":4237").source(natsSource);
        assertThrows(
                PortUnreachableException.class,
                nats::start,
                "NatsServer failed to start"
        );
        nats.stop();
    }

    @Test
    @DisplayName("Validate Windows path")
    void natsServerOnWindows_shouldAddExeToPath() {
        Nats nats = new Nats(4244).source(natsSource);
        String windowsNatsServerPath = nats.getNatsServerPath(WINDOWS).toString();
        String expectedExe = nats.name.toLowerCase() + ".exe";
        assertThat(windowsNatsServerPath, containsString(expectedExe));
    }

    @Test
    @DisplayName("Config without url [FAIL]")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void natsServerWithoutSourceUrl_shouldThrowException() {
        Nats nats = new Nats(4239).source(natsSource);
        nats.getNatsServerPath(getOsType()).toFile().delete();
        nats.source(null);
        assertThrows(
                RuntimeException.class,
                nats::start,
                "Could not initialise port"
        );
        assertThrows(
                RuntimeException.class,
                nats::tryStart,
                "Could not initialise port"
        );
    }

    @Test
    @DisplayName("Configure without value param")
    void natsServer_withoutValue() throws IOException {
        Nats nats = new Nats(4242).source(natsSource);
        nats.config().put(TRACE, "true");
        nats.start();
        nats.stop();
    }

    @Test
    @DisplayName("Nats server with random port")
    void natsServer_withRandomPort() {
        Nats nats = new Nats(-1).source(natsSource);
        assertThat(nats.port(), is(not((int) PORT.getDefaultValue())));
        assertThat(nats.port(), is(greaterThan((int) PORT.getDefaultValue())));
        assertThat(nats.port(), is(lessThan((int) PORT.getDefaultValue() + 501)));
    }
}

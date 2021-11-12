package berlin.yuna.natsserver.logic;

import berlin.yuna.natsserver.config.NatsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static berlin.yuna.natsserver.config.NatsConfig.ADDR;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_CONFIG_FILE;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_DOWNLOAD_URL;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_LOG_NAME;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_SYSTEM;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_VERSION;
import static berlin.yuna.natsserver.config.NatsConfig.PORT;
import static berlin.yuna.natsserver.logic.NatsBase.NATS_PREFIX;
import static berlin.yuna.natsserver.logic.NatsUtils.getSystem;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.io.FileMatchers.anExistingFile;

@Tag("UnitTest")
@DisplayName("Nats config test")
class NatsConfigTest {

    private static final String CUSTOM_LOG_NAME = "my_nats_name";
    private static final String CUSTOM_PORT = "123456";
    private static final String CUSTOM_ADDR = "example.com";
    private static final String CUSTOM_VERSION = "1.2.3";
    private String customPropertiesFile;

    @BeforeEach
    void setUp() throws IOException {
        Files.deleteIfExists(new Nats().binaryFile());
        Arrays.stream(NatsConfig.values()).forEach(config -> {
            System.clearProperty(config.name());
            System.clearProperty(NATS_PREFIX + config.name());
        });
        customPropertiesFile = Objects.requireNonNull(getClass().getClassLoader().getResource("custom.properties")).getPath();
    }

    @Test
    @DisplayName("Nats default setup")
    void natsDefault() {
        final Nats nats = new Nats();
        assertThat(nats.pid(), is(-1));
        assertThat(nats.pidFile().toString(), is(endsWith(PORT.valueRaw() + ".pid")));
        assertThat(nats.url(), is(equalTo("nats://" + ADDR.valueRaw() + ":" + PORT.valueRaw())));
        assertThat(nats.port(), is(equalTo(PORT.valueRaw())));
        assertThat(nats.binaryFile().toString(), is(containsString(System.getProperty("java.io.tmpdir"))));
        assertThat(nats.binaryFile().toString(), is(containsString(((String) NATS_LOG_NAME.valueRaw()).toLowerCase())));
    }

    @Test
    @DisplayName("Nats env configs")
    void envConfig() {
        System.setProperty(NATS_VERSION.name(), CUSTOM_VERSION);
        System.setProperty(NATS_LOG_NAME.name(), CUSTOM_LOG_NAME);
        System.setProperty(NATS_PREFIX + NatsConfig.PORT, CUSTOM_PORT);
        System.setProperty(NATS_PREFIX + NatsConfig.ADDR, CUSTOM_ADDR);

        assertCustomConfig(new Nats());
    }

    @Test
    @DisplayName("Nats dsl configs")
    void dlsConfig() {
        final Nats nats = new Nats()
                .config(NATS_VERSION, CUSTOM_VERSION)
                .config(NATS_LOG_NAME, CUSTOM_LOG_NAME)
                .config(PORT, CUSTOM_PORT)
                .config(ADDR, CUSTOM_ADDR);

        assertCustomConfig(nats);
    }

    @Test
    @DisplayName("Nats dsl configs")
    void dslMultiConfig() {
        final Nats nats = new Nats()
                .config(
                        NATS_VERSION.name(), CUSTOM_VERSION,
                        NATS_LOG_NAME.name(), CUSTOM_LOG_NAME,
                        PORT.name(), CUSTOM_PORT,
                        ADDR.name(), CUSTOM_ADDR
                );
        assertCustomConfig(nats);
    }

    @Test
    @DisplayName("Nats property file")
    void propertyFileConfig() {
        System.setProperty(NATS_CONFIG_FILE.name(), customPropertiesFile);
        assertCustomConfig(new Nats());
    }

    @Test
    @DisplayName("Nats non existing property file")
    void propertyNonExistingFileConfig() {
        System.setProperty(NATS_CONFIG_FILE.name(), "invalid");
        assertThat(new Nats().pidFile().toString(), is(endsWith(PORT.valueRaw() + ".pid")));
    }

    @Test
    @DisplayName("Prepare command")
    void prepareCommand() {
        System.setProperty(NATS_CONFIG_FILE.name(), customPropertiesFile);
        final String command = new Nats().prepareCommand();
        assertThat(command, containsString(CUSTOM_ADDR));
        assertThat(command, containsString(CUSTOM_PORT));
        assertThat(command, containsString(CUSTOM_LOG_NAME));
        assertThat(command, containsString(getSystem()));
        assertThat(command, containsString("--customArg1=123 --customArg2=456"));
    }

    @Test
    @DisplayName("download without zip")
    void downloadNatsWithoutZip() throws IOException {
        final Path inputFile = Paths.get(customPropertiesFile);
        final Nats nats = new Nats().config(NATS_DOWNLOAD_URL, inputFile.toUri().toString());

        final Path path = nats.downloadNats();
        assertThat(nats.binaryFile().toFile(), is(anExistingFile()));
        assertThat(Files.readAllLines(nats.binaryFile()), is(equalTo(Files.readAllLines(inputFile))));
        System.out.println(path);
    }

    @Test
    @DisplayName("download with zip")
    void downloadNatsWithZip() throws IOException {
        final Path inputFile = Paths.get(customPropertiesFile);
        final Path inputZipFile = zipFile(inputFile);
        final Nats nats = new Nats().config(NATS_DOWNLOAD_URL, inputZipFile.toUri().toString());

        final Path path = nats.downloadNats();
        assertThat(nats.binaryFile().toFile(), is(anExistingFile()));
        assertThat(Files.readAllLines(nats.binaryFile()), is(equalTo(Files.readAllLines(inputFile))));
        System.out.println(path);
    }

    @Test
    @DisplayName("no download if binary exists")
    void noDownloadIfExists() throws IOException {
        final Path inputFile = Paths.get(customPropertiesFile);
        final Nats nats = new Nats().config(NATS_DOWNLOAD_URL, inputFile.toUri().toString());

        Files.write(nats.binaryFile(), "Should not be overwritten".getBytes());
        final Path path = nats.downloadNats();

        assertThat(nats.binaryFile().toFile(), is(anExistingFile()));
        assertThat(Files.readAllLines(nats.binaryFile()), is(equalTo(asList("Should not be overwritten"))));
        System.out.println(path);
    }

    @Test
    @DisplayName("findFreePort")
    void findFreePort() {
        final Nats nats = new Nats();
        assertThat(nats.port(), is(equalTo(PORT.valueRaw())));
        nats.config(PORT, "-1").setNextFreePort();
        assertThat(nats.port(), is(greaterThan((int) PORT.valueRaw())));
    }

    @Test
    @DisplayName("delete pid file")
    void deletePidFile() throws IOException {
        final Nats nats = new Nats();
        Files.createFile(nats.pidFile());
        assertThat(nats.pidFile().toFile(), is(anExistingFile()));
        nats.deletePidFile();
        assertThat(nats.pidFile().toFile(), is(not(anExistingFile())));
    }

    @Test
    @DisplayName("to String")
    void toStringTest() {
        assertThat(new Nats().toString(), containsString(String.valueOf(PORT.valueRaw())));
    }

    @Test
    @DisplayName("Constructor with customArgs")
    void constructor_customArgs() {
        final Nats nats = new Nats(Arrays.asList("--arg1=false", "--arg2=true"));
        nats.args("--arg3=null");
        assertThat(nats.args(), hasItems("--arg1=false", "--arg2=true", "--arg3=null"));
        assertThat(nats.prepareCommand(), containsString("--arg1=false --arg2=true --arg3=null"));
    }

    @Test
    @DisplayName("Constructor with customArgs")
    void constructor_port() {
        final Nats nats = new Nats(123456);
        assertThat(nats.prepareCommand(), containsString(CUSTOM_PORT));
    }

    private Path zipFile(final Path source) throws IOException {
        final String result = source.toString() + ".zip";
        try (final ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(result))) {
            zipOut.putNextEntry(new ZipEntry(source.getFileName().toString()));
            Files.copy(source, zipOut);
        }
        return Paths.get(result);
    }

    private void assertCustomConfig(final Nats nats) {
        assertThat(nats.pidFile().toString(), is(endsWith(CUSTOM_PORT + ".pid")));
        assertThat(nats.url(), is(equalTo("nats://" + CUSTOM_ADDR + ":" + CUSTOM_PORT)));
        assertThat(String.valueOf(nats.port()), is(equalTo(CUSTOM_PORT)));
        assertThat(nats.binaryFile().toString(), is(containsString(System.getProperty("java.io.tmpdir"))));
        assertThat(nats.binaryFile().toString(), is(containsString(CUSTOM_LOG_NAME)));
        assertThat(nats.binaryFile().toString(), not(containsString("null")));
        assertThat(nats.downloadUrl(), is(containsString(CUSTOM_VERSION)));
        assertThat(nats.downloadUrl(), is(containsString(nats.config().get(NATS_SYSTEM))));
        assertThat(nats.downloadUrl(), not(containsString("null")));
    }

    //    private String natsSource;
//
//    @BeforeEach
//    void setUp() {
//        natsSource = NatsSourceConfig.URL.getDefaultValue(OS, OS_ARCH, OS_ARCH_TYPE);
//        assertThat(NatsSourceConfig.URL.getDescription(), is(equalTo("[STRING] DEFAULT SOURCE URL")));
//    }
//
//    @Test
//    @DisplayName("No start without annotation")
//    void natsServer_withoutAnnotation_shouldNotBeStarted() {
//        assertThrows(
//                ConnectException.class,
//                () -> new Socket("localhost", 4245).close(),
//                "Connection refused"
//        );
//    }
//
//    @Test
//    @DisplayName("Default config")
//    void natsServer_withoutConfig_shouldStartWithDefaultValues() {
//        Nats nats = new Nats().config(ADDR, "localhost").port(4238).source(natsSource);
//        assertThat(nats.source(), is(equalTo(natsSource)));
//        nats.tryStart(SECONDS.toMillis(10));
//        nats.close();
//        assertThat(nats.toString().length(), is(greaterThan(1)));
//    }
//
//    @Test
//    @DisplayName("Default config JetStream")
//    void natsServer_withJetStream_shouldStartWithDefaultValues() {
//        Nats nats = new Nats().config(JETSTREAM, "true").port(4249).source(natsSource);
//        assertThat(nats.source(), is(equalTo(natsSource)));
//        nats.tryStart(SECONDS.toMillis(10));
//        nats.stop();
//        assertThat(nats.toString().length(), is(greaterThan(1)));
//    }
//
//    @Test
//    @DisplayName("Deactivate JetStream")
//    void natsServer_withJetStreamFalse_shouldNotUseJetStream() {
//        Nats nats = new Nats().config(JETSTREAM, "true");
//        nats.config(JETSTREAM, "false");
//        assertThat(nats.config().get(JETSTREAM), is(nullValue()));
//    }
//
//    @Test
//    @DisplayName("Setup config")
//    void natsServer_configureConfig_shouldNotOverwriteOldConfig() {
//        Nats nats = new Nats(4240).source(natsSource);
//        nats.config("user:adminUser", "PAss:adminPw");
//
//        assertThat(nats.config().get(USER), is(equalTo("adminUser")));
//        assertThat(nats.config().get(PASS), is(equalTo("adminPw")));
//
//        nats.config("user:newUser");
//        assertThat(nats.config().get(USER), is(equalTo("newUser")));
//        assertThat(nats.config().get(PASS), is("adminPw"));
//
//        Map<NatsConfig, String> newConfig = new HashMap<>();
//        newConfig.put(USER, "oldUser");
//        nats.config(newConfig);
//        assertThat(nats.config().get(USER), is(equalTo("oldUser")));
//    }
//
//    @Test
//    @DisplayName("Unknown config is ignored")
//    void natsServer_invalidConfig_shouldNotRunIntroException() {
//        Nats nats = new Nats(4244).source(natsSource);
//        nats.config("user:adminUser:password", " ", "auth:isValid", "");
//        assertThat(nats.config().size(), is(3));
//        assertThat(nats.config().get(AUTH), is(equalTo("isValid")));
//    }
//
//    @Test
//    @DisplayName("Duplicate starts will be ignored")
//    void natsServer_duplicateStart_shouldNotRunIntroExceptionOrInterrupt() throws IOException {
//        Nats nats = new Nats(4231).source(natsSource);
//        nats.start();
//        nats.start(SECONDS.toMillis(10));
//        nats.stop(SECONDS.toMillis(10));
//    }
//
//    @Test
//    @DisplayName("Unknown config [FAIL]")
//    void natsServer_withWrongConfig_shouldNotStartAndThrowException() {
//        assertThrows(
//                IllegalArgumentException.class,
//                () -> new Nats("unknown:config", "port:4232").source(natsSource),
//                "No enum constant"
//        );
//    }
//
//    @Test
//    @DisplayName("Duplicate instances [FAIL]")
//    void natsServer_asTwoInstances_shouldThrowBindException() {
//        Nats nats_Java_one = new Nats(4233).source(natsSource);
//        Nats nats_Java_two = new Nats(4233).source(natsSource);
//        Exception exception = null;
//        try {
//            nats_Java_one.start();
//            nats_Java_two.start();
//        } catch (Exception e) {
//            exception = e;
//        } finally {
//            nats_Java_one.stop();
//            nats_Java_two.stop();
//        }
//        assertThat(requireNonNull(exception).getClass().getSimpleName(), is(equalTo(BindException.class.getSimpleName())));
//    }
//
//    @Test
//    @DisplayName("Stop without start will be ignored")
//    void natsServer_stopWithoutStart_shouldNotRunIntroExceptionOrInterrupt() {
//        Nats nats = new Nats(4241).source(natsSource);
//        nats.stop();
//    }
//
//    @Test
//    @DisplayName("Start multiple times")
//    void natsServer_multipleTimes_shouldBeOkay() throws IOException {
//        int pid1 = new Nats(4234).source(natsSource).start(SECONDS.toMillis(10)).stop(SECONDS.toMillis(10)).pid();
//        int pid2 = new Nats(4234).source(natsSource).start(SECONDS.toMillis(10)).stop(SECONDS.toMillis(10)).pid();
//        int pid3 = new Nats(4234).source(natsSource).start(SECONDS.toMillis(10)).stop(SECONDS.toMillis(10)).pid();
//        assertThat(pid1, is(not(equalTo(pid2))));
//        assertThat(pid2, is(not(equalTo(pid3))));
//        assertThat(pid3, is(not(equalTo(pid1))));
//    }
//
//    @Test
//    @DisplayName("Start in parallel")
//    void natsServer_inParallel_shouldBeOkay() throws IOException {
//        Nats nats1 = new Nats(4235).source(natsSource).start();
//        Nats nats2 = new Nats(4236).source(natsSource).start();
//        assertThat(nats1.pid(), is(not(equalTo(nats2.pid()))));
//        assertThat(nats1.port(), is(not(equalTo(nats2.port()))));
//        assertThat(nats1.pidFile(), is(not(equalTo(nats2.pidFile()))));
//        assertThat(Files.exists(nats1.pidFile()), is(true));
//        assertThat(Files.exists(nats2.pidFile()), is(true));
//        nats1.stop();
//        nats2.stop();
//    }
//
//    @Test
//    @DisplayName("Config port with NULL [FAIL]")
//    void natsServer_withNullablePortValue_shouldThrowMissingFormatArgumentException() {
//        Nats nats = new Nats(4243).source(natsSource);
//        nats.config().put(PORT, null);
//        assertThrows(
//                MissingFormatArgumentException.class,
//                nats::port,
//                "Could not initialise port"
//        );
//    }
//
//    @Test
//    @DisplayName("Configure with NULL value should be ignored")
//    void natsServer_withNullableConfigValue_shouldNotRunIntroExceptionOrInterrupt() throws IOException {
//        Nats nats = new Nats(4236).source(natsSource);
//        nats.config().put(ADDR, null);
//        nats.start();
//        nats.stop();
//    }
//
//    @Test
//    @DisplayName("Configure with invalid config value [FAIL]")
//    void natsServer_withInvalidConfigValue_shouldNotRunIntroExceptionOrInterrupt() {
//        Nats nats = new Nats(ADDR + ":invalidValue", PORT + ":4237").source(natsSource);
//        assertThrows(
//                PortUnreachableException.class,
//                nats::start,
//                "NatsServer failed to start"
//        );
//        nats.stop();
//    }
//
//    @Test
//    @DisplayName("Validate Windows path")
//    void natsServerOnWindows_shouldAddExeToPath() {
//        Nats nats = new Nats(4244).source(natsSource);
//        String windowsNatsServerPath = nats.getNatsServerPath(OS_WINDOWS, ARCH_INTEL, AT_64).toString();
//        String expectedExe = nats.name.toLowerCase() + "_windows_intel64.exe";
//        assertThat(windowsNatsServerPath, containsString(expectedExe));
//    }
//
//    @Test
//    @DisplayName("Config without url [FAIL]")
//    @SuppressWarnings("ResultOfMethodCallIgnored")
//    void natsServerWithoutSourceUrl_shouldThrowException() {
//        Nats nats = new Nats(4239).source(natsSource);
//        nats.getDefaultPath().toFile().delete();
//        nats.source(null);
//        assertThrows(
//                RuntimeException.class,
//                nats::start,
//                "Could not initialise port"
//        );
//        assertThrows(
//                RuntimeException.class,
//                nats::tryStart,
//                "Could not initialise port"
//        );
//    }
//
//    @Test
//    @DisplayName("Configure without value param")
//    void natsServer_withoutValue() throws IOException {
//        Nats nats = new Nats(4242).source(natsSource);
//        nats.config().put(TRACE, "true");
//        nats.start();
//        nats.stop();
//    }
//
//    @Test
//    @DisplayName("Nats server with random port")
//    void natsServer_withRandomPort() {
//        Nats nats = new Nats(-1).source(natsSource);
//        assertThat(nats.port(), is(not((int) PORT.valueRaw())));
//        assertThat(nats.port(), is(greaterThan((int) PORT.valueRaw())));
//        assertThat(nats.port(), is(lessThan((int) PORT.valueRaw() + 501)));
//    }
//
//    @Test
//    @DisplayName("Cov dummy")
//    void covDummy() {
//        assertThat(new NatsFileReaderException("dummy", new RuntimeException()), is(notNullValue()));
//        assertThat(new NatsStartException(new RuntimeException()), is(notNullValue()));
//    }
}

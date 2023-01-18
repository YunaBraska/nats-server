package berlin.yuna.natsserver.logic;

import berlin.yuna.natsserver.config.NatsConfig;
import berlin.yuna.natsserver.config.NatsInterface;
import berlin.yuna.natsserver.config.NatsOptionsBuilder;
import berlin.yuna.natsserver.config.OptionsNats;
import org.junit.jupiter.api.AfterEach;
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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static berlin.yuna.natsserver.config.NatsConfig.ADDR;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_DOWNLOAD_URL;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_LOG_NAME;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_PROPERTY_FILE;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_SYSTEM;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_VERSION;
import static berlin.yuna.natsserver.config.NatsConfig.PORT;
import static berlin.yuna.natsserver.logic.Nats.NATS_PREFIX;
import static berlin.yuna.natsserver.logic.NatsUtils.getSystem;
import static berlin.yuna.natsserver.config.OptionsNats.natsBuilder;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
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
    void setUp() {
        purge();
    }

    @AfterEach
    void tearDown() {
        purge();
    }

    @Test
    @DisplayName("Nats default setup")
    void natsDefault() {
        final Nats nats = new Nats(noAutostart());
        assertThat(nats.pid(), is(-1));
        assertThat(nats.pidFile().toString(), is(endsWith(PORT.defaultValue() + ".pid")));
        assertThat(nats.url(), is(equalTo("nats://" + ADDR.defaultValue() + ":" + PORT.defaultValue())));
        assertThat(nats.port(), is(equalTo(PORT.defaultValue())));
        assertThat(nats.binary().toString(), is(containsString(System.getProperty("java.io.tmpdir"))));
        assertThat(nats.binary().toString(), is(containsString(((String) NATS_LOG_NAME.defaultValue()).toLowerCase())));
    }

    @Test
    @DisplayName("Nats env configs")
    void envConfig() {
        System.setProperty(NATS_VERSION.name(), CUSTOM_VERSION);
        System.setProperty(NATS_LOG_NAME.name(), CUSTOM_LOG_NAME);
        System.setProperty(NATS_PREFIX + NatsConfig.PORT, CUSTOM_PORT);
        System.setProperty(NATS_PREFIX + NatsConfig.ADDR, CUSTOM_ADDR);

        assertCustomConfig(new Nats(noAutostart()));
    }

    @Test
    @DisplayName("Nats dsl configs")
    void dlsConfig() {
        final Nats nats = new Nats(noAutostartBuilder()
                .config(NATS_VERSION, CUSTOM_VERSION)
                .config(NATS_LOG_NAME, CUSTOM_LOG_NAME)
                .config(PORT, CUSTOM_PORT)
                .config(ADDR, CUSTOM_ADDR)
                .build()
        );
        assertCustomConfig(nats);
    }

    @Test
    @DisplayName("Nats dsl configs")
    void dslMultiConfig() {
        final Nats nats = new Nats(noAutostartBuilder().config(
                NATS_VERSION.name(), CUSTOM_VERSION,
                NATS_LOG_NAME.name(), CUSTOM_LOG_NAME,
                PORT.name(), CUSTOM_PORT,
                ADDR.name(), CUSTOM_ADDR
        ).build());
        assertCustomConfig(nats);
    }

    @Test
    @DisplayName("Invalid Property File should be ignored")
    void invalidPropertyFile_shouldBeIgnored() {
        new Nats(noAutostartBuilder().config(NATS_PROPERTY_FILE.name(), this.getClass().getSimpleName()).build());
    }

    @Test
    @DisplayName("Nats property file absolute")
    void propertyFileConfig() {
        System.setProperty(NATS_PROPERTY_FILE.name(), customPropertiesFile);
        final Nats nats = new Nats(noAutostart());
        assertCustomConfig(nats);
        assertThat(nats.configPropertyFile(), is(notNullValue()));
    }

    @Test
    @DisplayName("Nats property file relative")
    void propertyFileConfigRelative() {
        System.setProperty(NATS_PROPERTY_FILE.name(), "custom.properties");
        final Nats nats = new Nats(noAutostart());
        assertCustomConfig(nats);
        assertThat(nats.configPropertyFile(), is(notNullValue()));
    }

    @Test
    @DisplayName("Nats default property file")
    void propertyDefaultFileConfig() throws Exception {
        final Path defaultFile = Paths.get(Paths.get(customPropertiesFile).getParent().toString(), "nats.properties");
        Files.deleteIfExists(defaultFile);

        Files.write(defaultFile, "ADDR=\"default nats file\"".getBytes());
        assertThat(new Nats(noAutostart()).getValue(ADDR), is(equalTo("default nats file")));

        Files.deleteIfExists(defaultFile);
    }

    @Test
    @DisplayName("Nats non existing property file")
    void propertyNonExistingFileConfig() {
        System.setProperty(NATS_PROPERTY_FILE.name(), "invalid");
        assertThat(new Nats(noAutostart()).pidFile().toString(), is(endsWith(PORT.defaultValue() + ".pid")));
    }

    @Test
    @DisplayName("Prepare command")
    void prepareCommand() {
        System.setProperty(NATS_PROPERTY_FILE.name(), customPropertiesFile);
        final String command = new Nats(noAutostart()).prepareCommand();
        assertThat(command, containsString(CUSTOM_ADDR));
        assertThat(command, containsString(CUSTOM_PORT));
        assertThat(command, containsString(CUSTOM_LOG_NAME));
        assertThat(command, containsString(getSystem()));
        assertThat(command, containsString("--customArg1=123 --customArg2=456"));
    }

    @Test
    @DisplayName("download without zip")
    void downloadNatsWithoutZip() throws Exception {
        final Path inputFile = Paths.get(customPropertiesFile);
        final Nats nats = new Nats(noAutostartBuilder().config(NATS_DOWNLOAD_URL, inputFile.toUri().toString()).build());

        nats.downloadNats();
        assertThat(nats.binary().toFile(), is(anExistingFile()));
        assertThat(Files.readAllLines(nats.binary()), is(equalTo(Files.readAllLines(inputFile))));
    }

    @Test
    @DisplayName("download with zip")
    void downloadNatsWithZip() throws Exception {
        final Path inputFile = Paths.get(customPropertiesFile);
        final Path inputZipFile = zipFile(inputFile);
        final Nats nats = new Nats(noAutostartBuilder().config(NATS_DOWNLOAD_URL, inputZipFile.toUri().toString()).build());

        nats.downloadNats();
        assertThat(nats.binary().toFile(), is(anExistingFile()));
        assertThat(Files.readAllLines(nats.binary()), is(equalTo(Files.readAllLines(inputFile))));
    }

    @Test
    @DisplayName("no download if binary exists")
    void noDownloadIfExists() throws Exception {
        final Path inputFile = Paths.get(customPropertiesFile);
        final Nats nats = new Nats(noAutostartBuilder().config(NATS_DOWNLOAD_URL, inputFile.toUri().toString()));

        Files.write(nats.binary(), "Should not be overwritten".getBytes());

        nats.downloadNats();
        assertThat(nats.binary().toFile(), is(anExistingFile()));
        assertThat(Files.readAllLines(nats.binary()), is(equalTo(Collections.singletonList("Should not be overwritten"))));
    }

    @Test
    @DisplayName("findFreePort")
    void findFreePort() {
        try (final Nats nats = new Nats(-1)) {
            assertThat(nats.port(), is(greaterThan((int) PORT.defaultValue())));
        }
    }

    @Test
    @DisplayName("delete pid file")
    void deletePidFile() throws Exception {
        try (final Nats nats = new Nats(noAutostart())) {
            Files.createFile(nats.pidFile());
            assertThat(nats.pidFile().toFile(), is(anExistingFile()));
            nats.deletePidFile();
            assertThat(nats.pidFile().toFile(), is(not(anExistingFile())));
        }
    }

    @Test
    @DisplayName("to String")
    void toStringTest() {
        assertThat(new Nats(noAutostart()).toString(), containsString(String.valueOf(PORT.defaultValue())));
    }

    @Test
    @DisplayName("Constructor with customArgs")
    void constructor_customArgs() {
        final Nats nats = new Nats(noAutostartBuilder().addArgs(new String[]{"--arg1=false", "--arg2=true"}).build());
        assertThat(asList(nats.customArgs()), hasItems("--arg1=false", "--arg2=true"));
        assertThat(nats.prepareCommand(), containsString("--arg1=false --arg2=true"));
    }

    @Test
    @DisplayName("Constructor with customArgs")
    void constructor_port() {
        final Nats nats = new Nats(noAutostartBuilder().port(parseInt(CUSTOM_PORT)).build());
        assertThat(nats.prepareCommand(), containsString(CUSTOM_PORT));
    }

    @Test
    @DisplayName("Constructor with customArgs")
    void config() {
        final Nats nats = new Nats(noAutostart());
        assertThat(nats.configMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().value())), is(equalTo(nats.config())));
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
        assertThat(nats.binary().toString(), is(containsString(System.getProperty("java.io.tmpdir"))));
        assertThat(nats.binary().toString(), is(containsString(CUSTOM_LOG_NAME)));
        assertThat(nats.binary().toString(), not(containsString("null")));
        assertThat(nats.downloadUrl(), is(containsString(CUSTOM_VERSION)));
        assertThat(nats.downloadUrl(), is(containsString(nats.configMap.get(NATS_SYSTEM).value())));
        assertCustomConfigNatsInterface(nats);
        nats.close();
    }

    private void assertCustomConfigNatsInterface(final NatsInterface nats) {
        assertThat(nats.url(), is(equalTo("nats://example.com:123456")));
        assertThat(nats.jetStream(), is(equalTo(false)));
        assertThat(nats.debug(), is(equalTo(false)));
        assertThat(nats.binary().toString(), is(containsString(System.getProperty("java.io.tmpdir"))));
        assertThat(nats.binary().toString(), is(containsString(CUSTOM_LOG_NAME)));
        assertThat(nats.binary().toString(), not(containsString("null")));
        assertThat(nats.customArgs().length, is(notNullValue()));
        assertThat(nats.logger(), is(notNullValue()));
        assertThat(nats.loggingLevel(), is(nullValue()));
        assertThat(nats.port(), is(equalTo(parseInt(CUSTOM_PORT))));
        assertThat(nats.process(), is(nullValue()));
        assertThat(nats.configFile(), is(nullValue()));
    }

    private void purge() {
        try {
            Files.deleteIfExists(new Nats(noAutostart()).binary());
            Arrays.stream(NatsConfig.values()).forEach(config -> {
                System.clearProperty(config.name());
                System.clearProperty(NATS_PREFIX + config.name());
            });
            customPropertiesFile = Objects.requireNonNull(getClass().getClassLoader().getResource("custom.properties")).getPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private OptionsNats noAutostart() {
        return natsBuilder().autostart(false).build();
    }

    private NatsOptionsBuilder noAutostartBuilder() {
        return natsBuilder().autostart(false);
    }
}

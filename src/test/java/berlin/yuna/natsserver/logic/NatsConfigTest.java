package berlin.yuna.natsserver.logic;

import berlin.yuna.natsserver.config.NatsConfig;
import berlin.yuna.natsserver.config.NatsOptions;
import berlin.yuna.natsserver.config.NatsOptionsBuilder;
import io.nats.commons.NatsInterface;
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

import static berlin.yuna.natsserver.config.NatsConfig.*;
import static berlin.yuna.natsserver.config.NatsOptions.natsBuilder;
import static berlin.yuna.natsserver.logic.Nats.NATS_PREFIX;
import static berlin.yuna.natsserver.logic.NatsUtils.getSystem;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;

@Tag("UnitTest")
@DisplayName("Nats config test")
@SuppressWarnings("resource")
class NatsConfigTest {

    private static final String CUSTOM_LOG_NAME = "my_nats_name";
    private static final String CUSTOM_PORT = "123456";
    private static final String CUSTOM_NET = "example.com";
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
        assertThat(nats.pid()).isEqualTo(-1);
        assertThat(nats.pidFile().toString()).endsWith(PORT.defaultValue() + ".pid");
        assertThat(nats.url()).isEqualTo("nats://" + NET.defaultValue() + ":" + PORT.defaultValue());
        assertThat(nats.port()).isEqualTo(PORT.defaultValue());
        assertThat(nats.binary().toString()).contains(System.getProperty("java.io.tmpdir"));
        assertThat(nats.binary().toString()).contains(((String) NATS_LOG_NAME.defaultValue()).toLowerCase());
    }

    @Test
    @DisplayName("Nats env configs")
    void envConfig() {
        System.setProperty(NATS_VERSION.name(), CUSTOM_VERSION);
        System.setProperty(NATS_LOG_NAME.name(), CUSTOM_LOG_NAME);
        System.setProperty(NATS_PREFIX + NatsConfig.PORT, CUSTOM_PORT);
        System.setProperty(NATS_PREFIX + NET, CUSTOM_NET);
        assertCustomConfig(new Nats(noAutostart()));
    }

    @Test
    @DisplayName("Nats dsl configs")
    void dlsConfig() {
        final Nats nats = new Nats(noAutostartBuilder()
                .config(NATS_VERSION, CUSTOM_VERSION)
                .config(NATS_LOG_NAME, CUSTOM_LOG_NAME)
                .config(PORT, CUSTOM_PORT)
                .config(NET, CUSTOM_NET)
                .build());
        assertCustomConfig(nats);
    }

    @Test
    @DisplayName("Nats dsl configs")
    void dslMultiConfig() {
        final Nats nats = new Nats(noAutostartBuilder().config(
                NATS_VERSION.name(), CUSTOM_VERSION,
                NATS_LOG_NAME.name(), CUSTOM_LOG_NAME,
                PORT.name(), CUSTOM_PORT,
                NET.name(), CUSTOM_NET
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
        assertThat(nats.configPropertyFile()).isNotNull();
    }

    @Test
    @DisplayName("Nats property file relative")
    void propertyFileConfigRelative() {
        System.setProperty(NATS_PROPERTY_FILE.name(), "custom.properties");
        final Nats nats = new Nats(noAutostart());
        assertCustomConfig(nats);
        assertThat(nats.configPropertyFile()).isNotNull();
    }

    @Test
    @DisplayName("Nats default property file")
    void propertyDefaultFileConfig() throws Exception {
        final Path defaultFile = Paths.get(Paths.get(customPropertiesFile).getParent().toString(), "nats.properties");
        Files.deleteIfExists(defaultFile);

        Files.write(defaultFile, "NET=\"default nats file\"".getBytes());
        assertThat(new Nats(noAutostart()).getValue(NET)).isEqualTo("default nats file");

        Files.deleteIfExists(defaultFile);
    }

    @Test
    @DisplayName("Nats non existing property file")
    void propertyNonExistingFileConfig() {
        System.setProperty(NATS_PROPERTY_FILE.name(), "invalid");
        assertThat(new Nats(noAutostart()).pidFile().toString()).endsWith(PORT.defaultValue() + ".pid");
    }

    @Test
    @DisplayName("Prepare command")
    void prepareCommand() {
        System.setProperty(NATS_PROPERTY_FILE.name(), customPropertiesFile);
        final String command = new Nats(noAutostart()).prepareCommand();
        assertThat(command).contains(CUSTOM_NET, CUSTOM_PORT, CUSTOM_LOG_NAME, getSystem(), "--customArg1=123 --customArg2=456");
    }

    @Test
    @DisplayName("download without zip")
    void downloadNatsWithoutZip() throws Exception {
        final Path inputFile = Paths.get(customPropertiesFile);
        final Nats nats = new Nats(noAutostartBuilder().config(NATS_DOWNLOAD_URL, inputFile.toUri().toString()).build());

        nats.downloadNats();
        assertThat(nats.binary().toFile()).exists();
        assertThat(Files.readAllLines(nats.binary())).isEqualTo(Files.readAllLines(inputFile));
    }

    @Test
    @DisplayName("download with zip")
    void downloadNatsWithZip() throws Exception {
        final Path inputFile = Paths.get(customPropertiesFile);
        final Path inputZipFile = zipFile(inputFile);
        final Nats nats = new Nats(noAutostartBuilder().config(NATS_DOWNLOAD_URL, inputZipFile.toUri().toString()).build());

        nats.downloadNats();
        assertThat(nats.binary().toFile()).exists();
        assertThat(Files.readAllLines(nats.binary())).isEqualTo(Files.readAllLines(inputFile));
    }

    @Test
    @DisplayName("no download if binary exists")
    void noDownloadIfExists() throws Exception {
        final Path inputFile = Paths.get(customPropertiesFile);
        final Nats nats = new Nats(noAutostartBuilder().config(NATS_DOWNLOAD_URL, inputFile.toUri().toString()));

        Files.write(nats.binary(), "Should not be overwritten".getBytes());

        nats.downloadNats();
        assertThat(nats.binary().toFile()).exists();
        assertThat(Files.readAllLines(nats.binary())).containsExactly("Should not be overwritten");
    }

    @Test
    @DisplayName("findFreePort")
    void findFreePort() {
        try (final Nats nats = new Nats(-1)) {
            assertThat(nats.port()).isGreaterThan((int) PORT.defaultValue());
        }
    }

    @Test
    @DisplayName("delete pid file")
    void deletePidFile() throws Exception {
        try (final Nats nats = new Nats(noAutostart())) {
            Files.createFile(nats.pidFile());
            assertThat(nats.pidFile().toFile()).exists();
            nats.deletePidFile();
            assertThat(nats.pidFile().toFile()).doesNotExist();
        }
    }

    @Test
    @DisplayName("to String")
    void toStringTest() {
        assertThat(new Nats(noAutostart()).toString()).contains(String.valueOf(PORT.defaultValue()));
    }

    @Test
    @DisplayName("Constructor with customArgs")
    void constructor_customArgs() {
        final Nats nats = new Nats(noAutostartBuilder().addArgs("--arg1=false", "--arg2=true").build());
        assertThat(asList(nats.customArgs())).contains("--arg1=false", "--arg2=true");
        assertThat(nats.prepareCommand()).contains("--arg1=false --arg2=true");
    }

    @Test
    @DisplayName("Constructor with customArgs")
    void constructor_port() {
        final Nats nats = new Nats(noAutostartBuilder().port(parseInt(CUSTOM_PORT)).build());
        assertThat(nats.prepareCommand()).contains(CUSTOM_PORT);
    }

    @Test
    @DisplayName("Constructor with customArgs")
    void config() {
        final Nats nats = new Nats(noAutostart());
        assertThat(nats.configMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().value())))
                .isEqualTo(nats.config());
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
        assertThat(nats.pidFile().toString()).endsWith(CUSTOM_PORT + ".pid");
        assertThat(nats.url()).isEqualTo("nats://" + CUSTOM_NET + ":" + CUSTOM_PORT);
        assertThat(String.valueOf(nats.port())).isEqualTo(CUSTOM_PORT);
        assertThat(nats.binary().toString()).contains(System.getProperty("java.io.tmpdir"), CUSTOM_LOG_NAME);
        assertThat(nats.binary().toString()).doesNotContain("null");
        assertThat(nats.downloadUrl()).contains(CUSTOM_VERSION, nats.configMap.get(NATS_SYSTEM).value().toString());
        assertCustomConfigNatsInterface(nats);
        nats.close();
    }

    private void assertCustomConfigNatsInterface(final NatsInterface nats) {
        assertThat(nats.url()).isEqualTo("nats://example.com:123456");
        assertThat(nats.jetStream()).isFalse();
        assertThat(nats.debug()).isFalse();
        assertThat(nats.binary().toString()).contains(System.getProperty("java.io.tmpdir"), CUSTOM_LOG_NAME);
        assertThat(nats.binary().toString()).doesNotContain("null");
        assertThat(nats.customArgs()).isNotNull();
        assertThat(nats.logger()).isNotNull();
        assertThat(nats.loggingLevel()).isNull();
        assertThat(nats.port()).isEqualTo(parseInt(CUSTOM_PORT));
        assertThat(nats.process()).isNull();
        assertThat(nats.configFile()).isNull();
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

    private NatsOptions noAutostart() {
        return natsBuilder().autostart(false).build();
    }

    private NatsOptionsBuilder noAutostartBuilder() {
        return natsBuilder().autostart(false);
    }
}
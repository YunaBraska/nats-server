package berlin.yuna.natsserver.logic;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.natsserver.config.NatsConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static berlin.yuna.clu.logic.SystemUtil.readFile;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_VERSION;
import static berlin.yuna.natsserver.config.NatsOptions.natsBuilder;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@Tag("IntegrationTest")
@DisplayName("NatsServer ConfigTest")
@SuppressWarnings("resource")
class NatsConfigComponentTest {

    public static final Pattern RELEASE_PATTERN = Pattern.compile("\"tag_name\":\"(?<version>.*?)\"");
    public static final int HELP_LINE_DESCRIPTION_INDEX = 37;

    @Test
    @DisplayName("Compare nats with java config")
    void compareNatsConfig() throws Exception {
        final String newNatsVersion = updateNatsVersion();
        Files.deleteIfExists(new Nats(natsBuilder().autostart(false).build()).binary());
        final Nats nats = new Nats(natsBuilder().autostart(false).port(-1).config(NATS_VERSION, newNatsVersion));
        nats.downloadNats();
        final Path natsServerPath = nats.binary();


        final var console = new ArrayList<String>();
        new Terminal().consumerInfoStream(console::add).consumerErrorStream(console::add).timeoutMs(10000).execute(natsServerPath.toString() + " --help");
        final List<String> missingConfigs = console.stream().filter(line -> line != null
                && line.length() >= HELP_LINE_DESCRIPTION_INDEX
                && line.contains("-")
                && !line.startsWith("                                     ")
                && stream(NatsConfig.values()).noneMatch(config -> config.description().contains(line.substring(HELP_LINE_DESCRIPTION_INDEX).trim()))
        ).collect(toList());

        final var removedConfigs = stream(NatsConfig.values()).filter(config -> config.key() != null
                        && console.stream().filter(Objects::nonNull).filter(line -> line.contains("-")).noneMatch(line ->
                        line.matches(".*" + config.key() + "(?:$|\\n|,|\\s|\\n|\\r).*") && (line.length() < HELP_LINE_DESCRIPTION_INDEX || line.contains(config.description().split("\\r?\\n")[0]))
                )
        ).map(config -> String.format("name[%s] key [%s] desc [%s]", config, config.key(), config.description())).collect(toList());
        assertThat("Missing config in java \n [" + nats.binary() + "] \n", missingConfigs, is(empty()));
        assertThat("Config was removed by nats \n [" + nats.binary() + "] \n", removedConfigs, is(empty()));
    }

    @Test
    @DisplayName("Compare config key with one dash")
    void getKey_WithOneDash_ShouldBeSuccessful() {
        assertThat(NatsConfig.ADDR.key(), is(equalTo("--addr")));
    }

    @Test
    @DisplayName("Compare config key with equal sign")
    void getKey_WithBoolean_ShouldAddOneEqualSign() {
        assertThat(NatsConfig.NO_ADVERTISE.key(), is(equalTo("--no_advertise")));
    }

    private String updateNatsVersion() throws IOException {
        try (final Stream<Path> stream = Files.walk(FileSystems.getDefault().getPath(System.getProperty("user.dir")), 99)) {
            final Path configJavaFile = stream.filter(path -> path.getFileName().toString().equalsIgnoreCase(NatsConfig.class.getSimpleName() + ".java")).findFirst().orElse(null);
            final URL url = new URL("https://api.github.com/repos/nats-io/nats-server/releases/latest");
            final HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            final String previousVersion = NATS_VERSION.defaultValueStr();
            final String newVersion = updateNatsVersion(configJavaFile, read(con.getInputStream()));
            if (!requireNonNull(previousVersion).equals(newVersion)) {
                Files.write(Paths.get(System.getProperty("user.dir"), "version.txt"), (newVersion.startsWith("v") ? newVersion.substring(1) : newVersion).getBytes());
            }
            return newVersion;
        }
    }

    private static String updateNatsVersion(final Path configJavaFile, final String release_json) throws IOException {
        final Matcher matcher = RELEASE_PATTERN.matcher(release_json);
        if (matcher.find()) {
            final String version = matcher.group("version");
            System.out.println("LATEST NATS VERSION [" + version + "]");
            String content = readFile(requireNonNull(configJavaFile));
            content = content.replaceFirst("(?<prefix>.*" + NATS_VERSION.name() + "\\(.*?\")(.*?)(?<suffix>\".*)", "${prefix}" + version + "${suffix}");
            Files.write(configJavaFile, content.getBytes());
            return version;
        } else {
            throw new IllegalStateException("Could not update nats server version");
        }
    }

    public static String read(final InputStream input) throws IOException {
        try (final BufferedReader buffer = new BufferedReader(new InputStreamReader(input, UTF_8))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }
}

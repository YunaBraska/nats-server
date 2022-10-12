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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static berlin.yuna.clu.logic.SystemUtil.readFile;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@Tag("IntegrationTest")
@DisplayName("NatsServer ConfigTest")
class NatsConfigComponentTest {

    public static final Pattern RELEASE_PATTERN = Pattern.compile("\"tag_name\":\"(?<version>.*?)\"");

    @Test
    @DisplayName("Compare nats with java config")
    void compareNatsConfig() throws IOException {
        updateNatsVersion();
        Files.deleteIfExists(new Nats().binaryFile());
        final Nats nats = new Nats(-1);
        nats.downloadNats();
        final Path natsServerPath = nats.binaryFile();

        final StringBuilder console = new StringBuilder();

        final Terminal terminal = new Terminal().timeoutMs(10000).execute(natsServerPath.toString() + " --help");
        console.append(terminal.consoleInfo()).append(terminal.consoleError());

        final List<String> consoleConfigKeys = readConfigKeys(console.toString());
        final List<String> javaConfigKeys = stream(NatsConfig.values()).map(Enum::name).filter(name -> !name.startsWith("NATS_")).collect(Collectors.toList());

        final Set<String> missingConfigInJava = getNotMatchingEntities(consoleConfigKeys, javaConfigKeys);

        final Set<String> missingConfigInConsole = getNotMatchingEntities(javaConfigKeys, consoleConfigKeys);
        assertThat("Missing config in java \n [" + nats.binaryFile() + "] \n" + console, missingConfigInJava, is(empty()));
        assertThat("Config was removed by nats \n [" + nats.binaryFile() + "] \n", missingConfigInConsole, is(empty()));
    }

    @Test
    @DisplayName("Compare config key with one dash")
    void getKey_WithOneDash_ShouldBeSuccessful() {
        assertThat(NatsConfig.ADDR.key(), is(equalTo("--addr ")));
    }

    @Test
    @DisplayName("Compare config key with equal sign")
    void getKey_WithBoolean_ShouldAddOneEqualSign() {
        assertThat(NatsConfig.NO_ADVERTISE.key(), is(equalTo("--no_advertise=")));
    }

    private void updateNatsVersion() throws IOException {
        try (final Stream<Path> stream = Files.walk(FileSystems.getDefault().getPath(System.getProperty("user.dir")), 99)) {
            final Path configJavaFile = stream.filter(path -> path.getFileName().toString().equalsIgnoreCase(NatsConfig.class.getSimpleName() + ".java")).findFirst().orElse(null);
            final URL url = new URL("https://api.github.com/repos/nats-io/nats-server/releases/latest");
            final HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            final String previousVersion = NATS_VERSION.value();
            final String newVersion = updateNatsVersion(configJavaFile, read(con.getInputStream()));
            if (!requireNonNull(previousVersion).equals(newVersion)) {
                Files.write(Paths.get(System.getProperty("user.dir"), "version.txt"), (newVersion.startsWith("v") ? newVersion.substring(1) : newVersion).getBytes());
            }
        }
    }

    private static String updateNatsVersion(final Path configJavaFile, final String release_json) throws IOException {
        final Matcher matcher = RELEASE_PATTERN.matcher(release_json);
        if (matcher.find()) {
            final String version = matcher.group("version");
            System.out.println("LATEST NATS VERSION [" + version + "]");
            String content = readFile(requireNonNull(configJavaFile));
            content = content.replaceFirst("(?<prefix>" + NATS_VERSION.name() + "\\(\")(.*)(?<suffix>\",\\s\")", "${prefix}" + version + "${suffix}");
            Files.write(configJavaFile, content.getBytes());
            return version;
        } else {
            throw new IllegalStateException("Could not update nats server version");
        }
    }

    private Set<String> getNotMatchingEntities(final List<String> list1, final List<String> list2) {
        final Set<String> noMatches = new HashSet<>();
        for (String entity : list1) {
            if (!entity.equalsIgnoreCase("help")
                    && !entity.equalsIgnoreCase("version")
                    && !entity.equalsIgnoreCase("help_tls")
                    && !list2.contains(entity)) {
                noMatches.add(entity);
            }
        }
        return noMatches;
    }

    private List<String> readConfigKeys(final String console) {
        final List<String> allMatches = new ArrayList<>();
        final Matcher m = Pattern.compile("(--(?<dd>[a-z_]+))|(-(?<d>[a-z_]+)\\s*[<=])").matcher(console);
        while (m.find()) {
            final String group = m.group("dd");
            allMatches.add((group == null ? m.group("d") : group).toUpperCase());
        }
        return allMatches;
    }

    public static String read(final InputStream input) throws IOException {
        try (final BufferedReader buffer = new BufferedReader(new InputStreamReader(input, UTF_8))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }
}

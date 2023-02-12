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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static berlin.yuna.clu.logic.SystemUtil.readFile;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_VERSION;
import static berlin.yuna.natsserver.config.NatsOptions.natsBuilder;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
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
        final var helpMenuFull = console.stream().map(String::trim).filter(s -> !s.isBlank() && !s.isEmpty()).collect(toList());
        final List<String[]> helpMenuItems = getHelpMenuItems(console);
        final var missingKeyOrDesc = helpMenuItems.stream().map(NatsConfigComponentTest::explainNatsConfig).filter(Objects::nonNull).collect(toList());
        final var removedConfigs = stream(NatsConfig.values()).filter(config -> config.key() != null).filter(config -> helpMenuItems.stream().noneMatch(item -> config.key().equals(item[0]))).collect(toList());
        assertThat(
                "Missing key or description of nats \n [" + nats.binary() + "] \n"
                        + "missingConfigs [" + missingKeyOrDesc.size() + "/" + helpMenuItems.size() + "] [\n\n" + String.join("\n", missingKeyOrDesc) + "\n\n]\n"
                        + "helpMenuList [\n\n" + String.join("\n", helpMenuFull) + "\n\n]\n",
                missingKeyOrDesc.size(),
                is(0)
        );
        assertThat("Config was removed by nats \n [" + nats.binary() + "] \n", removedConfigs, is(empty()));
    }

    @Test
    @DisplayName("Compare config key with one dash")
    void getKey_WithOneDash_ShouldBeSuccessful() {
        assertThat(NatsConfig.NET.key(), is(equalTo("--net")));
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
            con.setRequestProperty("Accept", "application/vnd.github+json");
            con.setRequestProperty("User-Agent", "MyGitHubClient");
            ofNullable(System.getProperty("GITHUB_TOKEN", System.getenv("GITHUB_TOKEN")))
                    .or(() -> ofNullable(System.getProperty("CI_TOKEN", System.getenv("CI_TOKEN"))))
                    .or(() -> ofNullable(System.getProperty("CI_TOKEN_WORKFLOW", System.getenv("CI_TOKEN_WORKFLOW"))))
                    .ifPresent(token -> {
                        System.out.println("Connect to github API with token authorization");
                        con.setRequestProperty("Authorization", "Token " + token);
                    });

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

    private static List<String[]> getHelpMenuItems(final Collection<String> lines) {
        return lines
                .stream()
                .map(String::trim)
                .filter(line -> line.startsWith("-"))
                .filter(line -> line.indexOf(" ", indexOfFirstKey(line)) != -1)
                .map(line -> {
                            final String[] result = new String[3];
                            final int fromIndex = indexOfFirstKey(line);
                            final var typeIndex = Stream.of(line.indexOf(" ", fromIndex), line.indexOf("=", fromIndex), line.indexOf("<", fromIndex)).filter(i -> i != -1).min(Integer::compareTo).orElse(fromIndex);
                            result[0] = parseKey(line, fromIndex, typeIndex).replace(",", "").trim();
                            result[1] = parseType(line, typeIndex).trim();
                            result[2] = line.substring(result[1].isEmpty() ? typeIndex : line.indexOf(">", fromIndex) + 1).replace("[=<pid>]", "").trim();
                            return result;
                        }

                )
                .collect(Collectors.toList());
    }

    private static String parseKey(final String line, final int index, final Integer typeIndex) {
        final var keys = line.substring(index, typeIndex).split(" ");
        return keys[keys.length - 1];
    }

    private static String parseType(final String line, final int index) {
        var result = line.substring(index).trim();
        if (!Character.isLetterOrDigit(result.charAt(0))) {
            result = result.startsWith("=") ? result.substring(1) : result;
            result = result.startsWith("<") && result.contains(">") ? result.substring(1, result.indexOf(">")) : result;
            result = result.startsWith("[") && result.contains("]") ? result.substring(1, result.indexOf("]")) : result;
            return result;
        }
        return "";
    }

    private static Class<?> parseType(final String type) {
        switch (type) {
            case "host":
            case "addr":
            case "user":
            case "rurl":
            case "token":
            case "string":
            case "signal":
            case "duration":
            case "password":
            case "server_name":
                return String.class;
            case "bool":
                return Boolean.class;
            case "dir":
            case "file":
                return Path.class;
            case "url":
            case "rurl-1":
            case "cluster-url":
                return URL.class;
            case "int":
            case "len":
            case "size":
            case "port":
            case "limit":
            case "number":
                return Integer.class;
            case "":
                return null;
            default:
                throw new IllegalArgumentException("Unknown type [" + type + "]");
        }
    }

    private static Integer indexOfFirstKey(final String line) {
        return Optional.of(line.lastIndexOf("--")).filter(i -> i != -1)
                .or(() -> Optional.of(line.indexOf("-")).filter(i -> i != -1))
                .orElse(0);
    }

    private static String explainNatsConfig(final String[] kv) {
        final var hasKey = stream(NatsConfig.values()).anyMatch(config -> kv[0].equals(config.key()));
        final var hasDec = stream(NatsConfig.values()).anyMatch(config -> kv[2].equals(config.description().split("\n")[0].trim()));
        final var typeMissmatch = stream(NatsConfig.values()).filter(config -> kv[0].equals(config.key())).findFirst().map(config -> {
            final var clazz = parseType(kv[1].contains(",") ? "string" : kv[1]);
            if (clazz != null && (!clazz.equals(config.type()) && (!config.type().equals(NatsConfig.SilentBoolean.class) && !clazz.equals(Boolean.class)))) {
                return format("%s != %s (%s)", ofNullable(config.type()).map(Class::getSimpleName).orElse(null), clazz.getSimpleName(), kv[1]);
            }
            return null;
        }).orElse(null);
        final boolean noneMatch = stream(NatsConfig.values()).noneMatch(config -> kv[0].equals(config.key()) && kv[2].equals(config.description().split("\n")[0].trim()));
        if (noneMatch || typeMissmatch != null) {
            return "missing ["
                    + Stream.of((!hasKey ? "key" : ""),
                    (!hasDec ? "desc" : ""),
                    (typeMissmatch != null ? typeMissmatch : "")).filter(s -> !s.isEmpty()).collect(joining(", "))
                    + "] item ["
                    + String.join(", ", kv)
                    + "]";
        }
        return null;
    }
}

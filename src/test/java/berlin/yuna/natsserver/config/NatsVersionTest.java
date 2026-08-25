package berlin.yuna.natsserver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("UnitTest")
@DisplayName("Nats Version Generator")
class NatsVersionTest {

    private static final String IDENTIFIER = "(?:0|[1-9][0-9]*|[0-9A-Za-z-]*[A-Za-z-][0-9A-Za-z-]*)";
    private static final Pattern VERSION = Pattern.compile("^v?(?<release>(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*))(?<preRelease>-(?:" + IDENTIFIER + ")(?:\\.(?:" + IDENTIFIER + "))*)?$");

    @Test
    void generateVersionsTest() throws IOException {
        writeVersions(readTags(callGET("https://api.github.com/repos/nats-io/nats-server/git/refs/tags/")));
    }

    @Test
    void keepsUnreleasedPreReleasesAndRemovesReleasedPreReleases() {
        final String versions = toEnum(List.of(
                "v2.15.0-preview.1",
                "v2.14.5-rc.1",
                "2.14.5",
                "v2.14.4+build.1",
                "v2.14.04",
                "v2.14.5-01"
        ));

        assertThat(versions)
                .contains("V2_15_0_PREVIEW_1(\"v2.15.0-preview.1\")", "V2_14_5(\"2.14.5\")")
                .doesNotContain("V2_14_5_RC_1", "V2_14_4_BUILD_1", "V2_14_04", "V2_14_5_01");
    }

    private static void writeVersions(final List<String> tags) throws IOException {
        final String className = NatsVersion.class.getSimpleName();
        Files.writeString(
                Path.of(System.getProperty("user.dir"), "src/main/java/berlin/yuna/natsserver/config/" + className + ".java"),
                "package berlin.yuna.natsserver.config;\n\n"
                        + "public enum " + className + " {\n\n"
                        + toEnum(tags)
                        + "\n\n    final String value;\n\n" +
                        "    " + className + "(final String value) {\n" +
                        "        this.value = value;\n" +
                        "    }\n\n" +
                        "    public String value() {\n" +
                        "        return value;\n" +
                        "    }"
                        + "\n}\n"
        );
    }

    private static List<String> readTags(final String json) {
        final List<String> tags = new ArrayList<>();

        int startIndex;
        int endIndex = 0;
        while (endIndex != -1) {
            final String tagPattern = "\"refs/tags/";
            startIndex = json.indexOf(tagPattern, endIndex);
            if (startIndex == -1) {
                break;
            }
            startIndex += tagPattern.length();
            endIndex = json.indexOf("\"", startIndex);
            tags.add(json.substring(startIndex, endIndex));
        }
        return tags;
    }

    private static String toEnum(final List<String> tags) {
        final List<String> stableVersions = tags.stream().filter(NatsVersionTest::isStable).map(NatsVersionTest::releaseLine).collect(Collectors.toList());
        return tags.stream()
                .filter(NatsVersionTest::isVersion)
                .filter(tag -> isStable(tag) || !stableVersions.contains(releaseLine(tag)))
                .sorted(Collections.reverseOrder())
                .limit(100)
                .map(NatsVersionTest::toEnum)
                .collect(Collectors.joining(",\n", "", ";"));
    }

    private static String toEnum(final String tag) {
        return "    V" + tag.replaceFirst("^v", "").toUpperCase().replaceAll("[^\\w\\d]", "_") + "(\"" + tag + "\")";
    }

    private static boolean isVersion(final String tag) {
        return VERSION.matcher(tag).matches();
    }

    private static boolean isStable(final String tag) {
        final Matcher matcher = VERSION.matcher(tag);
        return matcher.matches() && matcher.group("preRelease") == null;
    }

    private static String releaseLine(final String tag) {
        final Matcher matcher = VERSION.matcher(tag);
        return matcher.matches() ? matcher.group("release") : "";
    }

    private static String callGET(final String urlString) throws IOException {
        final URL url = new URL(urlString);
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/vnd.github+json");
        con.setRequestProperty("User-Agent", "YunaBraskaRestClient");
        ofNullable(System.getProperty("GITHUB_TOKEN", System.getenv("GITHUB_TOKEN")))
                .or(() -> ofNullable(System.getProperty("CI_TOKEN", System.getenv("CI_TOKEN"))))
                .or(() -> ofNullable(System.getProperty("CI_TOKEN_WORKFLOW", System.getenv("CI_TOKEN_WORKFLOW"))))
                .ifPresent(token -> {
                    System.out.println("Call method [GET] url [" + urlString + "] authorisation [" + true + "]");
                    con.setRequestProperty("Authorization", "Bearer " + token);
                });


        final int status = con.getResponseCode();
        if (status == 200) {
            final BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            final StringBuilder jsonString = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                jsonString.append(inputLine);
            }
            in.close();
            return jsonString.toString();
        } else {
            throw new IllegalStateException("Failed to call method [GET] url [" + urlString + "] status code [" + status + "]");
        }
    }
}

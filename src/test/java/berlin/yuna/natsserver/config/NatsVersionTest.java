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
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Tag("UnitTest")
@DisplayName("Nats Version Generator")
class NatsVersionTest {

    @Test
    void generateVersionsTest() throws IOException {
        writeVersions(readTags(callGET("https://api.github.com/repos/nats-io/nats-server/git/refs/tags/")));
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
        return tags.stream().sorted(Collections.reverseOrder()).limit(100).map(NatsVersionTest::toEnum).collect(Collectors.joining(",\n", "", ";"));
    }

    private static String toEnum(final String tag) {
        return "    " + tag.toUpperCase().replaceAll("[^\\w\\d]", "_") + "(\"" + tag + "\")";
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

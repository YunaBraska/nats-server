package berlin.yuna.natsserver.logic;

import berlin.yuna.natsserver.config.NatsConfig;
import berlin.yuna.natsserver.model.MapValue;
import berlin.yuna.natsserver.model.NatsDownloadException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import static berlin.yuna.clu.logic.SystemUtil.OS;
import static berlin.yuna.clu.logic.SystemUtil.OS_ARCH;
import static berlin.yuna.clu.logic.SystemUtil.OS_ARCH_TYPE;
import static java.nio.channels.Channels.newChannel;
import static java.util.Comparator.comparingLong;

public class NatsUtils {

    private NatsUtils() {
    }

    public static String getEnv(final String key) {
        return getEnv(key, () -> null);
    }

    public static String getEnv(final String key, final Supplier<String> fallback) {
        return Optional.ofNullable(System.getProperty(key.toLowerCase()))
                .orElseGet(() -> Optional.ofNullable(System.getProperty(key.toUpperCase())).orElseGet(fallback));
    }

    public static String resolveEnvs(final String input, final Map<NatsConfig, MapValue> config) {
        String result = input;
        int start;
        int end;
        while (result != null && (start = result.indexOf("%")) != -1 && (end = result.indexOf("%", start + 1)) != -1) {
            final String key = result.substring(start + 1, end);
            result = result.substring(0, start)
                    + envValue(key, config)
                    + result.substring(end + 1);
        }
        return result;
    }

    public static String getSystem() {
        return (osString(OS, null) + osString(OS_ARCH, "-") + osString(OS_ARCH_TYPE, null))
                .replace("mips64", "linux-mips64le")
                .replace("darwin-386", "darwin-amd64")
                ;
    }

    public static Path download(final URL source, final Path target) {
        try (final FileOutputStream fos = new FileOutputStream(target.toFile())) {
            fos.getChannel().transferFrom(newChannel(source.openStream()), 0, Long.MAX_VALUE);
            return target;
        } catch (Exception e) {
            throw new NatsDownloadException(e);
        }
    }

    public static Path unzip(final Path source, final Path target) throws IOException {
        try (final ZipFile zipFile = new ZipFile(source.toFile())) {
            final ZipEntry max = zipFile.stream().max(comparingLong(ZipEntry::getSize)).orElseThrow(() -> new IllegalStateException("File not found " + zipFile));
            Files.copy(zipFile.getInputStream(max), target);
        } catch (ZipException ze) {
            Files.copy(new FileInputStream(source.toFile()), target);
        }
        Files.deleteIfExists(source);
        return target;
    }

    public static void validatePort(final int port, final long timeoutMs, final boolean untilFree, final Supplier<Exception> onFail) throws Exception {
        if (!waitForPort(port, timeoutMs, untilFree)) {
            throw onFail.get();
        }
    }

    public static boolean waitForPort(final int port, final long timeoutMs, final boolean isFree) {
        final long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < timeoutMs) {
            if (isPortAvailable(port) == isFree) {
                return true;
            }
            Thread.yield();
        }
        return timeoutMs <= 0;
    }

    public static boolean isPortAvailable(final int port) {
        try {
            new Socket("localhost", port).close();
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    public static int getNextFreePort(final int startPort) {
        for (int i = 1; i < 1024; i++) {
            final int port = i + startPort;
            if (!isPortInUse(port)) {
                return port;
            }
        }
        throw new IllegalStateException("Could not find any free port");
    }

    public static boolean isPortInUse(final int portNumber) {
        try {
            new Socket("localhost", portNumber).close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isEmpty(final String string) {
        return string == null || string.trim().length() <= 0;
    }

    private static String envValue(final String key, final Map<NatsConfig, MapValue> config) {
        return Optional
                .ofNullable(config.get(NatsConfig.valueOf(key)))
                .map(MapValue::value)
                .orElseGet(() -> getEnv(key, () -> ""));
    }

    public static String removeQuotes(final String string) {
        if ((string.startsWith("\"") && string.endsWith("\"")) ||
                (string.startsWith("'") && string.endsWith("'"))) {
            return string.substring(1, string.length() - 1);
        }
        return string;
    }


    private static String osString(final Enum<?> input, final String prefix) {
        if (input != null && !input.name().contains("UNKNOWN")) {
            return (prefix == null ? "" : "-") + input.toString().toLowerCase()
                    .replace("86", "386")
                    .replace("intel", "")
                    .replace("_", "");
        }
        return "";
    }
}

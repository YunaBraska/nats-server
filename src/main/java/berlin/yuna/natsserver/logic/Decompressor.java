package berlin.yuna.natsserver.logic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static berlin.yuna.natsserver.logic.NatsUtils.deleteDirectory;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public final class Decompressor {

    public static Path extractAndReturnBiggest(final Path archivePath, final Path target) throws IOException {
        final String name = archivePath.getFileName().toString().toLowerCase();
        final Path tempDir = Files.createTempDirectory("unzipped_");
        Path tarFile = null;

        try {
            if (name.endsWith(".zip")) {
                unzip(archivePath, tempDir);
            } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
                tarFile = ungzip(archivePath);
                untar(tarFile, tempDir);
            } else if (name.endsWith(".gz")) {
                gunzipSingle(archivePath, tempDir);
            } else {
                throw new IllegalArgumentException("Unsupported file type: " + name);
            }

            final Path biggest;
            try (final Stream<Path> files = Files.walk(tempDir)) {
                biggest = files.filter(Files::isRegularFile)
                        .max(Comparator.comparingLong(f -> f.toFile().length()))
                        .orElseThrow(() -> new IOException("No files found after extraction"));
            }

            Files.createDirectories(target.getParent());
            Files.copy(biggest, target, REPLACE_EXISTING);
            return target;
        } finally {
            if (tarFile != null) {
                Files.deleteIfExists(tarFile);
            }
            deleteDirectory(tempDir);
        }
    }

    private static void unzip(final Path zipFile, final Path destDir) throws IOException {
        try (final ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                final Path newPath = destDir.resolve(entry.getName()).normalize();
                if (!newPath.startsWith(destDir)) throw new IOException("Bad zip entry");
                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    try (final OutputStream os = Files.newOutputStream(newPath)) {
                        copy(zis, os);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static Path ungzip(final Path gzFile) throws IOException {
        final Path out = Files.createTempFile("untar_", ".tar");
        try (final GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(gzFile));
             final OutputStream os = Files.newOutputStream(out)) {
            copy(gis, os);
        }
        return out;
    }

    private static void gunzipSingle(final Path gzFile, final Path destDir) throws IOException {
        // Extract the base filename without .gz extension
        String fileName = gzFile.getFileName().toString();
        if (fileName.toLowerCase().endsWith(".gz")) {
            fileName = fileName.substring(0, fileName.length() - 3);
        }

        final Path out = destDir.resolve(fileName);
        Files.createDirectories(destDir);
        try (final GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(gzFile));
             final OutputStream os = Files.newOutputStream(out)) {
            copy(gis, os);
        }
    }

    private static void untar(final Path tarFile, final Path destDir) throws IOException {
        try (final InputStream is = Files.newInputStream(tarFile)) {
            final byte[] header = new byte[512];
            while (is.read(header) == 512) {
                final String name = extractName(header);
                if (name.isEmpty()) break;
                final long size = extractSize(header);
                final Path filePath = destDir.resolve(name).normalize();
                if (!filePath.startsWith(destDir)) throw new IOException("Bad tar entry");
                if (name.endsWith("/")) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    try (final OutputStream os = Files.newOutputStream(filePath)) {
                        copy(is, os, size);
                    }
                }
                final long skip = (512 - (size % 512)) % 512;
                if (skip > 0) is.skip(skip);
            }
        }
    }

    private static String extractName(final byte[] header) {
        int len = 0;
        while (len < 100 && header[len] != 0) len++;
        return new String(header, 0, len);
    }

    private static long extractSize(final byte[] header) {
        long size = 0;
        for (int i = 124; i < 136 && header[i] != 0; i++) {
            size = (size << 3) + (header[i] - '0');
        }
        return size;
    }

    private static void copy(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }

    private static void copy(final InputStream in, final OutputStream out, final long size) throws IOException {
        final byte[] buf = new byte[8192];
        long remaining = size;
        while (remaining > 0) {
            final int len = in.read(buf, 0, (int) Math.min(buf.length, remaining));
            if (len == -1) break;
            out.write(buf, 0, len);
            remaining -= len;
        }
    }

    private Decompressor() {
        // Utility class
    }
}
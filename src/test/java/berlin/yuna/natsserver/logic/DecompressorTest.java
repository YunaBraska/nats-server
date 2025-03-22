package berlin.yuna.natsserver.logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecompressorTest {

    @TempDir
    Path tempDir;

    private Path testZipFile;
    private Path testGzipFile;
    private Path testTarGzFile;
    private Path targetDir;

    @BeforeEach
    void setUp() {
        testZipFile = tempDir.resolve("test.zip");
        testGzipFile = tempDir.resolve("test.txt.gz");
        testTarGzFile = tempDir.resolve("test.tar.gz");
        targetDir = tempDir.resolve("target");
    }

    @Test
    void shouldExtractBiggestFileFromZip() throws IOException {
        // Create a zip file with multiple files
        createTestZipFile(testZipFile, "small.txt", "medium.txt", "large.txt");

        // Extract and get the biggest file
        final Path result = Decompressor.extractAndReturnBiggest(testZipFile, targetDir);

        // Verify the result
        assertThat(result).exists();
        assertThat(Files.readString(result)).contains("large file content");
    }

    @Test
    void shouldExtractBiggestFileFromGzip() throws IOException {
        // Create a gzip file with content
        createTestGzipFile(testGzipFile);

        // Extract and get the biggest file
        final Path result = Decompressor.extractAndReturnBiggest(testGzipFile, targetDir);

        // Verify the result
        assertThat(result).exists();
        assertThat(Files.readString(result)).contains("test content");
    }

    @Test
    void shouldExtractBiggestFileFromTarGz() throws IOException {
        // Create a tar.gz file
        createTestTarGzFile(testTarGzFile, "small.txt", "medium.txt", "large.txt");

        // Extract and get the biggest file
        final Path result = Decompressor.extractAndReturnBiggest(testTarGzFile, targetDir);

        // Verify the result
        assertThat(result).exists();
        assertThat(Files.readString(result)).contains("large file content");
    }

    @Test
    void shouldThrowExceptionForUnsupportedFileType() {
        final Path unsupportedFile = tempDir.resolve("test.xyz");
        assertThatThrownBy(() -> Decompressor.extractAndReturnBiggest(unsupportedFile, targetDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    private void createTestZipFile(final Path zipFile, final String... fileNames) throws IOException {
        try (final ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (String fileName : fileNames) {
                final ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                
                // Make "large.txt" actually the largest file
                final String content;
                if (fileName.equals("large.txt")) {
                    content = "large file content " + "X".repeat(100);
                } else if (fileName.equals("medium.txt")) {
                    content = "medium content";
                } else {
                    content = "small";
                }
                
                zos.write(content.getBytes());
                zos.closeEntry();
            }
        }
    }

    private void createTestGzipFile(final Path gzFile) throws IOException {
        // First create a regular file
        final Path originalFile = tempDir.resolve("test.txt");
        Files.writeString(originalFile, "test content");
        
        // Then gzip it
        try (final OutputStream fos = Files.newOutputStream(gzFile);
             final GZIPOutputStream gos = new GZIPOutputStream(fos)) {
            gos.write(Files.readAllBytes(originalFile));
        }
    }

    private void createTestTarGzFile(final Path tarGzFile, final String... fileNames) throws IOException {
        // Create a tar file first
        final Path tarFile = tempDir.resolve("temp.tar");
        try (final var os = Files.newOutputStream(tarFile)) {
            for (String fileName : fileNames) {
                // Prepare content - make large.txt actually large
                final String content;
                if (fileName.equals("large.txt")) {
                    content = "large file content " + "X".repeat(100);
                } else if (fileName.equals("medium.txt")) {
                    content = "medium content";
                } else {
                    content = "small";
                }
                
                // Write tar header with correct size
                writeTarHeader(os, fileName, content.length());
                
                // Write file content
                os.write(content.getBytes());
                
                // Write padding
                final int padding = 512 - (content.length() % 512);
                if (padding < 512) {
                    os.write(new byte[padding]);
                }
            }
            // Write end of archive
            os.write(new byte[1024]);
        }

        // Compress the tar file
        try (final GZIPOutputStream gos = new GZIPOutputStream(Files.newOutputStream(tarGzFile))) {
            Files.copy(tarFile, gos);
        }
    }

    private void writeTarHeader(final OutputStream os, final String fileName, final int contentLength) throws IOException {
        final byte[] header = new byte[512];
        // Write filename
        System.arraycopy(fileName.getBytes(), 0, header, 0, fileName.length());
        // Write file size (in octal)
        final String size = String.format("%011o", contentLength);
        System.arraycopy(size.getBytes(), 0, header, 124, size.length());
        // Write file type (regular file)
        header[156] = '0';
        os.write(header);
    }
} 
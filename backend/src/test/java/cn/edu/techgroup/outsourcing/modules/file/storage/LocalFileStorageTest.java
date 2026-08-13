package cn.edu.techgroup.outsourcing.modules.file.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.edu.techgroup.outsourcing.config.AppProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesWithRandomExtensionlessKeyAndSupportsLoadAndDelete() throws Exception {
        LocalFileStorage storage = storage();
        byte[] content = "safe content".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        String key = storage.store(new MockMultipartFile(
                "file", "report.txt", "text/plain", content));

        assertTrue(key.matches("\\d{4}/\\d{2}/[0-9a-f-]{36}"));
        assertFalse(key.endsWith(".txt"));
        assertArrayEquals(content, storage.load(key).getContentAsByteArray());

        storage.delete(key);
        assertThrows(IOException.class, () -> storage.load(key));
    }

    @Test
    void rejectsTraversalAndAbsoluteStorageKeys() throws Exception {
        LocalFileStorage storage = storage();

        assertThrows(IOException.class, () -> storage.load("../../secret.txt"));
        assertThrows(IOException.class,
                () -> storage.load(temporaryDirectory.toAbsolutePath().toString()));
    }

    @Test
    void createsOnlyPrivateRootAndTemporaryDirectoryInitially() throws Exception {
        Path uploadRoot = temporaryDirectory.resolve("nested/uploads");

        new LocalFileStorage(new AppProperties(
                List.of("http://localhost:5173"),
                uploadRoot.toString(), 20 * 1024 * 1024L, 5));

        assertTrue(Files.isDirectory(uploadRoot.resolve(".tmp")));
    }

    private LocalFileStorage storage() throws IOException {
        return new LocalFileStorage(new AppProperties(
                List.of("http://localhost:5173"),
                temporaryDirectory.resolve("uploads").toString(),
                20 * 1024 * 1024L,
                5));
    }
}

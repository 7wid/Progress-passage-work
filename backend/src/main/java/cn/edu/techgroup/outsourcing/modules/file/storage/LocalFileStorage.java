package cn.edu.techgroup.outsourcing.modules.file.storage;

import cn.edu.techgroup.outsourcing.config.AppProperties;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalFileStorage implements FileStorage {
    private final Path root;
    private final Path temporaryRoot;

    public LocalFileStorage(AppProperties properties) throws IOException {
        root = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
        temporaryRoot = root.resolve(".tmp").normalize();
        Files.createDirectories(temporaryRoot);
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String storageKey = "%04d/%02d/%s".formatted(
                today.getYear(), today.getMonthValue(), UUID.randomUUID());
        Path destination = resolveSafe(storageKey);
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(temporaryRoot, "upload-", ".tmp");
        try {
            try (var input = file.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination);
            }
            return storageKey;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public Resource load(String storageKey) throws IOException {
        Path path = resolveSafe(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new java.nio.file.NoSuchFileException(path.toString());
        }
        return new PathResource(path);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveSafe(storageKey));
    }

    private Path resolveSafe(String storageKey) throws IOException {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IOException("Invalid storage key");
        }
        Path relative = Path.of(storageKey).normalize();
        Path resolved = root.resolve(relative).normalize();
        if (relative.isAbsolute() || relative.startsWith("..")
                || !resolved.startsWith(root) || resolved.equals(root)) {
            throw new IOException("Invalid storage key");
        }
        return resolved;
    }
}

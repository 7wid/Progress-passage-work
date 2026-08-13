package cn.edu.techgroup.outsourcing.modules.file.storage;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    String store(MultipartFile file) throws IOException;
    Resource load(String storageKey) throws IOException;
    void delete(String storageKey) throws IOException;
}

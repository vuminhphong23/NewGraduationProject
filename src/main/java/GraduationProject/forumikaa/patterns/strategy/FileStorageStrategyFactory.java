package GraduationProject.forumikaa.patterns.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory class for creating file storage strategies
 * Uses Strategy Pattern to switch between different storage implementations
 */
@Component
public class FileStorageStrategyFactory {

    @Value("${app.file.storage.strategy:local}")
    private String storageStrategy;

    @Autowired
    @Qualifier("localStorageStrategy")
    private FileStorageStrategy localStorageStrategy;

    @Autowired
    @Qualifier("cloudinaryStorageStrategy")
    private FileStorageStrategy cloudinaryStorageStrategy;

    public FileStorageStrategy getStorageStrategy() {
        return getStorageStrategy(storageStrategy);
    }

    public FileStorageStrategy getStorageStrategy(String strategyType) {
        switch (strategyType.toLowerCase()) {
            case "local":
                return localStorageStrategy;
            case "cloudinary":
                return cloudinaryStorageStrategy;
            default:
                throw new IllegalArgumentException("Unsupported storage strategy: " + strategyType);
        }
    }

    public String getCurrentStrategyType() {
        return storageStrategy;
    }

    public boolean isLocalStorage() {
        return "local".equalsIgnoreCase(storageStrategy);
    }

    public boolean isCloudinaryStorage() {
        return "cloudinary".equalsIgnoreCase(storageStrategy);
    }
}

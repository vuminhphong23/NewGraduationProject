package GraduationProject.forumikaa.patterns.strategy;

import GraduationProject.forumikaa.patterns.strategy.impl.ChatCloudinaryStorageStrategy;
import GraduationProject.forumikaa.patterns.strategy.impl.ChatLocalStorageStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatFileStorageStrategyFactory {

    @Value("${app.chat.file.storage.strategy:cloudinary}")
    private String storageStrategy;

    @Autowired
    private ChatCloudinaryStorageStrategy chatCloudinaryStorageStrategy;

    @Autowired
    private ChatLocalStorageStrategy chatLocalStorageStrategy;

    public ChatFileStorageStrategy getStorageStrategy() {
        System.out.println("ChatFileStorageStrategyFactory: Getting strategy for: " + storageStrategy);
        switch (storageStrategy.toLowerCase()) {
            case "cloudinary":
                System.out.println("ChatFileStorageStrategyFactory: Returning ChatCloudinaryStorageStrategy");
                return chatCloudinaryStorageStrategy;
            case "local":
            default:
                System.out.println("ChatFileStorageStrategyFactory: Returning ChatLocalStorageStrategy");
                return chatLocalStorageStrategy;
        }
    }
}

package vn.dentalcare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class AvatarWebConfig implements WebMvcConfigurer {
    private final String uploadDirectory;

    public AvatarWebConfig(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        this.uploadDirectory = uploadDirectory;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path avatarDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("avatars");
        String location = avatarDirectory.toUri().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler("/uploads/avatars/**").addResourceLocations(location);
    }
}

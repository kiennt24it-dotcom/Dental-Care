package vn.dentalcare.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import vn.dentalcare.exception.ApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AvatarStorageServiceTest {
    @TempDir Path temporaryDirectory;

    @Test
    void storesValidatedPngAvatar() throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"png",bytes);
        AvatarStorageService storage=new AvatarStorageService(temporaryDirectory.toString());

        String url=storage.store(42,new MockMultipartFile("file","avatar.png","image/png",bytes.toByteArray()));

        assertTrue(url.startsWith("/uploads/avatars/42-"));
        assertTrue(url.endsWith(".png"));
        assertTrue(Files.exists(temporaryDirectory.resolve(url.substring("/uploads/".length()))));
    }

    @Test
    void rejectsNonImageAvatar() {
        AvatarStorageService storage=new AvatarStorageService(temporaryDirectory.toString());
        MockMultipartFile invalid=new MockMultipartFile("file","avatar.png","image/png","not an image".getBytes());

        assertThrows(ApiException.class,()->storage.store(42,invalid));
    }
}

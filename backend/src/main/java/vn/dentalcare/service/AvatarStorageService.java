package vn.dentalcare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.dentalcare.exception.ApiException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
public class AvatarStorageService {
    private static final Logger log = LoggerFactory.getLogger(AvatarStorageService.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private static final long MAX_PIXELS = 16_000_000L;
    private final Path avatarDirectory;

    public AvatarStorageService(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        this.avatarDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("avatars");
    }

    public String store(long userId, MultipartFile file) {
        if (file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh đại diện");
        if (file.getSize() > MAX_BYTES) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh đại diện không được vượt quá 5 MB");
        try {
            byte[] content = file.getBytes();
            String extension = validateImage(content);
            Files.createDirectories(avatarDirectory);
            Path target = avatarDirectory.resolve(userId + "-" + UUID.randomUUID() + "." + extension).normalize();
            if (!target.startsWith(avatarDirectory)) throw new ApiException(HttpStatus.BAD_REQUEST, "Tên tệp không hợp lệ");
            Path temporary = avatarDirectory.resolve(UUID.randomUUID() + ".tmp");
            try {
                Files.write(temporary, content);
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                    Files.move(temporary, target);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return "/uploads/avatars/" + target.getFileName();
        } catch (IOException ex) {
            log.error("Could not store avatar for user {}", userId, ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu ảnh đại diện", ex);
        }
    }

    private static String validateImage(byte[] content) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) throw invalidImage();
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalidImage();
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (!(format.equals("png") || format.equals("jpeg") || format.equals("jpg"))
                    || width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION
                    || (long) width * height > MAX_PIXELS) throw invalidImage();
                return format.equals("png") ? "png" : "jpg";
            } finally {
                reader.dispose();
            }
        }
    }

    private static ApiException invalidImage() {
        return new ApiException(HttpStatus.BAD_REQUEST, "Ảnh đại diện phải là PNG hoặc JPEG hợp lệ, tối đa 4096×4096 điểm ảnh");
    }
}

package com.uniremington.marketplace.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProductoImageStorageService {

    private static final long MAX_IMAGE_PIXELS = 20_000_000L;

    private final Path uploadDirectory;
    private final long maxImageBytes;

    public ProductoImageStorageService(
            @Value("${marketplace.upload.directory:${user.home}/marketplace-uploads}") String uploadDirectory,
            @Value("${spring.servlet.multipart.max-file-size:5MB}") String maxImageSize
    ) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.maxImageBytes = parseMaxFileSize(maxImageSize);
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un archivo de imagen");
        }
        if (file.getSize() > maxImageBytes) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "La imagen supera el tamaño permitido");
        }

        String extension = validateImage(file);
        Files.createDirectories(uploadDirectory);
        String fileName = UUID.randomUUID() + "." + extension;
        Path destination = uploadDirectory.resolve(fileName).normalize();
        if (!destination.getParent().equals(uploadDirectory)) {
            throw new IllegalStateException("La ruta de almacenamiento de imagen no es válida");
        }

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination);
        }
        return "/uploads/" + fileName;
    }

    public void delete(String publicUrl) throws IOException {
        if (publicUrl == null || !publicUrl.matches("^/uploads/[a-f0-9-]+\\.(jpg|png|gif)$")) {
            return;
        }
        Path file = uploadDirectory.resolve(Path.of(publicUrl.substring("/uploads/".length())))
                .normalize();
        if (file.getParent().equals(uploadDirectory)) {
            Files.deleteIfExists(file);
        }
    }

    public String resourceLocation() {
        String location = uploadDirectory.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }

    private String validateImage(MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no es una imagen válida");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Formato no admitido. Usa una imagen JPG, PNG o GIF"
                );
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String extension = switch (format) {
                    case "jpeg", "jpg" -> "jpg";
                    case "png" -> "png";
                    case "gif" -> "gif";
                    default -> throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Formato no admitido. Usa una imagen JPG, PNG o GIF"
                    );
                };

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "La resolución de la imagen no es válida o es demasiado grande"
                    );
                }
                if (reader.read(0) == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen");
                }
                return extension;
            } finally {
                reader.dispose();
            }
        }
    }

    private long parseMaxFileSize(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            if (normalized.endsWith("MB")) {
                return Math.multiplyExact(Long.parseLong(normalized.substring(0, normalized.length() - 2).trim()),
                        1024L * 1024L);
            }
            if (normalized.endsWith("KB")) {
                return Math.multiplyExact(Long.parseLong(normalized.substring(0, normalized.length() - 2).trim()),
                        1024L);
            }
            if (normalized.endsWith("B")) {
                return Long.parseLong(normalized.substring(0, normalized.length() - 1).trim());
            }
            return Long.parseLong(normalized);
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("Tamaño máximo de carga no válido: " + value, exception);
        }
    }
}

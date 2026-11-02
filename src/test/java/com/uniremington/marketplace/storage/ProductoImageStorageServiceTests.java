package com.uniremington.marketplace.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductoImageStorageServiceTests {

    @TempDir
    Path tempDirectory;

    @Test
    void storesAndDeletesValidImage() throws IOException {
        ProductoImageStorageService storage = new ProductoImageStorageService(
                tempDirectory.toString(),
                "5MB"
        );
        MockMultipartFile file = new MockMultipartFile(
                "foto",
                "producto.png",
                "image/png",
                pngImage()
        );

        String publicUrl = storage.store(file);
        Path storedFile = tempDirectory.resolve(publicUrl.substring("/uploads/".length()));

        assertTrue(Files.exists(storedFile));
        assertTrue(publicUrl.matches("^/uploads/[a-f0-9-]+\\.png$"));
        storage.delete(publicUrl);
        assertFalse(Files.exists(storedFile));
    }

    @Test
    void rejectsNonImageFiles() {
        ProductoImageStorageService storage = new ProductoImageStorageService(
                tempDirectory.toString(),
                "5MB"
        );
        MockMultipartFile file = new MockMultipartFile(
                "foto",
                "documento.txt",
                "text/plain",
                "no es una imagen".getBytes()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> storage.store(file)
        );

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void rejectsFilesAboveConfiguredLimit() {
        ProductoImageStorageService storage = new ProductoImageStorageService(
                tempDirectory.toString(),
                "1B"
        );
        MockMultipartFile file = new MockMultipartFile(
                "foto",
                "producto.png",
                "image/png",
                new byte[]{1, 2}
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> storage.store(file)
        );

        assertEquals(413, exception.getStatusCode().value());
    }

    private byte[] pngImage() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}

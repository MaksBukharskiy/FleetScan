package com.fleetScan.taxiService.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class PhotoStorageService {

    private final Path uploadDir;

    public PhotoStorageService(@Value("${fleetscan.upload-dir:/tmp/fleetscan-uploads}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public String store(byte[] bytes, String originalFilename) {
        try {
            Files.createDirectories(uploadDir);
            String extension = extractExtension(originalFilename);
            Path target = uploadDir.resolve(UUID.randomUUID() + extension);
            Files.write(target, bytes);
            return target.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить фото", e);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не передан.");
        }
        try {
            Files.createDirectories(uploadDir);
            String extension = extractExtension(file.getOriginalFilename());
            Path target = uploadDir.resolve(UUID.randomUUID() + extension);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить загруженный файл", e);
        }
    }

    public byte[] read(String path) {
        try {
            return Files.readAllBytes(resolve(path));
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать фото", e);
        }
    }

    public String contentType(String path) {
        try {
            String detected = Files.probeContentType(resolve(path));
            return detected == null ? "image/jpeg" : detected;
        } catch (IOException e) {
            return "image/jpeg";
        }
    }

    public Path resolve(String path) {
        Path resolved = Path.of(path).toAbsolutePath().normalize();
        if (!resolved.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Недопустимый путь к файлу.");
        }
        return resolved;
    }

    public boolean exists(String path) {
        return Files.exists(resolve(path));
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return ".jpg";
        }
        String sanitized = filename.trim();
        int dot = sanitized.lastIndexOf('.');
        if (dot < 0 || dot == sanitized.length() - 1) {
            return ".jpg";
        }
        String extension = sanitized.substring(dot);
        return extension.length() > 8 ? ".jpg" : extension;
    }
}

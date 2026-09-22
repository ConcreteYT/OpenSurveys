package com.opensurveys.service;

import com.opensurveys.model.QuestionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class UploadStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Path uploadRoot;

    public UploadStorageService(@Value("${app.upload.dir:data/uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public void ensureRootExists() throws IOException {
        Files.createDirectories(uploadRoot);
    }

    public Path stagingDirectory(long formId, String stagingId) {
        validateStagingId(stagingId);
        return uploadRoot.resolve("staging").resolve(stagingId).resolve("form-" + formId);
    }

    public Path questionStagingDirectory(long formId, String stagingId, long questionId) {
        return stagingDirectory(formId, stagingId).resolve("q-" + questionId);
    }

    public Path formDirectory(long formId) {
        return uploadRoot.resolve("forms").resolve(String.valueOf(formId));
    }

    public Path resolveFormFile(long formId, String storedName) {
        validateStoredFileName(storedName);
        return formDirectory(formId).resolve(storedName).normalize();
    }

    public boolean formFileExists(long formId, String storedName) {
        try {
            Path resolved = resolveFormFile(formId, storedName);
            return resolved.startsWith(formDirectory(formId)) && Files.isRegularFile(resolved);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Replaces any prior staged files for this question, validates count/size/types, stores under staging.
     *
     * @return stored file names (internal id + extension) in upload order
     */
    public List<String> stageQuestionFiles(
            long formId,
            String stagingId,
            long questionId,
            int maxFiles,
            List<MultipartFile> files
    ) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new UploadValidationException("At least one image is required");
        }
        if (files.size() > maxFiles) {
            throw new UploadValidationException("Too many files (max " + maxFiles + ")");
        }

        long totalBytes = 0;
        for (MultipartFile file : files) {
            totalBytes += file.getSize();
        }
        if (totalBytes > QuestionType.IMAGE_UPLOAD_MAX_BYTES_PER_QUESTION) {
            throw new UploadValidationException("Total size exceeds 100 MB for this question");
        }

        Path questionDir = questionStagingDirectory(formId, stagingId, questionId);
        if (Files.exists(questionDir)) {
            deleteDirectoryRecursive(questionDir);
        }
        Files.createDirectories(questionDir);

        List<String> storedNames = new ArrayList<>();
        for (MultipartFile file : files) {
            storedNames.add(saveValidatedImage(file, questionDir));
        }
        return storedNames;
    }

    public List<String> listStagedFileNames(long formId, String stagingId, long questionId) throws IOException {
        Path questionDir = questionStagingDirectory(formId, stagingId, questionId);
        if (!Files.isDirectory(questionDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(questionDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }

    /**
     * Moves staged files for one question into the form directory. Returns semicolon-separated stored names.
     */
    public String moveStagedQuestionToFinal(long formId, String stagingId, long questionId) throws IOException {
        List<String> staged = listStagedFileNames(formId, stagingId, questionId);
        if (staged.isEmpty()) {
            throw new UploadValidationException("Missing staged files for question " + questionId);
        }

        Path formDir = formDirectory(formId);
        Files.createDirectories(formDir);
        Path questionDir = questionStagingDirectory(formId, stagingId, questionId);

        List<String> finalNames = new ArrayList<>();
        for (String name : staged) {
            Path source = questionDir.resolve(name);
            Path target = formDir.resolve(name);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            finalNames.add(name);
        }
        return String.join(";", finalNames);
    }

    public void deleteStagingSession(String stagingId) {
        try {
            validateStagingId(stagingId);
            Path sessionRoot = uploadRoot.resolve("staging").resolve(stagingId);
            if (Files.exists(sessionRoot)) {
                deleteDirectoryRecursive(sessionRoot);
            }
        } catch (IOException | IllegalArgumentException ignored) {
            // best-effort cleanup
        }
    }

    public void deleteFormUploads(long formId) throws IOException {
        Path formDir = formDirectory(formId);
        if (Files.exists(formDir)) {
            deleteDirectoryRecursive(formDir);
        }
    }

    public void rollbackMovedFiles(long formId, List<String> storedNames) {
        if (storedNames == null) {
            return;
        }
        for (String name : storedNames) {
            try {
                Path path = resolveFormFile(formId, name);
                Files.deleteIfExists(path);
            } catch (IOException | IllegalArgumentException ignored) {
                // best-effort
            }
        }
    }

    private String saveValidatedImage(MultipartFile file, Path targetDir) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new UploadValidationException("Empty file");
        }
        String original = file.getOriginalFilename();
        String extension = extensionFromFilename(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UploadValidationException("File type not allowed: " + extension);
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
                throw new UploadValidationException("Content type not allowed");
            }
        }

        byte[] bytes = file.getBytes();
        if (bytes.length == 0) {
            throw new UploadValidationException("Empty file");
        }
        try (InputStream imageStream = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(imageStream);
            if (image == null) {
                throw new UploadValidationException("Not a valid image file");
            }
        }

        String storedName = UUID.randomUUID() + "." + extension;
        Path target = targetDir.resolve(storedName);
        Files.write(target, bytes);
        return storedName;
    }

    private static String extensionFromFilename(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public static void validateStagingId(String stagingId) {
        if (stagingId == null || stagingId.isBlank()) {
            throw new IllegalArgumentException("Invalid staging id");
        }
        try {
            UUID.fromString(stagingId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid staging id");
        }
    }

    public static void validateStoredFileName(String storedName) {
        if (storedName == null || storedName.isBlank() || storedName.contains("/") || storedName.contains("\\")
                || storedName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }
    }

    public static List<String> splitAnswerFileNames(String answer) {
        if (answer == null || answer.isBlank()) {
            return List.of();
        }
        String[] parts = answer.split(";");
        List<String> names = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        return names;
    }

    private static void deleteDirectoryRecursive(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    public static class UploadValidationException extends RuntimeException {
        public UploadValidationException(String message) {
            super(message);
        }
    }
}

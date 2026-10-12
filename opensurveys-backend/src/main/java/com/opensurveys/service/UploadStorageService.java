package com.opensurveys.service;

import com.opensurveys.model.QuestionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class UploadStorageService {

    private static final DateTimeFormatter COMMITTED_IMAGE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );
    /** Reject before a full decode so a small compressed file cannot expand without bound. */
    static final long MAX_IMAGE_PIXELS = 20_000_000L;
    static final long STAGING_MAX_AGE_MILLIS = 24L * 60 * 60 * 1000;

    private final Path uploadRoot;

    public UploadStorageService(@Value("${app.upload.dir:data/uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public void ensureRootExists() throws IOException {
        Files.createDirectories(uploadRoot);
    }

    private Path stagingDirectory(long formId, String stagingId) {
        validateStagingId(stagingId);
        return uploadRoot.resolve("staging").resolve(stagingId).resolve("form-" + formId);
    }

    private Path questionStagingDirectory(long formId, String stagingId, long questionId) {
        return stagingDirectory(formId, stagingId).resolve("q-" + questionId);
    }

    private Path formDirectory(long formId) {
        return uploadRoot.resolve("forms").resolve(String.valueOf(formId));
    }

    private Path resolveFormFile(long formId, String storedName) {
        validateStoredFileName(storedName);
        return formDirectory(formId).resolve(storedName).normalize();
    }

    /**
     * @return the stored file's path, or empty if the name is invalid or no such file exists for this form
     */
    public Optional<Path> findFormFile(long formId, String storedName) {
        try {
            Path resolved = resolveFormFile(formId, storedName);
            if (resolved.startsWith(formDirectory(formId)) && Files.isRegularFile(resolved)) {
                return Optional.of(resolved);
            }
        } catch (IllegalArgumentException ignored) {
            // invalid name
        }
        return Optional.empty();
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
        sweepExpiredStagingDirectories();
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
        deleteDirectoryRecursive(questionDir);
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
     * Moves staged files for one question into the form directory.
     *
     * @return the moved stored names in sorted order; empty (and nothing moved) if none were staged
     */
    public List<String> moveStagedQuestionToFinal(long formId, String stagingId, long questionId) throws IOException {
        List<String> staged = listStagedFileNames(formId, stagingId, questionId);
        if (staged.isEmpty()) {
            return staged;
        }

        Path formDir = formDirectory(formId);
        Files.createDirectories(formDir);
        Path questionDir = questionStagingDirectory(formId, stagingId, questionId);

        for (String name : staged) {
            Files.move(questionDir.resolve(name), formDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
        return staged;
    }

    /**
     * Final stored name: {@code yyyy-MM-dd-HH-mm-ss-{answerId}-{index}.{extension}}.
     * {@code index} is 1-based among the files of that answer.
     */
    public static String finalImageFileName(LocalDateTime committedAt, long answerId, int index, String extension) {
        return COMMITTED_IMAGE_TIMESTAMP.format(committedAt) + "-" + answerId + "-" + index + "." + extension;
    }

    /**
     * Renames files already moved into the form directory. {@code rollbackNames} is updated
     * in place so a later failure deletes the names currently on disk.
     */
    public List<String> renameCommittedImages(
            long formId,
            List<String> storedNames,
            long answerId,
            LocalDateTime committedAt,
            List<String> rollbackNames
    ) throws IOException {
        List<String> renamed = new ArrayList<>();
        Path formDir = formDirectory(formId);
        for (int i = 0; i < storedNames.size(); i++) {
            String oldName = storedNames.get(i);
            validateStoredFileName(oldName);
            String extension = extensionFromFilename(oldName);
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IOException("Stored file has no allowed extension");
            }
            String newName = finalImageFileName(committedAt, answerId, i + 1, extension);
            validateStoredFileName(newName);
            Path source = formDir.resolve(oldName).normalize();
            Path target = formDir.resolve(newName).normalize();
            if (!source.startsWith(formDir) || !target.startsWith(formDir)) {
                throw new IOException("Invalid stored path");
            }
            Files.move(source, target);
            int rollbackIndex = rollbackNames.indexOf(oldName);
            if (rollbackIndex >= 0) {
                rollbackNames.set(rollbackIndex, newName);
            }
            renamed.add(newName);
        }
        return renamed;
    }

    /**
     * Deletes staging session directories whose own lastModified time is older than 24 hours.
     * Younger sessions, including one being written by the current request, are left in place.
     */
    public void sweepExpiredStagingDirectories() {
        Path stagingRoot = uploadRoot.resolve("staging");
        if (!Files.isDirectory(stagingRoot)) {
            return;
        }
        long cutoff = System.currentTimeMillis() - STAGING_MAX_AGE_MILLIS;
        try (Stream<Path> children = Files.list(stagingRoot)) {
            for (Path child : children.toList()) {
                try {
                    if (!Files.isDirectory(child)) {
                        continue;
                    }
                    if (Files.getLastModifiedTime(child).toMillis() < cutoff) {
                        deleteDirectoryRecursive(child);
                    }
                } catch (IOException | UncheckedIOException ignored) {
                    // best-effort; a locked or racing session can be retried later
                }
            }
        } catch (IOException ignored) {
            // best-effort
        }
    }

    public void deleteStagingSession(String stagingId) {
        try {
            validateStagingId(stagingId);
            deleteDirectoryRecursive(uploadRoot.resolve("staging").resolve(stagingId));
        } catch (IOException | IllegalArgumentException ignored) {
            // best-effort cleanup
        }
    }

    public void deleteFormUploads(long formId) throws IOException {
        deleteDirectoryRecursive(formDirectory(formId));
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

        String storedName = UUID.randomUUID() + "." + extension;
        Path target = targetDir.resolve(storedName);
        boolean accepted = false;
        try {
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.size(target) == 0) {
                throw new UploadValidationException("Empty file");
            }
            assertImageWithinPixelLimit(target);
            accepted = true;
            return storedName;
        } finally {
            if (!accepted) {
                Files.deleteIfExists(target);
            }
        }
    }

    /**
     * Reads dimensions from the file already on disk, rejects decompression bombs before a full
     * decode, then decodes once (the bytes are not also held in a byte[]) to reject non-images.
     */
    private static void assertImageWithinPixelLimit(Path path) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) {
                throw new UploadValidationException("Not a valid image file");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new UploadValidationException("Not a valid image file");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * (long) height;
                if (width <= 0 || height <= 0 || pixels > MAX_IMAGE_PIXELS) {
                    throw new UploadValidationException("Image exceeds " + MAX_IMAGE_PIXELS + " pixels");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new UploadValidationException("Not a valid image file");
                }
                image.flush();
            } catch (IIOException | IndexOutOfBoundsException e) {
                throw new UploadValidationException("Not a valid image file");
            } finally {
                reader.dispose();
            }
        }
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

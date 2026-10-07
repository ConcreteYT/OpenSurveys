package com.opensurveys.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadStorageServiceTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @TempDir
    Path root;

    private static MultipartFile png(String name) {
        return new MockMultipartFile("files", name, "image/png", PNG);
    }

    @Test
    void stageMoveFindAndDelete() throws IOException {
        UploadStorageService service = new UploadStorageService(root.toString());
        String stagingId = UUID.randomUUID().toString();

        List<String> staged = service.stageQuestionFiles(5, stagingId, 9, 2, List.of(png("a.png"), png("b.PNG")));
        assertEquals(2, staged.size());
        assertEquals(staged.stream().sorted().toList(), service.listStagedFileNames(5, stagingId, 9));

        List<String> moved = service.moveStagedQuestionToFinal(5, stagingId, 9);
        assertEquals(staged.stream().sorted().toList(), moved);
        assertTrue(service.listStagedFileNames(5, stagingId, 9).isEmpty());
        for (String name : moved) {
            assertTrue(service.findFormFile(5, name).isPresent());
            assertFalse(service.findFormFile(6, name).isPresent());
        }

        service.deleteStagingSession(stagingId);
        assertFalse(Files.exists(root.resolve("staging").resolve(stagingId)));

        service.deleteFormUploads(5);
        assertFalse(Files.exists(root.resolve("forms").resolve("5")));
        service.deleteFormUploads(5);
    }

    @Test
    void moveWithNothingStagedMovesNothing() throws IOException {
        UploadStorageService service = new UploadStorageService(root.toString());
        assertTrue(service.moveStagedQuestionToFinal(5, UUID.randomUUID().toString(), 9).isEmpty());
        assertFalse(Files.exists(root.resolve("forms").resolve("5")));
    }

    @Test
    void restagingReplacesPreviousFiles() throws IOException {
        UploadStorageService service = new UploadStorageService(root.toString());
        String stagingId = UUID.randomUUID().toString();
        service.stageQuestionFiles(1, stagingId, 2, 3, List.of(png("a.png"), png("b.png")));
        List<String> second = service.stageQuestionFiles(1, stagingId, 2, 3, List.of(png("c.png")));
        assertEquals(second, service.listStagedFileNames(1, stagingId, 2));
    }

    @Test
    void findFormFileRejectsUnsafeNames() throws IOException {
        UploadStorageService service = new UploadStorageService(root.toString());
        Files.createDirectories(root.resolve("forms").resolve("1"));
        Files.write(root.resolve("forms").resolve("secret.png"), PNG);

        assertTrue(service.findFormFile(1, "../secret.png").isEmpty());
        assertTrue(service.findFormFile(1, "..\\secret.png").isEmpty());
        assertTrue(service.findFormFile(1, "").isEmpty());
        assertTrue(service.findFormFile(1, null).isEmpty());
        assertTrue(service.findFormFile(1, "missing.png").isEmpty());
    }

    @Test
    void stagingValidationErrors() {
        UploadStorageService service = new UploadStorageService(root.toString());
        String stagingId = UUID.randomUUID().toString();

        assertThrows(UploadStorageService.UploadValidationException.class,
                () -> service.stageQuestionFiles(1, stagingId, 2, 1, List.of(png("a.png"), png("b.png"))));
        assertThrows(UploadStorageService.UploadValidationException.class,
                () -> service.stageQuestionFiles(1, stagingId, 2, 1, List.of()));
        assertThrows(UploadStorageService.UploadValidationException.class,
                () -> service.stageQuestionFiles(1, stagingId, 2, 1,
                        List.of(new MockMultipartFile("files", "a.png", "image/png", "nope".getBytes()))));
        assertThrows(IllegalArgumentException.class,
                () -> service.stageQuestionFiles(1, "not-a-uuid", 2, 1, List.of(png("a.png"))));
    }

    @Test
    void splitAnswerFileNames() {
        assertEquals(List.of("a.png", "b.png"), UploadStorageService.splitAnswerFileNames(" a.png ;; b.png;"));
        assertTrue(UploadStorageService.splitAnswerFileNames(null).isEmpty());
        assertTrue(UploadStorageService.splitAnswerFileNames(" ").isEmpty());
    }
}

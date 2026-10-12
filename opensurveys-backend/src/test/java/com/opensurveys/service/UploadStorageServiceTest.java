package com.opensurveys.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;

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
    void rejectsImageWhosePixelCountExceedsLimit() throws IOException {
        UploadStorageService service = new UploadStorageService(root.toString());
        byte[] bomb = pngClaimingDimensions(20_000, 1_001);
        assertTrue((long) 20_000 * 1_001 > UploadStorageService.MAX_IMAGE_PIXELS);
        MultipartFile file = new MockMultipartFile("files", "bomb.png", "image/png", bomb);

        UploadStorageService.UploadValidationException error = assertThrows(
                UploadStorageService.UploadValidationException.class,
                () -> service.stageQuestionFiles(1, UUID.randomUUID().toString(), 2, 1, List.of(file)));
        assertTrue(error.getMessage().contains("pixels"));
        try (var children = Files.list(root.resolve("staging"))) {
            for (Path session : children.toList()) {
                try (var nested = Files.walk(session)) {
                    assertTrue(nested.filter(Files::isRegularFile).findAny().isEmpty());
                }
            }
        }
    }

    private static byte[] pngClaimingDimensions(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
        byte[] ihdr = new byte[13];
        ihdr[0] = (byte) (width >>> 24);
        ihdr[1] = (byte) (width >>> 16);
        ihdr[2] = (byte) (width >>> 8);
        ihdr[3] = (byte) width;
        ihdr[4] = (byte) (height >>> 24);
        ihdr[5] = (byte) (height >>> 16);
        ihdr[6] = (byte) (height >>> 8);
        ihdr[7] = (byte) height;
        ihdr[8] = 8;
        ihdr[9] = 2;
        writePngChunk(out, "IHDR", ihdr);
        writePngChunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void writePngChunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
        out.write(new byte[] {
                (byte) (data.length >>> 24),
                (byte) (data.length >>> 16),
                (byte) (data.length >>> 8),
                (byte) data.length
        });
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        out.write(typeBytes);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        long value = crc.getValue();
        out.write(new byte[] {
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        });
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

    @Test
    void finalImageFileNameUsesCommitTimeAnswerIdAndIndex() {
        LocalDateTime committedAt = LocalDateTime.of(2026, 10, 12, 10, 34, 5);
        String name = UploadStorageService.finalImageFileName(committedAt, 17, 1, "jpg");
        assertEquals("2026-10-12-10-34-05-17-1.jpg", name);
        assertEquals(
                "2026-01-02-03-04-05-8-2.png",
                UploadStorageService.finalImageFileName(LocalDateTime.of(2026, 1, 2, 3, 4, 5), 8, 2, "png"));
        UploadStorageService.validateStoredFileName(name);
        assertEquals(
                List.of(name, "2026-10-12-10-34-05-17-2.jpeg"),
                UploadStorageService.splitAnswerFileNames(name + ";2026-10-12-10-34-05-17-2.jpeg"));
    }

    @Test
    void renameCommittedImagesReplacesStoredNames() throws IOException {
        UploadStorageService service = new UploadStorageService(root.toString());
        String stagingId = UUID.randomUUID().toString();
        service.stageQuestionFiles(5, stagingId, 9, 2, List.of(png("a.jpg"), png("b.png")));
        List<String> moved = service.moveStagedQuestionToFinal(5, stagingId, 9);
        List<String> rollback = new ArrayList<>(moved);
        LocalDateTime committedAt = LocalDateTime.of(2026, 10, 12, 10, 34, 5);

        List<String> renamed = service.renameCommittedImages(5, moved, 17, committedAt, rollback);

        List<String> expected = new ArrayList<>();
        for (int i = 0; i < moved.size(); i++) {
            String oldName = moved.get(i);
            String extension = oldName.substring(oldName.lastIndexOf('.') + 1);
            expected.add(UploadStorageService.finalImageFileName(committedAt, 17, i + 1, extension));
        }
        assertEquals(expected, renamed);
        assertEquals(expected, rollback);
        for (String oldName : moved) {
            assertTrue(service.findFormFile(5, oldName).isEmpty());
        }
        for (String name : renamed) {
            assertTrue(service.findFormFile(5, name).isPresent());
        }
    }
}

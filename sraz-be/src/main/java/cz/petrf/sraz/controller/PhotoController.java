package cz.petrf.sraz.controller;

import cz.petrf.sraz.db.entity.Photo;
import cz.petrf.sraz.security.AppUser;
import cz.petrf.sraz.service.PhotoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/photo")
@Slf4j
public class PhotoController {

  private final PhotoService photoService;

  public PhotoController(PhotoService photoService) {
    this.photoService = photoService;
  }

  private static Map<String, Object> toResponseMap(Photo photo) {
    Map<String, Object> response = new HashMap<>();

    response.put("id", photo.getId());
    response.put("fileName", photo.getFileName());
    response.put("contentType", photo.getContentType());
    response.put("fileSize", photo.getFileSize());
    response.put("createdAt", photo.getCreatedAt());
    response.put("updatedAt", photo.getUpdatedAt());

    return response;
  }

  public static byte[] blobToByteArray(Blob blob) throws SQLException, IOException {
    if (blob==null) {
      return null;
    }

    try (InputStream inputStream = blob.getBinaryStream()) {
      return inputStream.readAllBytes();
    }
  }

  /**
   * Nahrání nové fotografie (přepíše předchozí)
   * POST /api/photo
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadPhoto(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal AppUser appUser) {
    try {
      Photo photo = photoService.createOrUpdatePhoto(file, appUser.getDbUser());
      Map<String, Object> response = toResponseMap(photo);

      response.put("message", "Fotografie byla úspěšně uložen");

      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Chyba při nahrávání souboru", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Chyba při ukládání souboru"));
    }
  }

  /**
   * Aktualizace fotografie
   * PUT /api/photo
   */
  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> updatePhoto(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal AppUser appUser) {
    try {
      Photo photo = photoService.createOrUpdatePhoto(file, appUser.getDbUser());

      Map<String, Object> response = toResponseMap(photo);

      response.put("message", "Fotografie byla úspěšně aktualizována");

      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Načtení fotografie
   * GET /api/photo
   */
  @GetMapping
  public ResponseEntity<?> getPhoto(@AuthenticationPrincipal AppUser appUser) {
    return photoService.getPhoto(appUser.getDbUser())
        .map(photo -> {
          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.parseMediaType(photo.getContentType()));
          headers.setContentLength(photo.getFileSize());
          headers.setContentDispositionFormData("inline", photo.getFileName());
          headers.set("X-Photo-Id", Optional.ofNullable(photo.getId()).orElse(0L).toString());
          headers.set("X-Photo-Filename", photo.getFileName());
          // Převod data na RFC 1123 format
          Instant createdAt = photo.getCreatedAt().toInstant();
          String rfc1123Date = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.systemDefault())
              .format(createdAt);
          headers.set("X-Photo-Uploaded-At", rfc1123Date);

          try {
            return new ResponseEntity<>(blobToByteArray(photo.getData()), headers, HttpStatus.OK);
          } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
          }
        })
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(null));
  }

  /**
   * Získání metadat fotografie
   * GET /api/photo/info
   */
  @GetMapping("/info")
  public ResponseEntity<?> getPhotoInfo(@AuthenticationPrincipal AppUser appUser) {
    return photoService.getPhoto(appUser.getDbUser())
        .map(PhotoController::toResponseMap)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", "Fotografie nebyla nalezena")));
  }

  /**
   * Smazání fotografie
   * DELETE /api/photo
   */
  @DeleteMapping
  public ResponseEntity<?> deletePhoto(@AuthenticationPrincipal AppUser appUser) {
    boolean deleted = photoService.deletePhoto(appUser.getDbUser());

    if (deleted) {
      return ResponseEntity.ok(Map.of("message", "Fotografie byla úspěšně smazána"));
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("message", "Fotografie nebyla nalezena"));
    }
  }
}
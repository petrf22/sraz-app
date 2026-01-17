package cz.petrf.sraz.controller;

import cz.petrf.sraz.db.entity.TextContent;
import cz.petrf.sraz.security.AppUser;
import cz.petrf.sraz.service.TextContentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/text")
public class TextContentController {

  private final TextContentService textContentService;

  public TextContentController(TextContentService textContentService) {
    this.textContentService = textContentService;
  }

  private static Map<String, Object> toResponseMap(TextContent textContent) {
    Map<String, Object> response = new HashMap<>();

    response.put("id", textContent.getId());
    response.put("content", textContent.getContent());
    response.put("createdAt", textContent.getCreatedAt());
    response.put("updatedAt", textContent.getUpdatedAt());

    return response;
  }

  /**
   * Vytvoření nového textového záznamu
   * POST /api/text
   */
  @PostMapping
  public ResponseEntity<?> createTextContent(@RequestBody Map<String, String> request, @AuthenticationPrincipal AppUser appUser) {
    try {
      String content = request.get("content");
      TextContent textContent = textContentService.createOrUpdateTextContent(content, appUser);
      Map<String, Object> response = toResponseMap(textContent);

      response.put("message", "Textový záznam byl úspěšně uložen");

      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Aktualizace textového záznamu
   * PUT /api/text/{id}
   */
  @PutMapping("/{id}")
  public ResponseEntity<?> updateTextContent(@PathVariable Long id, @RequestBody Map<String, String> request, @AuthenticationPrincipal AppUser appUser) {
    try {
      String content = request.get("content");
      TextContent textContent = textContentService.updateTextContent(id, content, appUser);
      Map<String, Object> response = toResponseMap(textContent);

      response.put("message", "Textový záznam byl úspěšně aktualizován");

      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Získání konkrétního textového záznamu
   * GET /api/text/{id}
   */
  @GetMapping("/{id}")
  public ResponseEntity<?> getTextContent(@PathVariable Long id, @AuthenticationPrincipal AppUser appUser) {
    return textContentService.getTextContent(id, appUser)
        .map(TextContentController::toResponseMap)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", "Textový záznam s ID " + id + " nebyl nalezen")));
  }

  /**
   * Získání všech textových záznamů
   * GET /api/text
   */
  @GetMapping
  public ResponseEntity<?> getAllTextContents(@AuthenticationPrincipal AppUser appUser) {
    return textContentService.findByUser(appUser)
        .map(TextContentController::toResponseMap)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", "Uživatel nemá vytvořený text")));
  }

  /**
   * Smazání textového záznamu
   * DELETE /api/text/{id}
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteTextContent(@PathVariable Long id, @AuthenticationPrincipal AppUser appUser) {
    boolean deleted = textContentService.deleteTextContent(id, appUser);

    if (deleted) {
      return ResponseEntity.ok(Map.of("message", "Textový záznam byl úspěšně smazán"));
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("message", "Textový záznam s ID " + id + " nebyl nalezen"));
    }
  }
}
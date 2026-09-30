package cz.petrf.sraz.service;

import cz.petrf.sraz.config.FioProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Šifrování tajemství uložených v DB (token Fio API) – AES-256-GCM, výstup base64(IV ‖ šifrový text).
 * Klíč z app.fio.token-key; chybný klíč appku shodí hned při startu.
 */
@Component
public class TokenCipher {

  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;

  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public TokenCipher(FioProperties properties) {
    byte[] raw = properties.getTokenKey()!=null ? Base64.getDecoder().decode(properties.getTokenKey().trim()):new byte[0];
    if (raw.length!=32) {
      throw new IllegalStateException("app.fio.token-key musí být 32 bajtů v base64 (openssl rand -base64 32)");
    }
    this.key = new SecretKeySpec(raw, "AES");
  }

  public String encrypt(String plain) {
    try {
      byte[] iv = new byte[IV_BYTES];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Šifrování se nepodařilo", e);
    }
  }

  public String decrypt(String encoded) {
    try {
      byte[] data = Base64.getDecoder().decode(encoded);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
      return new String(cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalStateException("Token nejde dešifrovat – změnil se app.fio.token-key?", e);
    }
  }
}

package cz.petrf.sraz.service;

import cz.petrf.sraz.exception.DomainException;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.Locale;

/**
 * Normalizace a kontrola IBAN (délka, znaky, kontrolní součet mod 97).
 */
public final class Iban {

  private Iban() {
  }

  /** Vrátí IBAN bez mezer velkými písmeny, nebo null pro prázdný vstup; neplatný → DomainException. */
  public static String normalize(String raw) {
    String iban = StringUtils.deleteWhitespace(StringUtils.trimToEmpty(raw)).toUpperCase(Locale.ROOT);
    if (iban.isEmpty()) {
      return null;
    }
    if (!iban.matches("[A-Z]{2}\\d{2}[A-Z0-9]{10,30}") || !checksumOk(iban)) {
      throw new DomainException("Neplatný IBAN: " + raw);
    }
    return iban;
  }

  private static boolean checksumOk(String iban) {
    String rearranged = iban.substring(4) + iban.substring(0, 4);
    StringBuilder digits = new StringBuilder();
    for (char c : rearranged.toCharArray()) {
      digits.append(Character.isLetter(c) ? String.valueOf(c - 'A' + 10):String.valueOf(c));
    }
    return new BigInteger(digits.toString()).mod(BigInteger.valueOf(97)).intValue()==1;
  }
}

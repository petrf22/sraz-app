package cz.petrf.sraz.db.entity;

/**
 * Typ opakování období: WEEKLY = vybrané dny každý N-tý týden, MONTHLY = n-tý výskyt dne v měsíci
 * (např. 1. a 3. pátek, poslední pátek) každý N-tý měsíc.
 */
public enum Recurrence {
  WEEKLY, MONTHLY
}

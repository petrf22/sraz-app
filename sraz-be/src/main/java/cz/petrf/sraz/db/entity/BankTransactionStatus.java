package cz.petrf.sraz.db.entity;

/**
 * Stav staženého pohybu: spárovaný s platbou, čeká na ruční spárování, nebo se ignoruje (odchozí, cizí).
 */
public enum BankTransactionStatus {
  MATCHED, UNMATCHED, IGNORED
}

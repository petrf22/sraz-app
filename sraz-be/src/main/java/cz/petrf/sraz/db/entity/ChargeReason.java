package cz.petrf.sraz.db.entity;

/**
 * Za co se platí: odehraná akce, nebo pokuta (celý podíl) za pozdní odhlášení / neomluvenou neúčast.
 */
public enum ChargeReason {
  PLAYED, LATE_CANCEL, NO_SHOW
}

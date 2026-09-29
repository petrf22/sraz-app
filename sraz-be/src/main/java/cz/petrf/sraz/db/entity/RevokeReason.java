package cz.petrf.sraz.db.entity;

/**
 * Důvod zneplatnění refresh tokenu.
 */
public enum RevokeReason {
  REUSE_DETECTED, LOGOUT, BLOCKED
}

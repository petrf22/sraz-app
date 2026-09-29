package cz.petrf.sraz.db.entity;

/**
 * Stav členství ve skupině – INVITED čeká na souhlas pozvaného.
 */
public enum MembershipStatus {
  INVITED, ACTIVE, DECLINED, REMOVED
}

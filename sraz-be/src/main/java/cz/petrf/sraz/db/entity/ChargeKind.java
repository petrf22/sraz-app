package cz.petrf.sraz.db.entity;

/**
 * Podle čeho se počítala částka: stálý člen max(poplatek, podíl), náhradník podíl, brankář nic.
 */
public enum ChargeKind {
  REGULAR, SUBSTITUTE, GOALIE
}

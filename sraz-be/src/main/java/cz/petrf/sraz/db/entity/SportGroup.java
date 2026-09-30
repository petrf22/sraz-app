package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sport_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportGroup extends BaseEntity {

  @Column(nullable = false)
  private String name;

  private String description;

  /** Účet skupiny pro QR platby (IBAN). */
  private String iban;

  /** Pozdní odhlášení a neomluvená neúčast platí celý podíl. */
  private boolean finesEnabled;

  /** Token Fio API (jen čtení) zašifrovaný TokenCipher; null = párování plateb vypnuté. */
  private String fioTokenEnc;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime fioLastSyncAt;

  /** Chyba posledního stažení pohybů (null = v pořádku). */
  private String fioLastError;
}

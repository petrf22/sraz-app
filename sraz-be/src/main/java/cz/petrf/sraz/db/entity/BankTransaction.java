package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pohyb na účtu skupiny stažený z Fio API; příchozí platby se párují na Charge podle VS.
 */
@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransaction extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id")
  private SportGroup group;

  /** ID pohybu ve Fio (column22) – brání dvojímu uložení při opakovaném stažení. */
  @Column(nullable = false)
  private Long fioId;

  @Column(nullable = false)
  private LocalDate bookedOn;

  @Column(nullable = false)
  private BigDecimal amount;

  private String currency;
  private String variableSymbol;
  private String counterAccount;
  private String counterName;
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BankTransactionStatus status;

  /** Proč pohyb nešel spárovat (nebo přeplatek). */
  private String note;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "charge_id")
  private Charge charge;
}

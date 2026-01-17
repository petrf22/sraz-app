package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRefreshToken implements Persistable<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false,
      foreignKey = @ForeignKey(name = "fk_refresh_user"))
  private User user;

  @Column(name = "jti", nullable = false, unique = true, columnDefinition = "UUID")
  private UUID jti;

  @Column(name = "device", length = 100)
  private String device;

  @Column(name = "issued_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime issuedAt;

  @Column(name = "exp", nullable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime exp;

  @Builder.Default
  @Column(name = "revoked", nullable = false)
  private boolean revoked = false;

  @PrePersist
  protected void onCreate() {
    issuedAt = OffsetDateTime.now();
  }

  @Override
  public boolean isNew() {
    return id==null;
  }
}
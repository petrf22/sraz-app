package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;

@Entity
@Table(name = "text_contents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextContent implements Persistable<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false,
      foreignKey = @ForeignKey(name = "fk_text_content_user"))
  private User user;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime createdAt;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = OffsetDateTime.now();
    updatedAt = OffsetDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  @Override
  public boolean isNew() {
    return id==null;
  }
}
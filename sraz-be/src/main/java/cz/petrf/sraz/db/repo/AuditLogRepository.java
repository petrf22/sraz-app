package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  List<AuditLog> findByEntityAndEntityIdOrderByCreatedAtDesc(String entity, Long entityId);
}

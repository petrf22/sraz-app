package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.AuditLog;
import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.repo.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Záznam o tom, kdo co a kdy udělal (přihlášky, pozvánky, změny členství).
 */
@Service
@RequiredArgsConstructor
public class AuditService {

  private final AuditLogRepository auditRepo;

  public void log(User actor, String action, String entity, Long entityId, String detail) {
    auditRepo.save(AuditLog.builder()
        .actor(actor)
        .action(action)
        .entity(entity)
        .entityId(entityId)
        .detail(detail)
        .build());
  }
}

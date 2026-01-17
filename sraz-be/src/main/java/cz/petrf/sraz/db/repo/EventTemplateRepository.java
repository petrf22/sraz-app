package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.EventTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventTemplateRepository extends JpaRepository<EventTemplate, Long> {
  List<EventTemplate> findByOwnerId(Long ownerId);

  List<EventTemplate> findByOwnerIdAndActiveTrue(Long ownerId);

  List<EventTemplate> findByOwnerIdAndIsPublicTrue(Long ownerId);
}
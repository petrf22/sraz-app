package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabelRepository extends JpaRepository<Label, Long> {
  List<Label> findByOwnerId(Long ownerId);

  List<Label> findByOwnerIdAndGlobalTrue(Long ownerId);
}
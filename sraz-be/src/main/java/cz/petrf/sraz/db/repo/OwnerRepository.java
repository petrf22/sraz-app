package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerRepository extends JpaRepository<Owner, Long> {
}
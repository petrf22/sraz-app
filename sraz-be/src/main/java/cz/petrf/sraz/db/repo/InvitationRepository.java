package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

  Optional<Invitation> findByToken(String token);

  Optional<Invitation> findByEventIdAndUserId(Long eventId, Long userId);
}

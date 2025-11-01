package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
  List<Reminder> findByOwnerId(Long ownerId);
}
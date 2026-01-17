package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.ReminderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderEventRepository extends JpaRepository<ReminderEvent, Long> {
  List<ReminderEvent> findByOwnerId(Long ownerId);

  List<ReminderEvent> findByEventId(Long eventId);

  List<ReminderEvent> findByReminderLabelId(Long reminderLabelId);
}
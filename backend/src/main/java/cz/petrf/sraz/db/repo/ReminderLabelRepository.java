package cz.petrf.sraz.db.repo;
import cz.petrf.sraz.db.entity.ReminderLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReminderLabelRepository extends JpaRepository<ReminderLabel, Long> {
    List<ReminderLabel> findByOwnerId(Long ownerId);
    List<ReminderLabel> findByReminderId(Long reminderId);
}
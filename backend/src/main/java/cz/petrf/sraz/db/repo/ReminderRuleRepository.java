package cz.petrf.sraz.db.repo;
import cz.petrf.sraz.db.entity.ReminderRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReminderRuleRepository extends JpaRepository<ReminderRule, Long> {
    List<ReminderRule> findByOwnerId(Long ownerId);
    List<ReminderRule> findByReminderLabelId(Long reminderLabelId);
    List<ReminderRule> findByOwnerIdOrderByOrder(Long ownerId);
}
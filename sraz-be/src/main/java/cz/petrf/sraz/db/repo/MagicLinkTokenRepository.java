package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.MagicLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MagicLinkTokenRepository extends JpaRepository<MagicLinkToken, String> {

}
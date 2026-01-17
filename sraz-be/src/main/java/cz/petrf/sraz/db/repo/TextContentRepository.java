package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.TextContent;
import cz.petrf.sraz.db.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TextContentRepository extends JpaRepository<TextContent, Long> {
  Optional<TextContent> findByIdAndUser(Long id, User dbUser);

  Optional<TextContent> findByUser(User dbUser);

  boolean existsByIdAndUser(Long id, User dbUser);

  void deleteByIdAndUser(Long id, User dbUser);

  void deleteAllByUser(User appUser);

  void deleteByUser(User user);
}
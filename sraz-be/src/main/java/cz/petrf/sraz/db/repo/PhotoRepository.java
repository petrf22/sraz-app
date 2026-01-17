package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.Photo;
import cz.petrf.sraz.db.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

  void deleteAllByUser(User user);

  Optional<Photo> findByUser(User user);

  boolean existsByUser(User user);

  void deleteByUser(User user);
}
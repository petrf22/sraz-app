package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.repo.PhotoRepository;
import cz.petrf.sraz.db.repo.TextContentRepository;
import cz.petrf.sraz.db.repo.UserRefreshTokenRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepo;
  private final PhotoRepository photoRepo;
  private final TextContentRepository textContentRepo;
  private final UserRefreshTokenRepository userRefreshTokenRepo;

  @Transactional
  public void deleteUserAccount(User user) {
    userRefreshTokenRepo.deleteByUser(user);
    textContentRepo.deleteByUser(user);
    photoRepo.deleteByUser(user);

    userRepo.delete(user);
  }
}
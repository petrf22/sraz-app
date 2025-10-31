package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.OwnerUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OwnerUserRepository extends JpaRepository<OwnerUser, Long> {

    /* unikátní dvojice (owner, user) – pokud přidáš UK do DB */
    Optional<OwnerUser> findByOwnerIdAndUserId(Long ownerId, Long userId);

    /* všichni uživatelé daného ownera */
    List<OwnerUser> findByOwnerId(Long ownerId);

    /* všichni owneri daného uživatele */
    List<OwnerUser> findByUserId(Long userId);

    /* pouze aktivní záznamy */
    List<OwnerUser> findByOwnerIdAndActiveTrue(Long ownerId);

    /* admini v rámci ownera */
    List<OwnerUser> findByOwnerIdAndAdminTrue(Long ownerId);
}
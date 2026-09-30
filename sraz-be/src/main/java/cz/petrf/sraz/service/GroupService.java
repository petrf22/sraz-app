package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.*;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Skupiny, jejich týmy a místa konání.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {

  /** Výchozí týmy nové skupiny – lze přejmenovat nebo smazat. */
  private static final List<String[]> DEFAULT_TEAMS = List.of(
      new String[]{"Modří", "#1677ff"},
      new String[]{"Červení", "#f5222d"});

  private final SportGroupRepository groupRepo;
  private final TeamRepository teamRepo;
  private final VenueRepository venueRepo;
  private final GroupMemberRepository memberRepo;
  private final AccessService access;
  private final AuditService audit;

  @Transactional(readOnly = true)
  public List<SportGroup> myGroups(User user) {
    return access.isAdmin(user) ? groupRepo.findAll() : groupRepo.findActiveForUser(user.getId());
  }

  @Transactional(readOnly = true)
  public SportGroup get(Long groupId, User user) {
    access.requireReader(groupId, user);
    return find(groupId);
  }

  public SportGroup find(Long groupId) {
    return groupRepo.findById(groupId).orElseThrow(() -> new NotFoundException("Skupina", groupId));
  }

  /** Zakladatel se stává aktivním stálým členem a organizátorem. */
  public SportGroup create(String name, String description, User creator) {
    SportGroup group = groupRepo.save(SportGroup.builder()
        .name(requireText(name, "Název skupiny"))
        .description(StringUtils.trimToNull(description))
        .build());

    for (int i = 0; i < DEFAULT_TEAMS.size(); i++) {
      teamRepo.save(Team.builder().group(group).name(DEFAULT_TEAMS.get(i)[0]).color(DEFAULT_TEAMS.get(i)[1]).sortOrder(i).build());
    }

    memberRepo.save(GroupMember.builder()
        .group(group)
        .user(creator)
        .email(creator.getEmail())
        .memberType(MemberType.REGULAR)
        .position(Position.PLAYER)
        .organizer(true)
        .status(MembershipStatus.ACTIVE)
        .build());

    audit.log(creator, "GROUP_CREATE", "group", group.getId(), group.getName());
    return group;
  }

  public SportGroup update(Long groupId, String name, String description, String iban, User user) {
    access.requireOrganizer(groupId, user);
    SportGroup group = find(groupId);
    group.setName(requireText(name, "Název skupiny"));
    group.setDescription(StringUtils.trimToNull(description));
    group.setIban(Iban.normalize(iban));
    return group;
  }

  // ---- týmy ----

  @Transactional(readOnly = true)
  public List<Team> teams(Long groupId) {
    return teamRepo.findByGroupIdOrderBySortOrderAscIdAsc(groupId);
  }

  public Team saveTeam(Long groupId, Long teamId, String name, String color, Integer sortOrder, User user) {
    access.requireOrganizer(groupId, user);
    Team team = teamId==null
        ? Team.builder().group(find(groupId)).build()
        : teamRepo.findById(teamId).filter(t -> t.getGroup().getId().equals(groupId))
        .orElseThrow(() -> new NotFoundException("Tým", teamId));

    team.setName(requireText(name, "Název týmu"));
    team.setColor(StringUtils.trimToNull(color));
    team.setSortOrder(sortOrder!=null ? sortOrder:teams(groupId).size());
    return teamRepo.save(team);
  }

  public void deleteTeam(Long teamId, User user) {
    Team team = teamRepo.findById(teamId).orElseThrow(() -> new NotFoundException("Tým", teamId));
    access.requireOrganizer(team.getGroup().getId(), user);
    teamRepo.delete(team);
  }

  // ---- místa ----

  @Transactional(readOnly = true)
  public List<Venue> venues(Long groupId, User user) {
    access.requireReader(groupId, user);
    return venueRepo.findByGroupIdOrderByName(groupId);
  }

  public Venue saveVenue(Long groupId, Long venueId, String name, String address, String mapUrl,
                         BigDecimal latitude, BigDecimal longitude, User user) {
    access.requireOrganizer(groupId, user);
    Venue venue = venueId==null
        ? Venue.builder().group(find(groupId)).build()
        : findVenue(groupId, venueId);

    if (latitude!=null && (latitude.abs().compareTo(BigDecimal.valueOf(90)) > 0)
        || longitude!=null && (longitude.abs().compareTo(BigDecimal.valueOf(180)) > 0)) {
      throw new DomainException("GPS souřadnice jsou mimo rozsah.");
    }

    venue.setName(requireText(name, "Název místa"));
    venue.setAddress(StringUtils.trimToNull(address));
    venue.setMapUrl(StringUtils.trimToNull(mapUrl));
    venue.setLatitude(latitude);
    venue.setLongitude(longitude);
    return venueRepo.save(venue);
  }

  public void deleteVenue(Long venueId, User user) {
    Venue venue = venueRepo.findById(venueId).orElseThrow(() -> new NotFoundException("Místo", venueId));
    access.requireOrganizer(venue.getGroup().getId(), user);
    venueRepo.delete(venue);
  }

  public Venue findVenue(Long groupId, Long venueId) {
    return venueRepo.findById(venueId).filter(v -> v.getGroup().getId().equals(groupId))
        .orElseThrow(() -> new NotFoundException("Místo", venueId));
  }

  static String requireText(String value, String field) {
    String trimmed = StringUtils.trimToNull(value);
    if (trimmed==null) {
      throw new DomainException(field + " je povinný údaj.");
    }
    return trimmed;
  }
}

package cz.petrf.sraz.controller;

import cz.petrf.sraz.db.entity.BankTransaction;
import cz.petrf.sraz.db.entity.BankTransactionStatus;
import cz.petrf.sraz.db.entity.Charge;
import cz.petrf.sraz.db.entity.SportGroup;
import cz.petrf.sraz.security.CurrentUserService;
import cz.petrf.sraz.service.AccessService;
import cz.petrf.sraz.service.PaymentMatchingService;
import cz.petrf.sraz.service.PaymentMatchingService.SyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * GraphQL: napojení skupiny na Fio API a párování příchozích plateb. Token se nikdy nevrací.
 */
@Controller
@RequiredArgsConstructor
public class FioGraphqlController {

  private final CurrentUserService currentUser;
  private final AccessService access;
  private final PaymentMatchingService paymentMatching;

  @SchemaMapping(typeName = "SportGroup")
  public boolean fioConnected(SportGroup group) {
    return group.getFioTokenEnc()!=null;
  }

  @SchemaMapping(typeName = "SportGroup")
  public OffsetDateTime fioLastSyncAt(SportGroup group) {
    access.requireOrganizer(group.getId(), currentUser.requireUser());
    return group.getFioLastSyncAt();
  }

  @SchemaMapping(typeName = "SportGroup")
  public String fioLastError(SportGroup group) {
    access.requireOrganizer(group.getId(), currentUser.requireUser());
    return group.getFioLastError();
  }

  @SchemaMapping(typeName = "SportGroup")
  public List<BankTransaction> bankTransactions(SportGroup group, @Argument BankTransactionStatus status) {
    return paymentMatching.transactions(group.getId(), status, currentUser.requireUser());
  }

  @SchemaMapping(typeName = "SportGroup")
  public List<Charge> unpaidCharges(SportGroup group) {
    return paymentMatching.unpaidCharges(group.getId(), currentUser.requireUser());
  }

  @MutationMapping
  public SyncResult groupSetFioToken(@Argument Long groupId, @Argument String token) {
    return paymentMatching.setToken(groupId, token, currentUser.requireUser()).orElse(null);
  }

  @MutationMapping
  public SyncResult fioSync(@Argument Long groupId) {
    return paymentMatching.sync(groupId, currentUser.requireUser());
  }

  @MutationMapping
  public BankTransaction bankTransactionAssign(@Argument Long id, @Argument Long chargeId) {
    return paymentMatching.assign(id, chargeId, currentUser.requireUser());
  }

  @MutationMapping
  public BankTransaction bankTransactionIgnore(@Argument Long id) {
    return paymentMatching.ignore(id, currentUser.requireUser());
  }
}

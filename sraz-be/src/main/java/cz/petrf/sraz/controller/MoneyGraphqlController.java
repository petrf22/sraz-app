package cz.petrf.sraz.controller;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.security.CurrentUserService;
import cz.petrf.sraz.service.AccountingService;
import cz.petrf.sraz.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;

/**
 * GraphQL: vyúčtování akcí, platby a bank skupiny.
 */
@Controller
@RequiredArgsConstructor
public class MoneyGraphqlController {

  private final CurrentUserService currentUser;
  private final AccountingService accounting;
  private final BankService bank;

  @QueryMapping
  public List<Charge> myCharges() {
    return accounting.myCharges(currentUser.requireUser());
  }

  @SchemaMapping(typeName = "Event")
  public List<Charge> charges(Event event) {
    return accounting.eventCharges(event, currentUser.requireUser());
  }

  @SchemaMapping(typeName = "Charge")
  public String iban(Charge charge) {
    return charge.getEvent().getGroup().getIban();
  }

  @SchemaMapping(typeName = "SportGroup")
  public BigDecimal bankBalance(SportGroup group) {
    return bank.balance(group.getId(), currentUser.requireUser());
  }

  @SchemaMapping(typeName = "SportGroup")
  public List<BankEntry> bankEntries(SportGroup group) {
    return bank.entries(group.getId(), currentUser.requireUser());
  }

  @MutationMapping
  public Registration attendanceSet(@Argument Long eventId, @Argument Long userId, @Argument boolean attended) {
    return accounting.setAttendance(eventId, userId, attended, currentUser.requireUser());
  }

  @MutationMapping
  public Registration registrationSetExcused(@Argument Long eventId, @Argument Long userId, @Argument boolean excused) {
    return accounting.setExcused(eventId, userId, excused, currentUser.requireUser());
  }

  @MutationMapping
  public List<Charge> eventClose(@Argument Long id) {
    return accounting.close(id, currentUser.requireUser());
  }

  @MutationMapping
  public Event eventReopen(@Argument Long id) {
    return accounting.reopen(id, currentUser.requireUser());
  }

  @MutationMapping
  public Charge chargeSetPaid(@Argument Long id, @Argument boolean paid, @Argument PaymentMethod method) {
    return accounting.setPaid(id, paid, method, currentUser.requireUser());
  }

  @MutationMapping
  public BankEntry bankEntryAdd(@Argument Long groupId, @Argument Double amount, @Argument String description) {
    return bank.addManual(groupId, GraphqlInputs.money(amount), description, currentUser.requireUser());
  }

  @MutationMapping
  public boolean bankEntryDelete(@Argument Long id) {
    bank.deleteManual(id, currentUser.requireUser());
    return true;
  }
}

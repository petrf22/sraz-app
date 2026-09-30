package cz.petrf.sraz.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.petrf.sraz.config.FioProperties;
import cz.petrf.sraz.exception.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Klient Fio API: pohyby na účtu za období (GET /periods/{token}/{od}/{do}/transactions.json).
 * Token je součástí URL – do logu ani chybových hlášek se URL nikdy nedostane.
 * Fio povoluje jeden dotaz na token za 30 s (jinak HTTP 409).
 */
@Component
@Slf4j
public class FioClient {

  /** Pohyb z výpisu (jen sloupce, které potřebujeme). */
  public record Transaction(long fioId, LocalDate bookedOn, BigDecimal amount, String currency, String variableSymbol,
                            String counterAccount, String counterName, String message) {
  }

  private final FioProperties properties;
  private final RestClient restClient;

  public FioClient(FioProperties properties, RestClient.Builder restClientBuilder) {
    this.properties = properties;
    this.restClient = restClientBuilder.build();
  }

  public List<Transaction> transactions(String token, LocalDate from, LocalDate to) {
    Response response;
    try {
      response = restClient.get()
          .uri(properties.getBaseUrl() + "/periods/{token}/{from}/{to}/transactions.json", token, from, to)
          .retrieve()
          .body(Response.class);
    } catch (RestClientResponseException e) {
      log.warn("transactions :: Fio API vrátilo HTTP {}", e.getStatusCode().value());
      if (e.getStatusCode().value()==HttpStatus.CONFLICT.value()) {
        throw new DomainException("Fio API povoluje jeden dotaz za 30 sekund – zkus to za chvíli.");
      }
      throw new DomainException("Fio API odmítlo požadavek (HTTP %d) – zkontroluj token.".formatted(e.getStatusCode().value()));
    } catch (ResourceAccessException e) {
      // zpráva výjimky obsahuje URL s tokenem – logujeme jen příčinu
      log.warn("transactions :: Fio API nedostupné: {}", e.getCause()!=null ? e.getCause().toString():"?");
      throw new DomainException("Fio API je nedostupné – zkus to později.");
    }
    if (response==null || response.accountStatement()==null || response.accountStatement().transactionList()==null
        || response.accountStatement().transactionList().transaction()==null) {
      return List.of();
    }
    return response.accountStatement().transactionList().transaction().stream()
        .map(FioClient::toTransaction)
        .filter(Objects::nonNull)
        .toList();
  }

  private static Transaction toTransaction(Row r) {
    if (r.column22()==null || r.column0()==null || r.column1()==null) {
      return null;
    }
    String account = text(r.column2());
    String bank = text(r.column3());
    return new Transaction(
        number(r.column22()).longValue(),
        LocalDate.parse(text(r.column0()).substring(0, 10)),   // "2026-09-30+0200"
        number(r.column1()),
        text(r.column14()),
        text(r.column5()),
        account!=null && bank!=null ? account + "/" + bank:account,
        text(r.column10()),
        text(r.column16()));
  }

  private static String text(Column c) {
    if (c==null || c.value()==null) {
      return null;
    }
    String s = c.value().toString().trim();
    return s.isEmpty() ? null:s;
  }

  private static BigDecimal number(Column c) {
    return new BigDecimal(c.value().toString());
  }

  // ---- JSON výpisu ----

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Response(AccountStatement accountStatement) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record AccountStatement(TransactionList transactionList) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TransactionList(List<Row> transaction) {
  }

  /** column0 datum, 1 objem, 2 protiúčet, 3 kód banky, 5 VS, 10 název protiúčtu, 14 měna, 16 zpráva, 22 ID pohybu. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Row(Column column0, Column column1, Column column2, Column column3, Column column5, Column column10,
             Column column14, Column column16, Column column22) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Column(Object value) {
  }
}

package cz.petrf.sraz.service;

import cz.petrf.sraz.config.FioProperties;
import cz.petrf.sraz.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FioClientTest {

  private static final String URL = "https://fio.test/periods/TOKEN/2026-09-01/2026-09-30/transactions.json";

  /** Zkrácený výpis ve formátu Fio API (null sloupce, číselné i textové hodnoty). */
  private static final String STATEMENT = """
      {"accountStatement":{"info":{"accountId":"2900000000","currency":"CZK"},
       "transactionList":{"transaction":[
        {"column22":{"value":26500001,"name":"ID pohybu","id":22},
         "column0":{"value":"2026-09-28+0200","name":"Datum","id":0},
         "column1":{"value":320.0,"name":"Objem","id":1},
         "column14":{"value":"CZK","name":"Měna","id":14},
         "column2":{"value":"123456789","name":"Protiúčet","id":2},
         "column3":{"value":"0800","name":"Kód banky","id":3},
         "column10":{"value":"Novák Jan","name":"Název protiúčtu","id":10},
         "column5":{"value":"0042","name":"VS","id":5},
         "column16":{"value":"hokej","name":"Zpráva pro příjemce","id":16},
         "column7":null},
        {"column22":{"value":26500002,"name":"ID pohybu","id":22},
         "column0":{"value":"2026-09-29+0200","name":"Datum","id":0},
         "column1":{"value":-2800,"name":"Objem","id":1},
         "column14":{"value":"CZK","name":"Měna","id":14},
         "column5":null}
       ]}}}
      """;

  MockRestServiceServer server;
  FioClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    FioProperties properties = new FioProperties();
    properties.setBaseUrl("https://fio.test");
    client = new FioClient(properties, builder);
  }

  @Test
  void parsesTransactions() {
    server.expect(requestTo(URL)).andRespond(withSuccess(STATEMENT, MediaType.APPLICATION_JSON));

    List<FioClient.Transaction> result = client.transactions("TOKEN", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

    assertThat(result).hasSize(2);
    FioClient.Transaction in = result.getFirst();
    assertThat(in.fioId()).isEqualTo(26500001L);
    assertThat(in.bookedOn()).isEqualTo(LocalDate.of(2026, 9, 28));
    assertThat(in.amount()).isEqualByComparingTo("320");
    assertThat(in.variableSymbol()).isEqualTo("0042");
    assertThat(in.counterAccount()).isEqualTo("123456789/0800");
    assertThat(in.counterName()).isEqualTo("Novák Jan");
    assertThat(in.message()).isEqualTo("hokej");
    assertThat(result.get(1).amount()).isEqualByComparingTo("-2800");
    assertThat(result.get(1).variableSymbol()).isNull();
  }

  @Test
  void rateLimitGivesReadableErrorWithoutToken() {
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.CONFLICT));

    assertThatThrownBy(() -> client.transactions("TOKEN", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("30 sekund")
        .hasMessageNotContaining("TOKEN");
  }
}

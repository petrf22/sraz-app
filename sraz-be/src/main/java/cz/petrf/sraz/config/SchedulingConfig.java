package cz.petrf.sraz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Zapíná @Scheduled úlohy (rozesílání pozvánek, obnova blocklistu, čištění tokenů)
 * a @Retryable (opakované odeslání e-mailu).
 */
@Configuration
@EnableScheduling
@EnableResilientMethods
public class SchedulingConfig {

  /** Aktuální čas – v testech lze nahradit pevnými hodinami. */
  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}

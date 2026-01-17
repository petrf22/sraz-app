package cz.petrf.sraz.config;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQlScalarConfig {

  @Bean
  public RuntimeWiringConfigurer dateTimeScalars() {
    return wiringBuilder -> wiringBuilder
        .scalar(ExtendedScalars.Date)          // LocalDate
        .scalar(ExtendedScalars.DateTime)      // LocalDateTime
        .scalar(ExtendedScalars.Time);         // LocalTime
  }
}
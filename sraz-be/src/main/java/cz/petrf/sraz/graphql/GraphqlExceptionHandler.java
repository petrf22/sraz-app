package cz.petrf.sraz.graphql;

import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.NotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Převod výjimek na GraphQL chyby. Rozšíření {@code code} čte frontend
 * (UNAUTHENTICATED → přesměrování na přihlášení).
 */
@Component
@Slf4j
public class GraphqlExceptionHandler extends DataFetcherExceptionResolverAdapter {

  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    return switch (ex) {
      case AuthenticationException e -> error(env, ErrorType.UNAUTHORIZED, "UNAUTHENTICATED", "Přihlaste se prosím.");
      case AccessDeniedException e -> error(env, ErrorType.FORBIDDEN, "FORBIDDEN", message(e, "Přístup odepřen."));
      case NotFoundException e -> error(env, ErrorType.NOT_FOUND, "NOT_FOUND", e.getMessage());
      case DomainException e -> error(env, ErrorType.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
      default -> {
        log.error("GraphQL chyba v {}", env.getExecutionStepInfo().getPath(), ex);
        yield error(env, ErrorType.INTERNAL_ERROR, "INTERNAL_ERROR", "Neočekávaná chyba serveru.");
      }
    };
  }

  private static String message(Throwable ex, String fallback) {
    return ex.getMessage()!=null ? ex.getMessage():fallback;
  }

  private static GraphQLError error(DataFetchingEnvironment env, ErrorType type, String code, String message) {
    return GraphqlErrorBuilder.newError(env)
        .errorType(type)
        .message(message)
        .extensions(Map.of("code", code))
        .build();
  }
}

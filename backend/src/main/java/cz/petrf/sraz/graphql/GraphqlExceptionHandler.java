package cz.petrf.sraz.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class GraphqlExceptionHandler implements DataFetcherExceptionHandler {

  @Override
  public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(DataFetcherExceptionHandlerParameters handlerParameters) {
    Throwable ex = handlerParameters.getException();
    GraphQLError error;

    if (ex instanceof AccessDeniedException) {
      error = GraphqlErrorBuilder.newError()
          .message("Access denied")
          .errorType(ErrorType.UNAUTHORIZED)
          .path(handlerParameters.getPath())
          .location(handlerParameters.getSourceLocation())
          .build();
    } else {
      error = GraphqlErrorBuilder.newError()
          .message(ex.getMessage()!=null ? ex.getMessage():"Internal server error")
          .errorType(ErrorType.INTERNAL_ERROR)
          .path(handlerParameters.getPath())
          .location(handlerParameters.getSourceLocation())
          .build();
    }

    DataFetcherExceptionHandlerResult.newResult().error(GraphqlErrorBuilder.newError()
        .message(ex.getMessage()).build()).build();

    DataFetcherExceptionHandlerResult result = DataFetcherExceptionHandlerResult.newResult()
        .error(error)
        .build();

    return CompletableFuture.completedFuture(result);
  }

}
import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection, inject } from '@angular/core';
import { provideRouter, Router } from '@angular/router';

import { routes } from './app.routes';
import { provideNzIcons } from 'ng-zorro-antd/icon';
import { cs_CZ, provideNzI18n } from 'ng-zorro-antd/i18n';
import { registerLocaleData } from '@angular/common';
import cs from '@angular/common/locales/en';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideApollo } from 'apollo-angular';
import { HttpLink } from 'apollo-angular/http';
import { ApolloClient, ApolloLink, CombinedGraphQLErrors, CombinedProtocolErrors, InMemoryCache } from '@apollo/client';
import { AccountBookFill, AlertFill, AlertOutline } from '@ant-design/icons-angular/icons';
import { IconDefinition } from '@ant-design/icons-angular';
import { JwtInterceptor } from './service/jwt-interceptor';
import { environment } from '../environments/environment';
import { NzMessageService } from 'ng-zorro-antd/message';
import { ErrorLink, onError } from '@apollo/client/link/error';

const icons: IconDefinition[] = [AccountBookFill, AlertOutline, AlertFill];

registerLocaleData(cs);

export function apolloOptionsFactory(
  httpLink: HttpLink,
  router: Router,
  msg: NzMessageService
): ApolloClient.Options {
  // Log any GraphQL errors, protocol errors, or network error that occurred
  const errorLink = new ErrorLink(({ error, operation }) => {
    if (CombinedGraphQLErrors.is(error)) {
      // Kontrola na UNAUTHENTICATED chybu
      const isUnauthenticated = error.errors.some(
        e => e.extensions?.['code'] === 'UNAUTHENTICATED'
      );

      if (isUnauthenticated) {
        msg.warning('Přihlášení vypršelo.');
        router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
        return;
      }

      error.errors.forEach(({ message, locations, path }) =>
        console.log(
          `[GraphQL error]: Message: ${message}, Location: ${locations}, Path: ${path}`
        )
      );
    } else if (CombinedProtocolErrors.is(error)) {
      error.errors.forEach(({ message, extensions }) =>
        console.log(
          `[Protocol error]: Message: ${message}, Extensions: ${JSON.stringify(
            extensions
          )}`
        )
      );
    } else {
      console.error(`[Network error]: ${error}`);
    }
  });
  return {
    link: ApolloLink.from([errorLink, httpLink.create({ uri: environment.apiBaseUrl + '/graphql' })]),
    cache: new InMemoryCache(),
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes), provideNzIcons(icons), provideNzI18n(cs_CZ), provideAnimationsAsync(), provideHttpClient(), provideHttpClient(), provideApollo(() => {
      const httpLink = inject(HttpLink);
      const router = inject(Router);
      const msg = inject(NzMessageService);

      return apolloOptionsFactory(httpLink, router, msg);
    }),
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
    provideNzIcons(icons)
  ]
};

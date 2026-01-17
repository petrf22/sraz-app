import { ApplicationConfig, LOCALE_ID, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { icons } from './icons-provider';
import { provideNzIcons } from 'ng-zorro-antd/icon';
import { cs_CZ, provideNzI18n } from 'ng-zorro-antd/i18n';
import { registerLocaleData } from '@angular/common';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { tokenInterceptor } from './func/token-func';
import { authInitializer } from './func/auth-initializer';

import { inject } from '@angular/core';
import { Router } from '@angular/router';

import { provideApollo } from 'apollo-angular';
import { HttpLink } from 'apollo-angular/http';
import { ApolloClient, ApolloLink, CombinedGraphQLErrors, CombinedProtocolErrors, InMemoryCache } from '@apollo/client';
import { AccountBookFill, AlertFill, AlertOutline } from '@ant-design/icons-angular/icons';
import { IconDefinition } from '@ant-design/icons-angular';
import { NzMessageService } from 'ng-zorro-antd/message';
import { ErrorLink, onError } from '@apollo/client/link/error';

import cs from '@angular/common/locales/en';

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
    link: ApolloLink.from([errorLink, httpLink.create({ uri: '/graphql' })]),
    cache: new InMemoryCache(),
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes), provideNzIcons(icons),
    provideAnimations(),
    { provide: LOCALE_ID, useValue: 'cs-CZ' }, provideNzI18n(cs_CZ),
    provideHttpClient(withInterceptors([tokenInterceptor])),
    provideAppInitializer(authInitializer()),
    provideApollo(() => {
      const httpLink = inject(HttpLink);
      const router = inject(Router);
      const msg = inject(NzMessageService);

      return apolloOptionsFactory(httpLink, router, msg);
    }),

  ]
};

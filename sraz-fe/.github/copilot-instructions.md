# Sraz Application - AI Coding Instructions

## Project Overview
Sraz is an Angular 20 frontend application for managing event wishes and participants. Built with TypeScript, Angular Material (ng-zorro-antd), Apollo GraphQL, and RxJS, it features authentication, email verification, and user management workflows.

## Architecture & Key Patterns

### Authentication & Security
- **Token Management**: JWT tokens stored in `sessionStorage` managed via `UserService.tokenSig()` (Angular Signal)
- **Token Verification**: Email-based token verification flow (`/api/auth/verify/{emailToken}`)
- **HTTP Interceptor**: `tokenInterceptor()` in [src/app/func/token-func.ts](src/app/func/token-func.ts) automatically adds Bearer tokens to all HTTP requests
- **Auth Guard**: Use `authGuard` from [src/app/auth-guard.ts](src/app/auth-guard.ts) to protect routes - checks `userService.tokenSig() !== null`
- **Error Handling**: Apollo Client detects `UNAUTHENTICATED` GraphQL errors and redirects to login with return URL

### Service Architecture
- **Dependency Injection**: Use `inject()` from `@angular/core` to inject services (modern Angular pattern, not constructor parameters)
- **UserService** ([src/app/services/user-service.ts](src/app/services/user-service.ts)): Single source of truth for token state via Signal `tokenSig`
- **AuthService** ([src/app/service/auth.service.ts](src/app/service/auth.service.ts)): Handles login, token lifecycle, expiration checks
- **Signals & Reactive State**: Use `signal<T>()` and `computed()` for reactive state - avoid direct subscriptions in components when possible

### Component Patterns
- **Standalone Components**: All components use `imports: [...]` array (Angular 14+) - no NgModule declarations
- **SCSS Styling**: Configure in [angular.json](angular.json) with `"inlineStyleLanguage": "scss"`
- **Form Handling**: Use `NonNullableFormBuilder` (`nzb.control()`, `nzb.group()`) for null-safe reactive forms
- **Template Binding**: Use `computed()` for derived state, regular `signal()` for state mutations
- **ng-zorro Modules**: Import specific modules (`NzButtonModule`, `NzFormModule`, etc.) into component `imports`

### Data Flow & GraphQL
- **GraphQL Codegen**: Run `npm run codegen` to generate types from `.graphql` files in [src/app/graphql/](src/app/graphql/)
- **Generated Types Location**: Types auto-generate to matching `.generated.ts` files alongside `.graphql` definitions
- **Apollo Client Setup**: Configured in [src/app/app.config.ts](src/app/app.config.ts) with error handling, caching, and locale
- **REST Endpoints**: Mix of REST (`/api/auth/*`, `/api/text`) and GraphQL - prefer GraphQL for user data queries

### Routing & Lazy Loading
- **Route Definitions**: [src/app/app.routes.ts](src/app/app.routes.ts) uses standalone components with `loadComponent` for lazy loading
- **Route Titles**: Define via `title` property in route config - extracted dynamically by App component
- **Protected Routes**: Apply `canActivate: [authGuard]` to routes requiring authentication

### State Management
- **Signals as State**: Components use Angular Signals for reactive state (e.g., `textPrani = signal('')`, `fileList = signal<NzUploadFile[]>([])`)
- **Computed Values**: Use `computed(() => ...)` for derived state that auto-tracks dependencies
- **Signal Updates**: Use `.set()` for replacement, `.update()` for mutations

## Development Workflows

### Build & Run
```bash
npm start                          # Start dev server on http://localhost:4200
npm run build                      # Production build
npm run watch                      # Watch mode for development
npm test                           # Run Karma tests
```

### GraphQL Development
```bash
npm run codegen                    # Generate types from .graphql files (run after schema changes)
npm run codegen:watch              # Watch mode for codegen (useful during development)
```

### Code Formatting
- **Prettier Config**: [package.json](package.json) defines 100-char line width, single quotes, Angular HTML parser
- Auto-format on save if using Prettier integration in your editor

## Project Structure & Key Files
- `src/app/app.ts` - Root App component with routing & authentication state
- `src/app/app.routes.ts` - Route definitions with lazy-loaded components
- `src/app/app.config.ts` - Apollo GraphQL setup, error handling, i18n (Czech locale)
- `src/app/services/` - Core services (UserService for auth state, text content)
- `src/app/func/` - Utility functions (tokenInterceptor, authInitializer)
- `src/app/graphql/` - GraphQL queries/mutations and auto-generated types
- `src/app/models/` - TypeScript interfaces (Token, TextContent, PhotoInfo)

## Common Tasks & Examples

### Adding a Protected Route
1. Create standalone component with `imports: [...]`
2. Add to [src/app/app.routes.ts](src/app/app.routes.ts): `{ path: 'new', loadComponent: ..., canActivate: [authGuard] }`
3. Route title auto-extracted from route config in App component

### Adding a GraphQL Query
1. Create `.graphql` file in [src/app/graphql/](src/app/graphql/) (e.g., `user-profile.graphql`)
2. Run `npm run codegen` to generate `.generated.ts` with typed query class
3. Inject `Apollo` service and use generated query class in component

### Making Authenticated HTTP Requests
1. Service method returns `Observable<T>`
2. `tokenInterceptor()` automatically adds `Authorization: Bearer {token}` header
3. AuthService handles token validation & expiration

### Handling Form Submissions
- Use `NonNullableFormBuilder` with validators
- Check `.valid` before submitting
- Mark invalid controls dirty: `control.markAsDirty()`
- Subscribe to service method with `next/error` handlers

## Testing Notes
- Unit tests colocated with components (`.spec.ts` files)
- Use Karma test runner (`npm test`)
- Test auth flows with mocked `UserService` and `Router`

## Czech Language & Localization
- Locale set to Czech (`cs_CZ`) in [src/app/app.config.ts](src/app/app.config.ts)
- ng-zorro components automatically localized
- Content is in Czech (Úvod, Přání, O nás, etc.)

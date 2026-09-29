import { Routes } from '@angular/router';
import { authGuard } from './auth-guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: '/uvod' },
  { path: 'verify-token/:emailToken', title: 'Ověření e-mailu', loadComponent: () => import('./verify-token/verify-token.component').then(c => c.VerifyTokenComponent) },
  { path: 'uvod', title: 'Úvod', loadComponent: () => import('./uvod/uvod.component').then(c => c.UvodComponent) },
  { path: 'login', title: 'Přihlášení', loadComponent: () => import('./login/login.component').then(c => c.LoginComponent) },
  { path: 'logout', title: 'Odhlásit se', loadComponent: () => import('./logout/logout.component').then(c => c.LogoutComponent), canActivate: [authGuard] }
];

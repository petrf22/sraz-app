import { Routes } from '@angular/router';
import { authGuard } from './auth-guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: '/uvod' },
  { path: 'verify-token/:emailToken', title: 'Ověření e-mailu', loadComponent: () => import('./verify-token/verify-token.component').then(c => c.VerifyTokenComponent) },
  { path: 'uvod', title: 'Úvod', loadComponent: () => import('./uvod/uvod.component').then(c => c.UvodComponent) },
  { path: 'prani', title: 'Přání paní doktorce', loadComponent: () => import('./prani/prani.component').then(c => c.PraniComponent), canActivate: [authGuard] },
  { path: 'logout', title: 'Odhlásit se', loadComponent: () => import('./logout/logout.component').then(c => c.LogoutComponent), canActivate: [authGuard] },
  { path: 'onas', title: 'O nás ...', loadComponent: () => import('./onas/onas.component').then(c => c.ONasComponent), canActivate: [authGuard] }
];

import { Routes } from '@angular/router';
import { authGuard } from './auth-guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: '/uvod' },
  { path: 'uvod', title: 'Úvod', loadComponent: () => import('./uvod/uvod.component').then(c => c.UvodComponent) },
  // veřejné stránky z odkazů v e-mailu (bez přihlášení do aplikace)
  { path: 'prihlaska/:token', title: 'Přihláška na akci', loadComponent: () => import('./prihlaska/prihlaska.component').then(c => c.PrihlaskaComponent) },
  { path: 'pozvanka/:token', title: 'Pozvánka do skupiny', loadComponent: () => import('./pozvanka/pozvanka.component').then(c => c.PozvankaComponent) },
  { path: 'login', title: 'Přihlášení', loadComponent: () => import('./login/login.component').then(c => c.LoginComponent) },
  { path: 'logout', title: 'Odhlásit se', loadComponent: () => import('./logout/logout.component').then(c => c.LogoutComponent), canActivate: [authGuard] }
];

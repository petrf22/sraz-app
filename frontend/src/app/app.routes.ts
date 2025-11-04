import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { authChildGuard } from './guard/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: '/welcome' },
  { path: 'login', component: LoginComponent },
  {
    path: 'welcome',
    canActivate: [authChildGuard],
    loadChildren: () => import('./pages/welcome/welcome.routes').then(m => m.WELCOME_ROUTES)
  }
];


import { Routes } from '@angular/router';
import { MainLayoutComponent } from './core/layout/main-layout/main-layout';
import { PublicProfileComponent } from './features/public-profile/public-profile';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { InboxComponent } from './features/inbox/inbox/inbox.component';
import { HomeComponent } from './features/home/home.component';
import { AdminDashboardComponent } from './features/admin/admin-dashboard.component';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', component: HomeComponent, pathMatch: 'full', canActivate: [guestGuard] },
      { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
      { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },
      { path: 'inbox', component: InboxComponent, canActivate: [authGuard] },
      { path: 'admin', component: AdminDashboardComponent, canActivate: [adminGuard] },
      { path: ':slug', component: PublicProfileComponent }
    ]
  },
  { path: '**', redirectTo: '' }
];

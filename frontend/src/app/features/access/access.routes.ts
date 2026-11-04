import { Routes } from '@angular/router';
import { accessGuard } from './access.guard';
import { AccessAuditPageComponent } from './pages/access-audit.page';
import { AccessHomePageComponent } from './pages/access-home.page';
import { ConflictExceptionsPageComponent } from './pages/conflict-exceptions.page';
import { IdentityAdminPageComponent } from './pages/identity-admin.page';
import { LoginPageComponent } from './pages/login.page';

export const ACCESS_ROUTES: Routes = [
  {path: '', canActivate: [accessGuard], component: AccessHomePageComponent, title: 'Acceso y ámbitos'},
  {path: 'login', component: LoginPageComponent, title: 'Iniciar sesión'},
  {path: 'users', canActivate: [accessGuard], component: IdentityAdminPageComponent, title: 'Usuarios y asignaciones'},
  {path: 'conflicts', canActivate: [accessGuard], component: ConflictExceptionsPageComponent, title: 'Conflictos y excepciones'},
  {path: 'audit', canActivate: [accessGuard], component: AccessAuditPageComponent, title: 'Auditoría de acceso'},
  {path: '**', redirectTo: ''}
];

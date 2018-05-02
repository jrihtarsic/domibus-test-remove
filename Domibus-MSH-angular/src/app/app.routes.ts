import {RouterModule, Routes} from "@angular/router";
import {LoginComponent} from "./login/login.component";
import {AuthenticatedGuard} from "./guards/authenticated.guard";
import {ErrorLogComponent} from "./errorlog/errorlog.component";
import {PModeComponent} from "./pmode/pmode.component";
import {AuthorizedAdminGuard} from "./guards/authorized-admin.guard";
import {MessageFilterComponent} from "./messagefilter/messagefilter.component";
import {MessageLogComponent} from "./messagelog/messagelog.component";
import {UserComponent} from "./user/user.component";
import {TruststoreComponent} from "app/truststore/truststore.component";
import {JmsComponent} from "./jms/jms.component";
import {DirtyGuard} from "./common/dirty.guard";
import {AuditComponent} from "./audit/audit.component";
import {PartyComponent} from "./party/party.component";
import {AlertsComponent} from "./alerts/alerts.component";

const appRoutes: Routes = [
  {path: '', component: MessageLogComponent, canActivate: [AuthenticatedGuard]},
  {
    path: 'pmode',
    component: PModeComponent,
    canActivate: [AuthenticatedGuard, AuthorizedAdminGuard],
    canDeactivate: [DirtyGuard]
  },
  {
    path: 'jms',
    component: JmsComponent,
    canActivate: [AuthenticatedGuard, AuthorizedAdminGuard],
    canDeactivate: [DirtyGuard]
  },
  {
    path: 'messagefilter',
    component: MessageFilterComponent,
    canActivate: [AuthenticatedGuard, AuthorizedAdminGuard],
    canDeactivate: [DirtyGuard]
  },
  {path: 'truststore', component: TruststoreComponent, canActivate: [AuthenticatedGuard, AuthorizedAdminGuard]},
  {path: 'messagelog', component: MessageLogComponent, canActivate: [AuthenticatedGuard]},
  {
    path: 'user',
    component: UserComponent,
    canActivate: [AuthenticatedGuard, AuthorizedAdminGuard],
    canDeactivate: [DirtyGuard]
  },
  {path: 'errorlog', component: ErrorLogComponent, canActivate: [AuthenticatedGuard]},
  {path: 'login', component: LoginComponent},
  {path: 'audit', component: AuditComponent, canActivate: [AuthenticatedGuard, AuthorizedAdminGuard]},
  {path: 'party', component: PartyComponent, canActivate: [AuthenticatedGuard, AuthorizedAdminGuard]},
  {path: 'alerts', component: AlertsComponent, canActivate: [AuthenticatedGuard, AuthorizedAdminGuard]},
  {path: '**', component: MessageLogComponent, canActivate: [AuthenticatedGuard]}
];

export const routing = RouterModule.forRoot(appRoutes);

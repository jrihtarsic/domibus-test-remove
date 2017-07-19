import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import {FormsModule,ReactiveFormsModule} from '@angular/forms';
import {HttpModule, Http, XHRBackend, RequestOptions} from '@angular/http';
import {MaterialModule, MdSelectModule} from '@angular/material';
import {MdButtonModule} from '@angular/material';
import 'hammerjs';

import {NgxDatatableModule} from '@swimlane/ngx-datatable';
import {Md2Module, Md2SelectModule} from 'md2';

import {AppComponent} from './app.component';
import {LoginComponent} from "./login/login.component";
import {HomeComponent} from "./home/home.component";
import {PModeComponent} from "./pmode/pmode.component";

import {AuthenticatedGuard} from "./guards/authenticated.guard";
import {AuthorizedGuard} from "./guards/authorized.guard";
import {routing} from "./app.routes";
import {IsAuthorized} from "./security/is-authorized.directive";
import {ExtendedHttpClient} from "./http/extended-http-client";
import {HttpEventService} from "./http/http.event.service";
import {SecurityService} from "./security/security.service";
import {SecurityEventService} from "./security/security.event.service";
import {AlertComponent} from "./alert/alert.component";
import {AlertService} from "./alert/alert.service";
import {ErrorLogComponent} from "./errorlog/errorlog.component";
import {FooterComponent} from "./footer/footer.component";
import {DomibusInfoService} from "./appinfo/domibusinfo.service";
import {AuthorizedAdminGuard} from "./guards/authorized-admin.guard";
import {MessageFilterComponent} from "./messagefilter/messagefilter.component";
import {MessageLogComponent} from "./messagelog/messagelog.component";
import {UserComponent} from "./user/user.component"
import {TruststoreComponent} from './truststore/truststore.component';
import {PmodeUploadComponent} from './pmode-upload/pmode-upload.component';
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {MessagelogDialogComponent} from './messagelog/messagelog-dialog/messagelog-dialog.component';
import {JmsComponent} from './jms/jms.component';
import {RowLimiterComponent} from './common/row-limiter/row-limiter.component';
import {MoveDialogComponent} from "./jms/move-dialog/move-dialog.component";
import {MessagefilterDialogComponent} from "./messagefilter/messagefilter-dialog/messagefilter-dialog.component";
import {CancelMessagefilterDialogComponent} from "./messagefilter/cancelmessagefilter-dialog/cancelmessagefilter-dialog.component";
import {MessageDialogComponent} from './jms/message-dialog/message-dialog.component';
import { PasswordComponent } from './user/password/password-dialog.component';
import {DatePipe} from './customDate/datePipe';
import { DefaultPasswordDialogComponent } from './security/default-password-dialog/default-password-dialog.component';
import {EditMessageFilterComponent} from "./messagefilter/editmessagefilter-form/editmessagefilter-form.component";

export function extendedHttpClientFactory(xhrBackend: XHRBackend, requestOptions: RequestOptions, httpEventService: HttpEventService) {
  return new ExtendedHttpClient(xhrBackend, requestOptions, httpEventService);
}

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    HomeComponent,
    MessageFilterComponent,
    MessageLogComponent,
    UserComponent,
    ErrorLogComponent,
    AlertComponent,
    FooterComponent,
    PModeComponent,
    IsAuthorized,
    TruststoreComponent,
    PModeComponent,
    PmodeUploadComponent,
    MessagefilterDialogComponent,
    PmodeUploadComponent,
    MessagelogDialogComponent,
    CancelMessagefilterDialogComponent,
    JmsComponent,
    RowLimiterComponent,
    MoveDialogComponent,
    MessageDialogComponent,
    MessagefilterDialogComponent,
    PasswordComponent,
    DatePipe,
    DefaultPasswordDialogComponent,
    EditMessageFilterComponent
  ],
  entryComponents: [
    AppComponent,
    PmodeUploadComponent,
    MessagefilterDialogComponent,
    PmodeUploadComponent,
    MessagelogDialogComponent,
    MoveDialogComponent,
    MessageDialogComponent,
    CancelMessagefilterDialogComponent,
    MoveDialogComponent,
    PasswordComponent,
    DefaultPasswordDialogComponent,
    EditMessageFilterComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpModule,
    NgxDatatableModule,
    MaterialModule,
    MdButtonModule,
    MdSelectModule,
    Md2Module.forRoot(),
    routing,
    ReactiveFormsModule,
    Md2SelectModule
  ],
  providers: [
    AuthenticatedGuard,
    AuthorizedGuard,
    AuthorizedAdminGuard,
    HttpEventService,
    SecurityService,
    SecurityEventService,
    DomibusInfoService,
    AlertService,
    {
      provide: Http,
      useFactory: extendedHttpClientFactory,
      deps: [XHRBackend, RequestOptions, HttpEventService],
      multi: false
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}

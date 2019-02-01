import {BrowserModule} from "@angular/platform-browser";
import {ErrorHandler, NgModule} from '@angular/core';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {Http, HttpModule, RequestOptions, XHRBackend} from "@angular/http";
import {
  MdButtonModule,
  MdButtonToggleModule,
  MdCheckboxModule,
  MdDialogModule,
  MdExpansionModule,
  MdIconModule,
  MdInputModule,
  MdListModule,
  MdMenuModule,
  MdSelectModule,
  MdSidenavModule,
  MdTooltipModule
} from '@angular/material';
import "hammerjs";

import {NgxDatatableModule} from "@swimlane/ngx-datatable";
import {Md2Module, Md2SelectModule} from "md2";

import {AppComponent} from "./app.component";
import {LoginComponent} from "./security/login/login.component";
import {CurrentPModeComponent} from "./pmode/current/currentPMode.component";
import {PModeArchiveComponent} from "./pmode/archive/pmodeArchive.component";

import {AuthenticatedGuard} from "./common/guards/authenticated.guard";
import {AuthorizedGuard} from "./common/guards/authorized.guard";
import {routing} from "./app.routes";
import {IsAuthorized} from "./security/is-authorized.directive";
import {ExtendedHttpClient} from "./common/http/extended-http-client";
import {HttpEventService} from "./common/http/http.event.service";
import {SecurityService} from "./security/security.service";
import {SecurityEventService} from "./security/security.event.service";
import {DomainService} from "./security/domain.service";
import {AlertComponent} from "./common/alert/alert.component";
import {AlertService} from "./common/alert/alert.service";
import {ErrorLogComponent} from "./errorlog/errorlog.component";
import {FooterComponent} from "./common/footer/footer.component";
import {DomibusInfoService} from "./common/appinfo/domibusinfo.service";
import {AuthorizedAdminGuard} from "./common/guards/authorized-admin.guard";
import {MessageFilterComponent} from "./messagefilter/messagefilter.component";
import {MessageLogComponent} from "./messagelog/messagelog.component";
import {UserComponent} from "./user/user.component";
import {TruststoreComponent} from "./truststore/truststore.component";
import {PmodeUploadComponent} from "./pmode/pmode-upload/pmode-upload.component";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {MessagelogDialogComponent} from "./messagelog/messagelog-dialog/messagelog-dialog.component";
import {JmsComponent} from "./jms/jms.component";
import {RowLimiterComponent} from "./common/row-limiter/row-limiter.component";
import {MoveDialogComponent} from "./jms/move-dialog/move-dialog.component";
import {MessageDialogComponent} from "./jms/message-dialog/message-dialog.component";
import {DatePipe} from "./common/customDate/datePipe";
import {CapitalizeFirstPipe} from "./common/capitalizefirst.pipe";
import {DefaultPasswordDialogComponent} from "./security/default-password-dialog/default-password-dialog.component";
import {MessagelogDetailsComponent} from "./messagelog/messagelog-details/messagelog-details.component";
import {ErrorlogDetailsComponent} from "./errorlog/errorlog-details/errorlog-details.component";
import {EditMessageFilterComponent} from "./messagefilter/editmessagefilter-form/editmessagefilter-form.component";
import {CancelDialogComponent} from "./common/cancel-dialog/cancel-dialog.component";
import {DirtyGuard} from "./common/guards/dirty.guard";
import {EditUserComponent} from "app/user/edituser-form/edituser-form.component";
import {SaveDialogComponent} from "./common/save-dialog/save-dialog.component";
import {TruststoreDialogComponent} from "./truststore/truststore-dialog/truststore-dialog.component";
import {TrustStoreUploadComponent} from "./truststore/truststore-upload/truststore-upload.component";
import {ColumnPickerComponent} from "./common/column-picker/column-picker.component";
import {PageHelperComponent} from "./common/page-helper/page-helper.component";
import {SharedModule} from "./common/module/SharedModule";
import {RestoreDialogComponent} from "./pmode/restore-dialog/restore-dialog.component";
import {ActionDirtyDialogComponent} from "./pmode/action-dirty-dialog/action-dirty-dialog.component";
import {AuditComponent} from "./audit/audit.component";
import {PartyComponent} from "./party/party.component";
import {PartyDetailsComponent} from "./party/party-details/party-details.component";
import {ClearInvalidDirective} from "./common/customDate/clearInvalid.directive";
import {PageHeaderComponent} from "./common/page-header/page-header.component";
import {DomainSelectorComponent} from "./common/domain-selector/domain-selector.component";
import {PmodeViewComponent} from './pmode/archive/pmode-view/pmode-view.component';
import {AlertsComponent} from "./alerts/alerts.component";
import {TestServiceComponent} from "./testservice/testservice.component";
import {PluginUserComponent} from './pluginuser/pluginuser.component';
import {EditbasicpluginuserFormComponent} from './pluginuser/editpluginuser-form/editbasicpluginuser-form.component';
import {EditcertificatepluginuserFormComponent} from './pluginuser/editpluginuser-form/editcertificatepluginuser-form.component';
import {PartyIdentifierDetailsComponent} from './party/party-identifier-details/party-identifier-details.component';
import {GlobalErrorHandler} from './common/global.error-handler';
import {UserService} from './user/user.service';
import {UserValidatorService} from './user/uservalidator.service';
import {DefaultPasswordGuard} from './security/defaultPassword.guard';
import {SanitizeHtmlPipe} from './common/sanitizeHtml.pipe';
import {LoggingComponent} from './logging/logging.component';
import {ChangePasswordComponent} from './security/change-password/change-password.component';
import {FilterableListComponent} from './common/filterable-list.component';
import {AuthExternalProviderGuard} from "./common/guards/auth-external-provider.guard";
import {LogoutAuthExtProviderComponent} from "./security/logout/logout.components";

export function extendedHttpClientFactory(xhrBackend: XHRBackend, requestOptions: RequestOptions, httpEventService: HttpEventService) {
  return new ExtendedHttpClient(xhrBackend, requestOptions, httpEventService);
}

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    MessageFilterComponent,
    MessageLogComponent,
    UserComponent,
    ErrorLogComponent,
    AlertComponent,
    FooterComponent,
    CurrentPModeComponent,
    PModeArchiveComponent,
    IsAuthorized,
    TruststoreComponent,
    PmodeUploadComponent,
    PmodeViewComponent,
    SaveDialogComponent,
    MessagelogDialogComponent,
    CancelDialogComponent,
    JmsComponent,
    RowLimiterComponent,
    MoveDialogComponent,
    MessageDialogComponent,
    DatePipe,
    CapitalizeFirstPipe,
    SanitizeHtmlPipe,
    DefaultPasswordDialogComponent,
    EditMessageFilterComponent,
    MessagelogDetailsComponent,
    ErrorlogDetailsComponent,
    EditUserComponent,
    TruststoreDialogComponent,
    TrustStoreUploadComponent,
    ColumnPickerComponent,
    TrustStoreUploadComponent,
    PageHelperComponent,
    RestoreDialogComponent,
    ActionDirtyDialogComponent,
    AuditComponent,
    PartyComponent,
    PartyDetailsComponent,
    ClearInvalidDirective,
    PageHeaderComponent,
    DomainSelectorComponent,
    AlertsComponent,
    TestServiceComponent,
    PluginUserComponent,
    EditbasicpluginuserFormComponent,
    EditcertificatepluginuserFormComponent,
    PartyIdentifierDetailsComponent,
    LoggingComponent,
    ChangePasswordComponent,
    FilterableListComponent,
    LogoutAuthExtProviderComponent
  ],
  entryComponents: [
    AppComponent,
    PmodeUploadComponent,
    PmodeViewComponent,
    MessagelogDialogComponent,
    MoveDialogComponent,
    MessageDialogComponent,
    MessagelogDetailsComponent,
    CancelDialogComponent,
    SaveDialogComponent,
    DefaultPasswordDialogComponent,
    EditMessageFilterComponent,
    ErrorlogDetailsComponent,
    EditUserComponent,
    TruststoreDialogComponent,
    TrustStoreUploadComponent,
    RestoreDialogComponent,
    ActionDirtyDialogComponent,
    PartyDetailsComponent,
    EditbasicpluginuserFormComponent,
    EditcertificatepluginuserFormComponent,
    PartyIdentifierDetailsComponent,
    ChangePasswordComponent,
    FilterableListComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpModule,
    NgxDatatableModule,
    MdButtonModule,
    MdDialogModule,
    MdTooltipModule,
    MdMenuModule,
    MdInputModule,
    MdIconModule,
    MdListModule,
    MdSidenavModule,
    MdSelectModule,
    routing,
    ReactiveFormsModule,
    Md2Module,
    Md2SelectModule,
    SharedModule,
    MdExpansionModule,
    MdCheckboxModule,
    MdButtonToggleModule,
    MdTooltipModule
  ],
  providers: [
    AuthenticatedGuard,
    AuthorizedGuard,
    AuthorizedAdminGuard,
    DirtyGuard,
    DefaultPasswordGuard,
    AuthExternalProviderGuard,
    HttpEventService,
    SecurityService,
    SecurityEventService,
    DomainService,
    DomibusInfoService,
    AlertService,
    {
      provide: Http,
      useFactory: extendedHttpClientFactory,
      deps: [XHRBackend, RequestOptions, HttpEventService],
      multi: false
    },
    {
      provide: ErrorHandler,
      useClass: GlobalErrorHandler
    },
    UserService,
    UserValidatorService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}

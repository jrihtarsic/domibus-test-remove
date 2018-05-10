import {Component, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {UserResponseRO, UserState} from './user';
import {UserService} from './user.service';
import {MdDialog, MdDialogRef} from '@angular/material';
import {UserValidatorService} from 'app/user/uservalidator.service';
import {AlertService} from '../alert/alert.service';
import {EditUserComponent} from 'app/user/edituser-form/edituser-form.component';
import {isNullOrUndefined} from 'util';
import {Headers, Http} from '@angular/http';
import {DirtyOperations} from '../common/dirty-operations';
import {CancelDialogComponent} from '../common/cancel-dialog/cancel-dialog.component';
import {SaveDialogComponent} from '../common/save-dialog/save-dialog.component';
import {ColumnPickerBase} from '../common/column-picker/column-picker-base';
import {RowLimiterBase} from '../common/row-limiter/row-limiter-base';
import {SecurityService} from '../security/security.service';
import {DownloadService} from '../download/download.service';
import {AlertComponent} from '../alert/alert.component';
import {SettingsService} from '../security/settings.service';

@Component({
  moduleId: module.id,
  templateUrl: 'user.component.html',
  providers: [UserService, UserValidatorService],
  styleUrls: ['./user.component.css']
})


export class UserComponent implements OnInit, DirtyOperations {

  @ViewChild('passwordTpl') passwordTpl: TemplateRef<any>;
  @ViewChild('editableTpl') editableTpl: TemplateRef<any>;
  @ViewChild('checkBoxTpl') checkBoxTpl: TemplateRef<any>;
  @ViewChild('rowActions') rowActions: TemplateRef<any>;

  columnPicker: ColumnPickerBase = new ColumnPickerBase();
  rowLimiter: RowLimiterBase = new RowLimiterBase();

  users: Array<UserResponseRO> = [];
  userRoles: Array<String> = [];
  userDomains: Array<String> = [];

  selected = [];

  enableCancel = false;
  enableSave = false;
  enableDelete = false;
  enableEdit = false;

  multiDomain = false;

  rowNumber = -1;

  editedUser: UserResponseRO;
  test: boolean = false;

  static readonly USER_URL: string = 'rest/user';
  static readonly USER_USERS_URL: string = UserComponent.USER_URL + '/users';
  static readonly USER_CSV_URL: string = UserComponent.USER_URL + '/csv';

  constructor (private http: Http,
               private userService: UserService,
               public dialog: MdDialog,
               private userValidatorService: UserValidatorService,
               private alertService: AlertService,
               private securityService: SecurityService,
               private settingsService: SettingsService) {
  }

  ngOnInit (): void {
    this.multiDomain = this.settingsService.isMultiDomain();

    this.columnPicker.allColumns = [
      {
        cellTemplate: this.editableTpl,
        name: 'Username',
        prop: 'userName',
        canAutoResize: true
      },
      {
        cellTemplate: this.editableTpl,
        name: 'Email',
        prop: 'email',
        canAutoResize: true
      },
      {
        cellTemplate: this.editableTpl,
        name: 'Role(s)',
        prop: 'roles',
        canAutoResize: true
      },
      {
        cellTemplate: this.editableTpl,
        name: 'Domain',
        prop: 'domain',
        canAutoResize: true
      },
      {
        cellTemplate: this.passwordTpl,
        name: 'Password',
        prop: 'password',
        canAutoResize: true,
        sortable: false
      },
      {
        cellTemplate: this.checkBoxTpl,
        name: 'Active',
        canAutoResize: true
      },
      {
        cellTemplate: this.rowActions,
        name: 'Actions',
        width: 60,
        canAutoResize: true,
        sortable: false
      }
    ];

    this.columnPicker.allColumns = this.columnPicker.allColumns.filter(col => this.multiDomain || col.name !== 'Domain');

    this.columnPicker.selectedColumns = this.columnPicker.allColumns.filter(col => {
      return ['Username', 'Role(s)', 'Domain', 'Password', 'Active', 'Actions'].indexOf(col.name) !== -1
    });

    this.getUsers();

    this.getUserRoles();

    this.getUserDomains();

    if (this.users.length > AlertComponent.MAX_COUNT_CSV) {
      this.alertService.error('Maximum number of rows reached for downloading CSV');
    }

  }

  getUsers (): void {
    this.userService.getUsers().subscribe(users => this.users = users);
  }

  getUserRoles (): void {
    this.userService.getUserRoles().subscribe(userroles => this.userRoles = userroles);
  }

  getUserDomains (): void {
    this.userService.getUserDomains().subscribe(userdomains => this.userDomains = userdomains);
  }

  onSelect ({selected}) {
    //console.log('Select Event', selected, this.selected);

    if (isNullOrUndefined(selected) || selected.length == 0) {
      // unselect
      this.enableDelete = false;
      this.enableEdit = false;

      return;
    }

    // select
    this.rowNumber = this.selected[0].$$index;

    this.selected.splice(0, this.selected.length);
    this.selected.push(...selected);
    this.enableDelete = selected.length > 0;
    this.enableEdit = selected.length == 1;
  }

  private isLoggedInUserSelected (selected): boolean {
    for (let entry of selected) {
      if (this.securityService.getCurrentUser().username === entry.userName) {
        return true;
      }
    }
    return false;
  }

  buttonNew (): void {
    this.editedUser = new UserResponseRO('', '', '', true, UserState[UserState.NEW], [], false, null);
    this.users.push(this.editedUser);
    this.users = this.users.slice();
    this.rowNumber = this.users.length - 1;
    const formRef: MdDialogRef<EditUserComponent> = this.dialog.open(EditUserComponent, {
      data: {
        edit: false,
        user: this.users[this.rowNumber],
        userroles: this.userRoles,
        userdomains: this.userDomains
      }
    });
    formRef.afterClosed().subscribe(result => {
      if (result == true) {
        this.updateUsername(formRef.componentInstance.userName);
        this.updateEmail(formRef.componentInstance.email);
        this.updateRoles(formRef.componentInstance.roles.toString());
        this.updatePassword(formRef.componentInstance.password);
        this.updateActive(formRef.componentInstance.active);
        if (UserState[UserState.PERSISTED] === this.users[this.rowNumber].status) {
          this.users[this.rowNumber].status = UserState[UserState.UPDATED]
        }

        this.enableSave = formRef.componentInstance.userForm.dirty;
        this.enableCancel = formRef.componentInstance.userForm.dirty;
      } else {
        this.users.pop();
        this.selected = [];
        this.enableEdit = false;
        this.enableDelete = false;
      }
    });
  }

  buttonEdit () {
    this.buttonEditAction(this.rowNumber);
  }

  buttonEditAction (rowNumber) {
    const formRef: MdDialogRef<EditUserComponent> = this.dialog.open(EditUserComponent, {
      data: {
        edit: true,
        user: this.users[rowNumber],
        userroles: this.userRoles,
        userdomains: this.userDomains
      }
    });
    formRef.afterClosed().subscribe(result => {
      if (result == true) {
        //this.updateUsername(formRef.componentInstance.userName);
        this.updateEmail(formRef.componentInstance.email);
        this.updateRoles(formRef.componentInstance.roles.toString());
        this.updatePassword(formRef.componentInstance.password);
        this.updateActive(formRef.componentInstance.active);
        if (UserState[UserState.PERSISTED] === this.users[this.rowNumber].status) {
          this.users[this.rowNumber].status = UserState[UserState.UPDATED]
        }

        this.enableSave = formRef.componentInstance.userForm.dirty;
        this.enableCancel = formRef.componentInstance.userForm.dirty;
      }
    });
  }

  private updateUsername (value: string) {
    this.users[this.rowNumber].userName = value;
  }

  private updateEmail (value: string) {
    this.users[this.rowNumber].email = value;
  }

  private updateRoles (value: string) {
    this.users[this.rowNumber].roles = value;
  }

  private updatePassword (value: string) {
    this.users[this.rowNumber].password = value;
  }

  private updateActive (value: boolean) {
    this.users[this.rowNumber].active = value;
  }

  buttonDelete () {
    if (this.isLoggedInUserSelected(this.selected)) {
      this.alertService.error('You cannot delete the logged in user: ' + this.securityService.getCurrentUser().username);
      return;
    }

    this.enableCancel = true;
    this.enableSave = true;
    this.enableDelete = false;
    this.enableEdit = false;

    // we need to use the old for loop approach to don't mess with the entries on the top before
    for (let i = this.selected.length - 1; i >= 0; i--) {
      this.users.splice(this.selected[i].$$index, 1);
    }

    this.selected = [];
  }

  buttonDeleteAction (row) {
    if (this.securityService.getCurrentUser().username === row.userName) {
      this.alertService.error('You cannot delete the logged in user: ' + this.securityService.getCurrentUser().username);
      return;
    }

    this.enableCancel = true;
    this.enableSave = true;
    this.enableDelete = false;
    this.enableEdit = false;

    // we need to use the old for loop approach to don't mess with the entries on the top before
    this.users.splice(row.$$index, 1);

    this.selected = [];
  }

  private disableSelectionAndButtons () {
    this.selected = [];
    this.enableCancel = false;
    this.enableSave = false;
    this.enableEdit = false;
    this.enableDelete = false;
  }

  cancelDialog () {
    let dialogRef = this.dialog.open(CancelDialogComponent);
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.disableSelectionAndButtons();
        this.users = [];
        this.getUsers();
      }
    });
  }

  saveDialog (withDownloadCSV: boolean) {
    let headers = new Headers({'Content-Type': 'application/json'});
    let dialogRef = this.dialog.open(SaveDialogComponent);
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.disableSelectionAndButtons();
        this.http.put(UserComponent.USER_USERS_URL, JSON.stringify(this.users), {headers: headers}).subscribe(res => {
          this.getUsers();
          this.getUserRoles();
          this.alertService.success('The operation \'update users\' completed successfully.', false);
          if (withDownloadCSV) {
            DownloadService.downloadNative(UserComponent.USER_CSV_URL);
          }
        }, err => {
          this.getUsers();
          this.getUserRoles();
          this.alertService.error('The operation \'update users\' not completed successfully.', false);
        });
      } else {
        if (withDownloadCSV) {
          DownloadService.downloadNative(UserComponent.USER_CSV_URL);
        }
      }
    });
  }

  /**
   * Method that checks if CSV Button export can be enabled
   * @returns {boolean} true, if button can be enabled; and false, otherwise
   */
  isSaveAsCSVButtonEnabled (): boolean {
    return this.users.length < AlertComponent.MAX_COUNT_CSV;
  }

  /**
   * Saves the content of the datatable into a CSV file
   */
  saveAsCSV () {
    if (this.isDirty()) {
      this.saveDialog(true);
    } else {
      DownloadService.downloadNative(UserComponent.USER_CSV_URL);
    }
  }

  isDirty (): boolean {
    return this.enableCancel;
  }

  changePageSize (newPageLimit: number) {
    this.rowLimiter.pageSize = newPageLimit;
    this.getUsers();
  }

}

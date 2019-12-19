import {ChangeDetectorRef, Component, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {UserResponseRO, UserState} from './user';
import {UserSearchCriteria, UserService} from './user.service';
import {MAT_CHECKBOX_CLICK_ACTION, MatDialog, MatDialogRef} from '@angular/material';
import {UserValidatorService} from 'app/user/uservalidator.service';
import {AlertService} from '../common/alert/alert.service';
import {EditUserComponent} from 'app/user/edituser-form/edituser-form.component';
import {HttpClient} from '@angular/common/http';
import {DirtyOperations} from '../common/dirty-operations';
import {SecurityService} from '../security/security.service';
import {DomainService} from '../security/domain.service';
import {Domain} from '../security/domain';
import {DialogsService} from '../common/dialogs/dialogs.service';
import mix from '../common/mixins/mixin.utils';
import BaseListComponent from '../common/mixins/base-list.component';
import FilterableListMixin from '../common/mixins/filterable-list.mixin';
import ModifiableListMixin from '../common/mixins/modifiable-list.mixin';
import {ClientPageableListMixin} from '../common/mixins/pageable-list.mixin';

@Component({
  moduleId: module.id,
  templateUrl: 'user.component.html',
  styleUrls: ['./user.component.css'],
  providers: [
    {provide: MAT_CHECKBOX_CLICK_ACTION, useValue: 'check'}
  ]
})

export class UserComponent extends mix(BaseListComponent)
  .with(FilterableListMixin, ModifiableListMixin, ClientPageableListMixin)
  implements OnInit, DirtyOperations {

  static readonly USER_URL: string = 'rest/user';
  static readonly USER_USERS_URL: string = UserComponent.USER_URL + '/users';
  static readonly USER_CSV_URL: string = UserComponent.USER_URL + '/csv';

  @ViewChild('passwordTpl', {static: false}) passwordTpl: TemplateRef<any>;
  @ViewChild('editableTpl', {static: false}) editableTpl: TemplateRef<any>;
  @ViewChild('checkBoxTpl', {static: false}) checkBoxTpl: TemplateRef<any>;
  @ViewChild('deletedTpl', {static: false}) deletedTpl: TemplateRef<any>;
  @ViewChild('rowActions', {static: false}) rowActions: TemplateRef<any>;

  userRoles: Array<String>;
  domains: Domain[];
  domainsPromise: Promise<Domain[]>;
  currentDomain: Domain;

  enableCancel: boolean;
  enableSave: boolean;
  enableDelete: boolean;
  enableEdit: boolean;

  currentUser: UserResponseRO;

  editedUser: UserResponseRO;

  areRowsDeleted: boolean;

  deletedStatuses: any[];

  constructor(private http: HttpClient,
              private userService: UserService,
              public dialog: MatDialog,
              private dialogsService: DialogsService,
              private userValidatorService: UserValidatorService,
              private alertService: AlertService,
              private securityService: SecurityService,
              private domainService: DomainService,
              private changeDetector: ChangeDetectorRef) {
    super();
  }

  async ngOnInit() {
    super.ngOnInit();

    super.filter = new UserSearchCriteria();
    this.deletedStatuses = [null, true, false];

    this.userRoles = [];

    this.enableCancel = false;
    this.enableSave = false;
    this.enableDelete = false;
    this.enableEdit = false;
    this.currentUser = null;
    this.editedUser = null;

    this.domainService.getCurrentDomain().subscribe((domain: Domain) => this.currentDomain = domain);

    this.getUserRoles();

    this.areRowsDeleted = false;

    this.filterData();
  }

  public get name(): string {
    return 'Users';
  }

  async ngAfterViewInit() {
    this.columnPicker.allColumns = [
      {
        cellTemplate: this.editableTpl,
        name: 'Username',
        prop: 'userName',
        canAutoResize: true
      },
      {
        cellTemplate: this.editableTpl,
        name: 'Role',
        prop: 'roles',
        canAutoResize: true
      },
      {
        cellTemplate: this.editableTpl,
        name: 'Email',
        prop: 'email',
        canAutoResize: true
      },
      {
        cellTemplate: this.passwordTpl,
        name: 'Password',
        prop: 'password',
        canAutoResize: true,
        sortable: false,
        width: 25
      },
      {
        cellTemplate: this.checkBoxTpl,
        name: 'Active',
        canAutoResize: true,
        width: 25
      },
      {
        cellTemplate: this.deletedTpl,
        name: 'Deleted',
        canAutoResize: true,
        width: 25
      },
      {
        cellTemplate: this.rowActions,
        name: 'Actions',
        width: 60,
        canAutoResize: true,
        sortable: false
      }
    ];

    const showDomain = await this.userService.isDomainVisible();
    if (showDomain) {
      this.getUserDomains();

      this.columnPicker.allColumns.splice(2, 0,
        {
          cellTemplate: this.editableTpl,
          name: 'Domain',
          prop: 'domainName',
          canAutoResize: true
        });
    }

    this.columnPicker.selectedColumns = this.columnPicker.allColumns.filter(col => {
      return ['Username', 'Role', 'Domain', 'Active', 'Deleted', 'Actions'].indexOf(col.name) !== -1
    });
  }

  ngAfterViewChecked() {
    this.changeDetector.detectChanges();
  }

  public async getDataAndSetResults(): Promise<any> {
    return this.getUsers();
  }

  async getUsers(): Promise<any> {
    return this.userService.getUsers(this.activeFilter).toPromise().then(async results => {
      const showDomain = await this.userService.isDomainVisible();
      if (showDomain) {
        await this.getUserDomains();
        results.forEach(user => this.setDomainName(user));
      }
      super.rows = results;
      super.count = results.length;
      this.areRowsDeleted = false;
      this.disableSelectionAndButtons();
    });
  }

  private setDomainName(user) {
    const domains = this.domains;
    if (domains) {
      const domain = domains.find(d => d.code == user.domain);
      if (domain) {
        user.domainName = domain.name;
      }
    }
  }

  getUserRoles(): void {
    this.userService.getUserRoles().subscribe(userroles => this.userRoles = userroles);
  }

  async getUserDomains(): Promise<Domain[]> {
    if (this.domainsPromise) {
      return this.domainsPromise;
    }
    this.domainsPromise = this.domainService.getDomains();
    this.domains = await this.domainsPromise;
    return this.domains;
  }

  onSelect({selected}) {
    if (!selected || selected.length == 0) {
      // unselect
      this.enableDelete = false;
      this.enableEdit = false;

      return;
    }

    // select
    this.currentUser = this.selected[0];
    this.editedUser = this.currentUser;

    this.enableDelete = selected.length > 0 && !selected.every(el => el.deleted);
    this.enableEdit = selected.length == 1 && !selected[0].deleted;
  }

  private isLoggedInUserSelected(selected): boolean {
    let currentUser = this.securityService.getCurrentUser();
    for (let entry of selected) {
      if (currentUser && currentUser.username === entry.userName) {
        return true;
      }
    }
    return false;
  }

  add(): void {
    if (this.isLoading) return;

    this.setPage(this.getLastPage());

    this.editedUser = new UserResponseRO('', this.currentDomain, '', '', true, UserState[UserState.NEW], [], false, false);
    this.setIsDirty();
    const formRef: MatDialogRef<EditUserComponent> = this.dialog.open(EditUserComponent, {
      data: {
        edit: false,
        user: this.editedUser,
        userroles: this.userRoles,
        userdomains: this.domains
      }
    });
    formRef.afterClosed().subscribe(ok => {
      if (ok) {
        this.onSaveEditForm(formRef);
        super.rows = [...this.rows, this.editedUser];
        super.count = this.count + 1;
        this.currentUser = this.editedUser;
      } else {
        super.selected = [];
        this.enableEdit = false;
        this.enableDelete = false;
      }
      this.setIsDirty();
    });
  }

  edit() {
    if (this.currentUser && this.currentUser.deleted) {
      this.alertService.error('You cannot edit a deleted user.', false, 5000);
      return;
    }
    this.buttonEditAction(this.currentUser);
  }

  buttonEditAction(currentUser) {
    if (this.isLoading) return;

    const formRef: MatDialogRef<EditUserComponent> = this.dialog.open(EditUserComponent, {
      data: {
        edit: true,
        user: currentUser,
        userroles: this.userRoles,
        userdomains: this.domains
      }
    });
    formRef.afterClosed().subscribe(ok => {
      if (ok) {
        this.onSaveEditForm(formRef);
        this.setIsDirty();
      }
    });
  }

  private onSaveEditForm(formRef: MatDialogRef<EditUserComponent>) {
    const editForm = formRef.componentInstance;
    const user = this.editedUser;
    if (!user) return;

    user.userName = editForm.userName || user.userName; // only for add
    user.email = editForm.email;
    user.roles = editForm.role;
    user.domain = editForm.domain;
    this.setDomainName(user);
    user.password = editForm.password;
    user.active = editForm.active;

    if (editForm.userForm.dirty) {
      if (UserState[UserState.PERSISTED] === user.status) {
        user.status = UserState[UserState.UPDATED]
      }
    }
  }

  setIsDirty() {
    super.isChanged = this.areRowsDeleted
      || this.rows.filter(el => el.status !== UserState[UserState.PERSISTED]).length > 0;

    this.enableSave = this.isChanged;
    this.enableCancel = this.isChanged;
  }

  delete() {
    this.deleteUsers(this.selected);
  }

  buttonDeleteAction(row) {
    this.deleteUsers([row]);
  }

  private deleteUsers(users: UserResponseRO[]) {
    if (this.isLoggedInUserSelected(users)) {
      this.alertService.error('You cannot delete the logged in user: ' + this.securityService.getCurrentUser().username);
      return;
    }

    this.enableDelete = false;
    this.enableEdit = false;

    for (const itemToDelete of users) {
      if (itemToDelete.status === UserState[UserState.NEW]) {
        this.rows.splice(this.rows.indexOf(itemToDelete), 1);
      } else {
        itemToDelete.status = UserState[UserState.REMOVED];
        itemToDelete.deleted = true;
      }
    }

    super.selected = [];
    this.areRowsDeleted = true;
    this.setIsDirty();
  }

  private disableSelectionAndButtons() {
    super.selected = [];
    this.enableCancel = false;
    this.enableSave = false;
    this.enableEdit = false;
    this.enableDelete = false;
  }

  public async doSave(): Promise<any> {
    const isValid = this.userValidatorService.validateUsers(this.rows);
    if (!isValid) return false; // TODO throw instead

    const modifiedUsers = this.rows.filter(el => el.status !== UserState[UserState.PERSISTED]);
    return this.http.put(UserComponent.USER_USERS_URL, modifiedUsers).toPromise().then(() => {
      this.loadServerData();
      // this.disableSelectionAndButtons();
    });
  }

  public get csvUrl(): string {
    return UserComponent.USER_CSV_URL;
  }

  isDirty(): boolean {
    return this.enableCancel;
  }

  setPage(offset: number): void {
    super.offset = offset;
  }

  getLastPage(): number {
    if (!this.rows || !this.rowLimiter || !this.rowLimiter.pageSize)
      return 0;
    return Math.floor(this.rows.length / this.rowLimiter.pageSize);
  }

  // onActivate(event) {
  //   if ('dblclick' === event.type) {
  //     this.edit();
  //   }
  // }

  setState() {
    this.filter.deleted_notSet = this.filter.i++ % 3 === 1;
    if (this.filter.deleted_notSet) {
      this.filter.deleted = true;
    }
  }

  canCancel() {
    return this.enableCancel;
  }

  canSave() {
    return this.enableSave && !this.isLoading;
  }

  canAdd() {
    return !this.isLoading;
  }

  canEdit() {
    return this.enableEdit && !this.isLoading;
  }

  canDelete() {
    return this.enableDelete && !this.isLoading;
  }
}

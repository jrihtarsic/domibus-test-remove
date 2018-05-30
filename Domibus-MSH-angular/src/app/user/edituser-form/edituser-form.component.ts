import {Component, Inject, ChangeDetectorRef} from '@angular/core';
import {AbstractControl, FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import {MD_DIALOG_DATA, MdDialogRef} from '@angular/material';
import {UserValidatorService} from '../uservalidator.service';
import {SecurityService} from '../../security/security.service';
import {UserService} from '../user.service';
import {Domain} from '../../security/domain';

const ROLE_AP_ADMIN = SecurityService.ROLE_AP_ADMIN;
const NEW_MODE = 'New User';
const EDIT_MODE = 'User Edit';

@Component({
  selector: 'edituser-form',
  templateUrl: 'edituser-form.component.html',
  providers: [UserService, UserValidatorService]
})

export class EditUserComponent {

  existingRoles = [];
  existingDomains = [];

  password: any;
  confirmation: any;
  userName = '';
  email = '';
  active = true;
  roles: string[] = [];
  oldRoles = [];
  domain: string;

  public emailPattern = '[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}';
  public passwordPattern = '^(?=.*[A-Z])(?=.*[ !#$%&\'()*+,-./:;<=>?@\\[^_`{|}~\\\]"])(?=.*[0-9])(?=.*[a-z]).{8,32}$';

  editMode: boolean;
  canChangePassword: boolean;
  isDomainVisible: boolean;
  domainTitle: string;

  formTitle: string = EDIT_MODE;

  userForm: FormGroup;

  constructor (public dialogRef: MdDialogRef<EditUserComponent>,
               @Inject(MD_DIALOG_DATA) public data: any,
               fb: FormBuilder,
               userValidatorService: UserValidatorService,
               private userService: UserService,
               private securityService: SecurityService,
               private cdr: ChangeDetectorRef) {

    this.isDomainVisible = this.userService.isDomainVisible();

    this.existingRoles = data.userroles;
    this.existingDomains = data.userdomains;

    this.editMode = data.edit;
    this.userName = data.user.userName;
    this.email = data.user.email;
    this.domain = data.user.domain;
    if (data.user.roles) {
      this.roles = data.user.roles.split(',');
      this.oldRoles = this.roles;
    }
    this.password = data.user.password;
    this.confirmation = data.user.password;
    this.active = data.user.active;

    this.canChangePassword = securityService.isCurrentUserSuperAdmin()
      || (securityService.isCurrentUserAdmin() && this.isCurrentUser());

    this.domainTitle = this.isSuperAdmin() ? 'Preferred Domain' : 'Domain';

    if (this.editMode) {
      this.existingRoles = this.getAllowedRoles(data.userroles, data.user.roles);

      this.userForm = fb.group({
        'userName': new FormControl({value: this.userName, disabled: true}, Validators.nullValidator),
        'email': [null, Validators.pattern],
        'roles': new FormControl(this.roles, Validators.required),
        'domain': this.isDomainVisible ? new FormControl({value: this.domain}, Validators.required) : null,
        'password': [null, Validators.pattern],
        'confirmation': [null],
        'active': new FormControl({value: this.active, disabled: this.isCurrentUser()}, Validators.required)
      }, {
        validator: userValidatorService.validateForm()
      });
    } else {
      this.formTitle = NEW_MODE;
      this.userForm = fb.group({
        'userName': new FormControl(this.userName, Validators.required),
        'email': [null, Validators.pattern],
        'roles': new FormControl(this.roles, Validators.required),
        'domain': this.isDomainVisible ? new FormControl({value: this.domain}, Validators.required) : null,
        'password': [Validators.required, Validators.pattern],
        'confirmation': [Validators.required],
        'active': [Validators.required]
      }, {
        validator: userValidatorService.validateForm()
      });
    }
  }

  updateUserName (event) {
    this.userName = event.target.value;
  }

  updateEmail (event) {
    this.email = event.target.value;
  }

  updatePassword (event) {
    this.password = event.target.value;
  }

  updateConfirmation (event) {
    this.confirmation = event.target.value;
  }

  updateActive (event) {
    this.active = event.target.checked;
  }

  submitForm () {
    this.dialogRef.close(true);
  }

  isCurrentUser (): boolean {
    return this.securityService.getCurrentUser().username === this.userName;
  }

  isSuperAdmin () {
    return this.roles.includes(SecurityService.ROLE_AP_ADMIN);
  }

  isDomainDisabled () {
    // if the edited user is not super-user
    return !this.isSuperAdmin();
  }

  // filters out roles so that the user cannot change from ap admin to the other 2 roles or vice-versa
  getAllowedRoles(allRoles, userRoles) {
    //console.log(allRoles, userRoles);
    if(userRoles.includes(SecurityService.ROLE_AP_ADMIN))
      return [SecurityService.ROLE_AP_ADMIN];
    else
      return allRoles.filter(role => role != SecurityService.ROLE_AP_ADMIN);
  }

  onRolesChanged () {
  }
}

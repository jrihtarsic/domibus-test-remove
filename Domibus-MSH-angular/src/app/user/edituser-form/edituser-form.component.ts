import {Component, Inject} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import {MD_DIALOG_DATA, MdDialogRef} from '@angular/material';
import {UserValidatorService} from '../uservalidator.service';
import {SecurityService} from '../../security/security.service';

let NEW_MODE = 'New User';
let EDIT_MODE = 'User Edit';

@Component({
  selector: 'edituser-form',
  templateUrl: 'edituser-form.component.html',
  providers: [UserValidatorService]
})

export class EditUserComponent {

  existingRoles = [];
  existingDomains = [];

  password: any;
  confirmation: any;
  userName = '';
  email = '';
  active = true;
  roles = [];
  domain: string;

  public emailPattern = '[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}';
  public passwordPattern = '^(?=.*[A-Z])(?=.*[ !#$%&\'()*+,-./:;<=>?@\\[^_`{|}~\\\]"])(?=.*[0-9])(?=.*[a-z]).{8,32}$';

  editMode: boolean;

  formTitle: string = EDIT_MODE;

  userForm: FormGroup;

  constructor (public dialogRef: MdDialogRef<EditUserComponent>,
               @Inject(MD_DIALOG_DATA) public data: any,
               fb: FormBuilder,
               userValidatorService: UserValidatorService,
               private securityService: SecurityService) {

    this.existingRoles = data.userroles;
    this.existingDomains = data.userdomains;

    this.editMode = data.edit;
    this.userName = data.user.userName;
    this.email = data.user.email;
    if (data.user.roles !== '') {
      this.roles = data.user.roles.split(',');
    }
    this.password = data.user.password;
    this.confirmation = data.user.password;
    this.active = data.user.active;

    if (this.editMode) {
      this.userForm = fb.group({
        'userName': new FormControl({value: this.userName, disabled: true}, Validators.nullValidator),
        'email': [null, Validators.pattern],
        'roles': new FormControl(this.roles, Validators.required),
        'domain': new FormControl(this.domain, Validators.required),
        'password': [null, Validators.pattern],
        'confirmation': [null],
        'active': new FormControl({value: this.active, disabled: this.isCurrentUser()}, Validators.required)
      }, {
        validator: userValidatorService.matchPassword
      });
    } else {
      this.formTitle = NEW_MODE;
      this.userForm = fb.group({
        'userName': new FormControl(this.userName, Validators.required),
        'email': [null, Validators.pattern],
        'roles': new FormControl(this.roles, Validators.required),
        'domain': new FormControl(this.domain, Validators.required),
        'password': [Validators.required, Validators.pattern],
        'confirmation': [Validators.required],
        'active': [Validators.required]
      }, {
        validator: userValidatorService.matchPassword
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

}

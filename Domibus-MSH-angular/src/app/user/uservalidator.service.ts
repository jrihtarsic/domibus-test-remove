import {UserResponseRO, UserState} from './user';
import {AlertService} from '../alert/alert.service';
import {Injectable} from '@angular/core';
import {AbstractControl} from '@angular/forms';

/**
 * Created by dussath on 6/20/17.
 */
@Injectable()
export class UserValidatorService {

  constructor (private alertService: AlertService) {
  }

  validateUsers (modifiedUsers: UserResponseRO[], users: UserResponseRO[]): boolean {
    let errorMessage: string = '';
    if (modifiedUsers.length == 0) {
      return false;
    }
    errorMessage = errorMessage.concat(this.checkUserNameDuplication(users));
    for (let u in modifiedUsers) {
      let user: UserResponseRO = modifiedUsers[u];
      let number = users.indexOf(user) + 1;
      if (user.status === UserState[UserState.NEW]) {
        errorMessage = errorMessage.concat(this.validateNewUsers(user, number));
      }
      else if (user.status === UserState[UserState.UPDATED]) {
        errorMessage = errorMessage.concat(this.validateRoles(user, number));
        errorMessage = errorMessage.concat(this.validateEmail(user, number));
      }
    }
    return this.triggerValidation(errorMessage);
  }


  validateNewUsers (user: UserResponseRO, number): string {
    let errorMessage: string = '';
    if (user.userName == null || user.userName.trim() === '') {
      errorMessage = errorMessage.concat('User ' + number + ' has no username defined\n');
    }
    errorMessage = errorMessage.concat(this.validateRoles(user, number));
    errorMessage = errorMessage.concat(this.validateEmail(user, number));
    if (user.password == null || user.password.trim() === '') {
      errorMessage = errorMessage.concat('User ' + number + ' has no password defined\n');
    }
    return errorMessage;
  }

  checkUserNameDuplication (allUsers: UserResponseRO[]) {
    let errorMessage: string = '';
    let seen = new Set();
    allUsers.every(function (user) {
      if (seen.size === seen.add(user.userName).size) {
        errorMessage = errorMessage.concat('Duplicate user name with user ' + allUsers.indexOf(user) + ' ');
        return false;
      }
      return true;
    });
    return errorMessage;
  }

  triggerValidation (errorMessage: string): boolean {
    if (errorMessage.trim()) {
      this.alertService.clearAlert();
      this.alertService.error(errorMessage);
      return false;
    }
    return true;
  }

  validateRoles (user: UserResponseRO, number): string {
    let errorMessage: string = '';
    if (user.roles == null || user.roles.trim() === '') {
      errorMessage = errorMessage.concat('User ' + number + ' has no role defined\n');
    }
    return errorMessage;
  }

  validateEmail (user: UserResponseRO, number): string {
    let email: string = user.email;
    let EMAIL_REGEXP = /^[a-z0-9!#$%&'*+\/=?^_`{|}~.-]+@[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*$/i;

    if (email != '' && (email.length <= 5 || !EMAIL_REGEXP.test(email))) {
      return 'incorrectMailFormat for user ' + number;
    }
    return '';
  }

  matchPassword (form: AbstractControl) {
    const password = form.get('password').value; // to get value in input tag
    const confirmPassword = form.get('confirmation').value; // to get value in input tag
    if (password !== confirmPassword) {
      form.get('confirmation').setErrors({confirmation: true})
    }
  }

  validateDomain (form: AbstractControl) {
    //console.log('validateDomain');
    const domain: string = form.get('domain').value;
    //console.log('domain:', domain);
    if (!domain) {
      form.get('domain').setErrors({required: true})
    }

    // const roles: string[] = form.get('roles').value;
    // if (!roles.includes('ROLE_AP_ADMIN')) {
    //   const domain: string = form.get('domain').value;
    //   if (!domain) {
    //     form.get('domain').setErrors({required: true})
    //   }
    // }
  }

  validateForm (isDomainVisible: boolean) {
    return (form: AbstractControl) => {
      if (isDomainVisible) {
        this.validateDomain(form);
      }
      this.matchPassword(form);
    }
  }

  // validateForm2 (form: AbstractControl, isDomainVisible: boolean) {
  //   if (isDomainVisible) {
  //     this.validateDomain(form);
  //   }
  //   this.matchPassword(form);
  // }

  // validateForm (form: AbstractControl) {
  //   UserValidatorService.validateDomain(form);
  //   UserValidatorService.matchPassword(form);
  // }
}

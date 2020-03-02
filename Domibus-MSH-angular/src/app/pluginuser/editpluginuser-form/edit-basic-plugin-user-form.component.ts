import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';
import {SecurityService} from '../../security/security.service';
import {EditPluginUserFormBaseComponent} from './edit-plugin-user-form-base.component';

@Component({
  selector: 'editbasicpluginuser-form',
  templateUrl: './edit-basic-plugin-user-form.component.html',
})

export class EditBasicPluginUserFormComponent extends EditPluginUserFormBaseComponent implements OnInit {

  passwordConfirmation: string;
  public passwordPattern: string;
  public passwordValidationMessage: string;

  constructor(public dialogRef: MatDialogRef<EditBasicPluginUserFormComponent>,
              @Inject(MAT_DIALOG_DATA) public data: any,
              private securityService: SecurityService) {

    super(dialogRef, data);

    this.passwordConfirmation = data.user.password;
  }

  async ngOnInit() {
    const passwordPolicy = await this.securityService.getPluginPasswordPolicy();
    this.passwordPattern = passwordPolicy.pattern;
    this.passwordValidationMessage = passwordPolicy.validationMessage;
  }

}

import {Component, Inject, OnInit} from '@angular/core';
import {NgControl, NgForm} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';
import {UserValidatorService} from '../../user/support/uservalidator.service';
import {PluginUserRO} from '../support/pluginuser';
import {PluginUserService} from '../support/pluginuser.service';
import {SecurityService} from '../../security/security.service';
import {UserState} from '../../user/support/user';

const NEW_MODE = 'New Plugin User';
const EDIT_MODE = 'Plugin User Edit';

@Component({
  template: '',
})

export class EditPluginUserFormBaseComponent implements OnInit {

  existingRoles = [];
  editMode: boolean;
  formTitle: string;
  user: PluginUserRO;

  public originalUserPattern = PluginUserService.originalUserPattern;
  public originalUserMessage = PluginUserService.originalUserMessage;

  constructor(public dialogRef: MatDialogRef<EditPluginUserFormBaseComponent>,
              @Inject(MAT_DIALOG_DATA) public data: any) {

    this.existingRoles = data.userroles;
    this.user = data.user;
    this.editMode = this.user.status !== UserState[UserState.NEW];
    this.formTitle = this.editMode ? EDIT_MODE : NEW_MODE;

  }

  async ngOnInit() {
  }

  submitForm(editForm: NgForm) {
    if (editForm.invalid) {
      return;
    }
    this.dialogRef.close(true);
  }

  shouldShowErrors(field: NgControl | NgForm): boolean {
    return (field.touched || field.dirty) && !!field.errors;
  }

  isFormDisabled(editForm: NgForm) {
    return editForm.invalid || !editForm.dirty;
  }

}

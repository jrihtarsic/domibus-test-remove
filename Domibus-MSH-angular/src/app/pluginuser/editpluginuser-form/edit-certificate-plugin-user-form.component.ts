import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';
import {UserValidatorService} from '../../user/support/uservalidator.service';
import {PluginUserService} from '../support/pluginuser.service';
import {EditPluginUserFormBaseComponent} from './edit-plugin-user-form-base.component';

@Component({
  selector: 'editcertificatepluginuser-form',
  templateUrl: './edit-certificate-plugin-user-form.component.html',
  providers: [UserValidatorService]
})
export class EditCertificatePluginUserFormComponent extends EditPluginUserFormBaseComponent {

  public certificateIdPattern = PluginUserService.certificateIdPattern;
  public certificateIdMessage = PluginUserService.certificateIdMessage;

  constructor(public dialogRef: MatDialogRef<EditCertificatePluginUserFormComponent>,
              @Inject(MAT_DIALOG_DATA) public data: any) {
    super(dialogRef, data);
  }

}

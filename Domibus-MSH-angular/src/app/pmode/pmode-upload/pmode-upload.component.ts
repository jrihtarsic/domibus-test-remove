import {Component, Inject, OnInit, ViewChild} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material';
import {HttpClient} from '@angular/common/http';
import {AlertService} from '../../common/alert/alert.service';
import {PropertiesService} from '../../properties/properties.service';
import {FileUploadValidatorService} from '../../common/file-upload-validator.service';

@Component({
  selector: 'app-pmode-upload',
  templateUrl: './pmode-upload.component.html',
  styleUrls: ['../pmode.component.css']
})
export class PmodeUploadComponent implements OnInit {

  private url = 'rest/pmode';

  enableSubmit = false;
  submitInProgress = false;

  description: string = '';

  useFileSelector: boolean = true;

  @ViewChild('fileInput', {static: false})
  private fileInput;

  constructor(@Inject(MAT_DIALOG_DATA) private data: { pModeContents: string },
              public dialogRef: MatDialogRef<PmodeUploadComponent>,
              private http: HttpClient, private alertService: AlertService,
              private fileUploadService: FileUploadValidatorService) {
  }

  ngOnInit() {
    this.useFileSelector = !this.data || !this.data.pModeContents;
  }

  public checkFileAndDescription() {
    this.enableSubmit = this.hasFile() && this.description.length !== 0;
  }

  private hasFile(): boolean {
    return (this.useFileSelector && this.fileInput.nativeElement.files.length !== 0)
      || (!this.useFileSelector && !!this.data.pModeContents);
  }

  private getFile(): Blob {
    if (this.useFileSelector) {
      return this.fileInput.nativeElement.files[0];
    } else {
      return new Blob([this.data.pModeContents], {type: 'text/xml'});
    }
  }

  public async submit() {
    if (this.submitInProgress) {
      return;
    }
    this.submitInProgress = true;

    try {
      const file = this.getFile();
      await this.fileUploadService.validateSize(file);
      if (file.type !== 'text/xml') {
        throw new Error('The file type should be xml.');
      }

      let input = new FormData();
      input.append('file', file);
      input.append('description', this.description);
      this.http.post<string>(this.url, input).subscribe(res => {
          this.alertService.success(res, false);
          this.dialogRef.close({done: true});
          this.submitInProgress = false;
        }, err => {
          this.processError(err);
        });
    } catch (err) {
      this.processError(err);
    }
  }

  private processError(err) {
    this.alertService.exception('Error uploading the PMode:', err);
    this.dialogRef.close({done: false});
    this.submitInProgress = false;
  }

  public cancel() {
    this.dialogRef.close({done: false})
  }
}

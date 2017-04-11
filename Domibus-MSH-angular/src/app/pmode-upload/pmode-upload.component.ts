import {Component, OnInit} from '@angular/core';
import {MdDialogRef} from "@angular/material";
import {FileUploadModule} from 'primeng/primeng';

import {FileUploader, FileUploaderOptions} from 'ng2-file-upload'


@Component({
  selector: 'app-pmode-upload',
  templateUrl: './pmode-upload.component.html',
  styleUrls: ['./pmode-upload.component.css']
})
export class PmodeUploadComponent implements OnInit {
  public options: FileUploaderOptions = {url:"/rest/pmode"};
  public uploader: FileUploader = new FileUploader(this.options);

  constructor(public dialogRef: MdDialogRef<PmodeUploadComponent>) {
  }

  ngOnInit() {
  }

}

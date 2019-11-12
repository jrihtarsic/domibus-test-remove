import {Component, OnInit} from '@angular/core';
import {MatDialogRef} from "@angular/material";

@Component({
  selector: 'app-message-dialog',
  templateUrl: './message-dialog.component.html',
  styleUrls: ['./message-dialog.component.css']
})
export class MessageDialogComponent implements OnInit {

  message: any;
  currentSearchSelectedSource: any;
  dateFormat: String = 'yyyy-MM-dd HH:mm:ssZ';

  constructor(public dialogRef: MatDialogRef<MessageDialogComponent>) {
  }

  ngOnInit() {
  }

}

import {Component, ElementRef, EventEmitter, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {Http, URLSearchParams, Response} from '@angular/http';
import {MessageLogResult} from './messagelogresult';
import {Observable} from 'rxjs';
import {AlertService} from '../alert/alert.service';
import {MessagelogDialogComponent} from 'app/messagelog/messagelog-dialog/messagelog-dialog.component';
import {MdDialog, MdDialogRef} from '@angular/material';
import {MessagelogDetailsComponent} from 'app/messagelog/messagelog-details/messagelog-details.component';
import {ColumnPickerBase} from '../common/column-picker/column-picker-base';
import {RowLimiterBase} from '../common/row-limiter/row-limiter-base';
import {DownloadService} from '../download/download.service';
import {AlertComponent} from '../alert/alert.component';
import {isNullOrUndefined} from 'util';
import {AppComponent} from '../app.component';
import {DatatableComponent} from '@swimlane/ngx-datatable';

@Component({
  moduleId: module.id,
  templateUrl: 'messagelog.component.html',
  providers: [],
  styleUrls: ['./messagelog.component.css']
})

export class MessageLogComponent implements OnInit {

  static readonly RESEND_URL: string = 'rest/message/restore?messageId=${messageId}';
  static readonly DOWNLOAD_MESSAGE_URL: string = 'rest/message/download?messageId=${messageId}';
  static readonly MESSAGE_LOG_URL: string = 'rest/messagelog';

  @ViewChild('rowWithDateFormatTpl') public rowWithDateFormatTpl: TemplateRef<any>;
  @ViewChild('nextAttemptInfoTpl') public nextAttemptInfoTpl: TemplateRef<any>;
  @ViewChild('nextAttemptInfoWithDateFormatTpl') public nextAttemptInfoWithDateFormatTpl: TemplateRef<any>;
  @ViewChild('rowActions') rowActions: TemplateRef<any>;
  @ViewChild('list') list: DatatableComponent;

  columnPicker: ColumnPickerBase;
  rowLimiter: RowLimiterBase;

  selected: any[];

  timestampFromMaxDate: Date;
  timestampToMinDate: Date;
  timestampToMaxDate: Date;

  filter: any;
  loading: boolean;
  rows: any[];
  count: number;
  offset: number;
  orderBy: string;
  asc: boolean;

  mshRoles: Array<String>;
  msgTypes: Array<String>;
  msgStatus: Array<String>;
  notifStatus: Array<String>;

  advancedSearch: boolean;

  messageResent: EventEmitter<boolean>;

  constructor(private http: Http, private alertService: AlertService, public dialog: MdDialog, public app: AppComponent,
              private elementRef: ElementRef) {
  }

  ngOnInit() {
    this.columnPicker = new ColumnPickerBase();
    this.rowLimiter = new RowLimiterBase();

    this.selected = [];

    this.timestampFromMaxDate = new Date();
    this.timestampToMinDate = null;
    this.timestampToMaxDate = new Date();

    this.filter = {};
    this.loading = false;
    this.rows = [];
    this.count = 0;
    this.offset = 0;
    this.orderBy = 'received';
    this.asc = false;

    this.messageResent = new EventEmitter(false);

    this.columnPicker.allColumns.push(
      {
        name: 'Message Id',
        width: 275
      },
      {
        name: 'From Party Id'
      },
      {
        name: 'To Party Id'
      },
      {
        name: 'Message Status',
        width: 175
      },
      {
        name: 'Notification Status',
        width: 175
      },
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Received',
        width: 155
      },
      {
        name: 'AP Role',
        prop: 'mshRole'
      },
      {
        cellTemplate: this.nextAttemptInfoTpl,
        name: 'Send Attempts'
      },
      {
        cellTemplate: this.nextAttemptInfoTpl,
        name: 'Send Attempts Max'
      },
      {
        cellTemplate: this.nextAttemptInfoWithDateFormatTpl,
        name: 'Next Attempt',
        width: 155
      },
      {
        name: 'Conversation Id'
      },
      {
        name: 'Message Type',
        width: 130
      },
      {
        name: 'Message Subtype',
        width: 100
      },
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Deleted',
        width: 155
      }
    );

    if (this.app.fourCornerEnabled) {
      this.columnPicker.allColumns.push(
        {
          name: 'Original Sender'
        },
        {
          name: 'Final Recipient'
        });
    }

    this.columnPicker.allColumns.push(
      {
        name: 'Ref To Message Id'
      },
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Failed',
        width: 155
      },
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Restored',
        width: 155
      },
      {
        cellTemplate: this.rowActions,
        name: 'Actions',
        width: 80,
        sortable: false
      }
    );

    this.columnPicker.selectedColumns = this.columnPicker.allColumns.filter(col => {
      return ['Message Id', 'From Party Id', 'To Party Id', 'Message Status', 'Received', 'AP Role', 'Message Type', 'Actions'].indexOf(col.name) != -1
    });

    this.page(this.offset, this.rowLimiter.pageSize);
  }

  public beforeDomainChange() {
    if (this.list.isHorScroll) {
      this.scrollLeft();
    }
  }

  createSearchParams(): URLSearchParams {
    const searchParams = new URLSearchParams();

    if (this.orderBy) {
      searchParams.set('orderBy', this.orderBy);
    }
    if (this.asc != null) {
      searchParams.set('asc', this.asc.toString());
    }

    if (this.filter.messageId) {
      searchParams.set('messageId', this.filter.messageId);
    }

    if (this.filter.mshRole) {
      searchParams.set('mshRole', this.filter.mshRole);
    }

    if (this.filter.conversationId) {
      searchParams.set('conversationId', this.filter.conversationId);
    }

    if (this.filter.messageType) {
      searchParams.set('messageType', this.filter.messageType);
    }

    if (this.filter.messageStatus) {
      searchParams.set('messageStatus', this.filter.messageStatus);
    }

    if (this.filter.notificationStatus) {
      searchParams.set('notificationStatus', this.filter.notificationStatus);
    }

    if (this.filter.fromPartyId) {
      searchParams.set('fromPartyId', this.filter.fromPartyId);
    }

    if (this.filter.toPartyId) {
      searchParams.set('toPartyId', this.filter.toPartyId);
    }

    if (this.filter.originalSender) {
      searchParams.set('originalSender', this.filter.originalSender);
    }

    if (this.filter.finalRecipient) {
      searchParams.set('finalRecipient', this.filter.finalRecipient);
    }

    if (this.filter.refToMessageId) {
      searchParams.set('refToMessageId', this.filter.refToMessageId);
    }

    if (this.filter.receivedFrom) {
      searchParams.set('receivedFrom', this.filter.receivedFrom.getTime());
    }

    if (this.filter.receivedTo) {
      searchParams.set('receivedTo', this.filter.receivedTo.getTime());
    }

    if (this.filter.isTestMessage) {
      searchParams.set('messageSubtype', this.filter.isTestMessage ? 'TEST' : null)
    }

    return searchParams;
  }

  getMessageLogEntries(offset: number, pageSize: number): Observable<MessageLogResult> {

    const searchParams = this.createSearchParams();

    searchParams.set('page', offset.toString());
    searchParams.set('pageSize', pageSize.toString());

    return this.http.get(MessageLogComponent.MESSAGE_LOG_URL, {search: searchParams})
      .map((response: Response) =>
        response.json()
      );
  }

  page(offset, pageSize) {
    this.loading = true;

    this.getMessageLogEntries(offset, pageSize).subscribe((result: MessageLogResult) => {
      // console.log('messageLog response:' + result);
      this.offset = offset;
      this.rowLimiter.pageSize = pageSize;
      this.count = result.count;
      this.selected = [];

      const start = offset * pageSize;
      const end = start + pageSize;
      const newRows = [...result.messageLogEntries];

      let index = 0;
      for (let i = start; i < end; i++) {
        newRows[i] = result.messageLogEntries[index++];
      }

      this.rows = newRows;

      if (result.filter.receivedFrom != null) {
        result.filter.receivedFrom = new Date(result.filter.receivedFrom);
      }
      if (result.filter.receivedTo != null) {
        result.filter.receivedTo = new Date(result.filter.receivedTo);
      }

      result.filter.isTestMessage = !isNullOrUndefined(result.filter.messageSubtype);

      this.filter = result.filter;
      this.mshRoles = result.mshRoles;
      this.msgTypes = result.msgTypes;
      this.msgStatus = result.msgStatus;
      this.notifStatus = result.notifStatus;
      this.loading = false;
    }, (error: any) => {
      console.log('error getting the message log:' + error);
      this.loading = false;
      this.alertService.error('Error occured:' + error);
    });
  }

  onPage(event) {
    this.page(event.offset, event.pageSize);
  }

  onSort(event) {
    this.orderBy = event.column.prop;
    this.asc = (event.newValue === 'desc') ? false : true;

    this.page(this.offset, this.rowLimiter.pageSize);
  }

  onSelect({selected}) {
    // console.log('Select Event', selected, this.selected);
  }

  onActivate(event) {
    // console.log('Activate Event', event);

    if ('dblclick' === event.type) {
      this.details(event.row);
    }
  }

  changePageSize(newPageLimit: number) {
    console.log('New page limit:', newPageLimit);
    this.page(0, newPageLimit);
  }

  search() {
    console.log('Searching using filter:' + this.filter);
    this.page(0, this.rowLimiter.pageSize);
  }

  resendDialog() {
    let dialogRef = this.dialog.open(MessagelogDialogComponent);
    dialogRef.afterClosed().subscribe(result => {
      switch (result) {
        case 'Resend' :
          this.resend(this.selected[0].messageId);
          this.selected = [];
          this.messageResent.subscribe(result => {
            this.search();
          });
          break;
        case 'Cancel' :
        //do nothing
      }
    });
  }

  resend(messageId: string) {
    console.log('Resending message with id ', messageId);

    let url = MessageLogComponent.RESEND_URL.replace('${messageId}', encodeURIComponent(messageId));

    console.log('URL is  ', url);

    this.http.put(url, {}, {}).subscribe(res => {
      this.alertService.success('The operation resend message completed successfully');
      setTimeout(() => {
        this.messageResent.emit();
      }, 500);
    }, err => {
      this.alertService.error('The message ' + messageId + ' could not be resent.');
    });
  }

  isResendButtonEnabledAction(row): boolean {
    return !row.deleted && row.messageStatus === 'SEND_FAILURE';
  }

  isResendButtonEnabled() {
    if (this.selected && this.selected.length == 1 && !this.selected[0].deleted && this.selected[0].messageStatus === 'SEND_FAILURE')
      return true;

    return false;
  }

  isDownloadButtonEnabledAction(row): boolean {
    return !row.deleted && row.messageType !== 'SIGNAL_MESSAGE';
  }

  isDownloadButtonEnabled(): boolean {
    if (this.selected && this.selected.length == 1 && !this.selected[0].deleted)
      return true;

    return false;
  }

  private downloadMessage(messageId) {
    const url = MessageLogComponent.DOWNLOAD_MESSAGE_URL.replace('${messageId}', encodeURIComponent(messageId));
    DownloadService.downloadNative(url);
  }

  downloadAction(row) {
    this.downloadMessage(row.messageId);
  }

  download() {
    this.downloadMessage(this.selected[0].messageId);
  }

  saveAsCSV() {
    if (this.count > AlertComponent.MAX_COUNT_CSV) {
      this.alertService.error(AlertComponent.CSV_ERROR_MESSAGE);
      return;
    }

    DownloadService.downloadNative(MessageLogComponent.MESSAGE_LOG_URL + '/csv?' + this.createSearchParams().toString());
  }

  details(selectedRow: any) {
    const dialogRef: MdDialogRef<MessagelogDetailsComponent> = this.dialog.open(MessagelogDetailsComponent);
    dialogRef.componentInstance.message = selectedRow;
    dialogRef.componentInstance.fourCornerEnabled = this.app.fourCornerEnabled;
    // dialogRef.componentInstance.currentSearchSelectedSource = this.currentSearchSelectedSource;
    dialogRef.afterClosed().subscribe(result => {
      //Todo:
    });
  }

  toggleAdvancedSearch() {
    this.advancedSearch = true;
  }

  toggleBasicSearch() {
    this.advancedSearch = false;

    this.resetAdvancedSearchParams();
  }

  resetAdvancedSearchParams() {
    this.filter.mshRole = null;
    this.filter.conversationId = null;
    this.filter.messageType = this.msgTypes[1];
    this.filter.notificationStatus = null;
    this.filter.originalSender = null;
    this.filter.finalRecipient = null;
    this.filter.refToMessageId = null;
    this.filter.receivedFrom = null;
    this.filter.receivedTo = null;
    this.filter.isTestMessage = null;
  }

  onTimestampFromChange(event) {
    this.timestampToMinDate = event.value;
  }

  onTimestampToChange(event) {
    this.timestampFromMaxDate = event.value;
  }

  private showNextAttemptInfo(row: any): boolean {
    if (row && (row.messageType === 'SIGNAL_MESSAGE' || row.mshRole === 'RECEIVING'))
      return false;
    return true;
  }

  public scrollLeft() {
    const dataTableBodyDom = this.elementRef.nativeElement.querySelector('.datatable-body');

    dataTableBodyDom.scrollLeft = 0;
  }
}

import {ChangeDetectorRef, Component, ElementRef, EventEmitter, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {MessageLogResult} from './messagelogresult';
import {Observable} from 'rxjs';
import {AlertService} from '../common/alert/alert.service';
import {MatDialog, MatSelectChange} from '@angular/material';
import {MessagelogDetailsComponent} from 'app/messagelog/messagelog-details/messagelog-details.component';
import {ColumnPickerBase} from '../common/column-picker/column-picker-base';
import {RowLimiterBase} from '../common/row-limiter/row-limiter-base';
import {DownloadService} from '../common/download.service';
import {AlertComponent} from '../common/alert/alert.component';
import {isNullOrUndefined} from 'util';
import {DatatableComponent} from '@swimlane/ngx-datatable';
import {DomibusInfoService} from '../common/appinfo/domibusinfo.service';
import FilterableListMixin from '../common/mixins/filterable-list.mixin';
import SortableListMixin from '../common/mixins/sortable-list.mixin';
import BaseListComponent from '../common/base-list.component';
import mix from '../common/mixins/mixin.utils';
import {DialogsService} from '../common/dialogs/dialogs.service';

@Component({
  moduleId: module.id,
  templateUrl: 'messagelog.component.html',
  providers: [],
  styleUrls: ['./messagelog.component.css']
})

export class MessageLogComponent extends mix(BaseListComponent).with(FilterableListMixin, SortableListMixin) implements OnInit {
  static readonly RESEND_URL: string = 'rest/message/restore?messageId=${messageId}';
  static readonly DOWNLOAD_MESSAGE_URL: string = 'rest/message/download?messageId=${messageId}';
  static readonly MESSAGE_LOG_URL: string = 'rest/messagelog';

  @ViewChild('rowWithDateFormatTpl', {static: false}) public rowWithDateFormatTpl: TemplateRef<any>;
  @ViewChild('nextAttemptInfoTpl', {static: false}) public nextAttemptInfoTpl: TemplateRef<any>;
  @ViewChild('nextAttemptInfoWithDateFormatTpl', {static: false}) public nextAttemptInfoWithDateFormatTpl: TemplateRef<any>;
  @ViewChild('rowActions', {static: false}) rowActions: TemplateRef<any>;
  @ViewChild('list', {static: false}) list: DatatableComponent;

  columnPicker: ColumnPickerBase;
  public rowLimiter: RowLimiterBase;

  selected: any[];

  timestampFromMaxDate: Date;
  timestampToMinDate: Date;
  timestampToMaxDate: Date;

  loading: boolean;
  rows: any[];
  count: number;
  offset: number;

  mshRoles: Array<String>;
  msgTypes: Array<String>;
  msgStatuses: Array<String>;
  notifStatus: Array<String>;

  advancedSearch: boolean;
  fourCornerEnabled: boolean;

  messageResent: EventEmitter<boolean>;

  canSearchByConversationId: boolean;
  conversationIdValue: String;

  dateFormat: String = 'yyyy-MM-dd HH:mm:ssZ';

  constructor(private http: HttpClient, private alertService: AlertService, private domibusInfoService: DomibusInfoService,
              public dialog: MatDialog, public dialogsService: DialogsService, private elementRef: ElementRef,
              private changeDetector: ChangeDetectorRef) {
    super();
  }

  async ngOnInit() {
    super.ngOnInit();

    this.columnPicker = new ColumnPickerBase();
    this.rowLimiter = new RowLimiterBase();

    this.selected = [];

    this.timestampFromMaxDate = new Date();
    this.timestampToMinDate = null;
    this.timestampToMaxDate = new Date();

    this.loading = false;
    this.rows = [];
    this.count = 0;
    this.offset = 0;
    this['orderBy'] = 'received';
    this['asc'] = false;

    this.messageResent = new EventEmitter(false);

    this.canSearchByConversationId = true;

    this.fourCornerEnabled = await this.domibusInfoService.isFourCornerEnabled();

    this.search();
  }

  async ngAfterViewInit() {
    this.fourCornerEnabled = await this.domibusInfoService.isFourCornerEnabled();
    this.configureColumnPicker();
  }

  ngAfterViewChecked() {
    this.changeDetector.detectChanges();
  }

  private configureColumnPicker() {
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

    if (this.fourCornerEnabled) {
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
  }

  public beforeDomainChange() {
    if (this.list.isHorScroll) {
      this.scrollLeft();
    }
  }


  createSearchParams(): HttpParams {
    let searchParams = new HttpParams();

    if (this.orderBy) {
      searchParams = searchParams.append('orderBy', this.orderBy);
    }
    if (this.asc != null) {
      searchParams = searchParams.append('asc', this.asc.toString());
    }

    if (this.activeFilter.messageId) {
      searchParams = searchParams.append('messageId', this.activeFilter.messageId);
    }

    if (this.activeFilter.mshRole) {
      searchParams = searchParams.append('mshRole', this.activeFilter.mshRole);
    }

    if (this.activeFilter.conversationId) {
      searchParams = searchParams.append('conversationId', this.activeFilter.conversationId);
    }

    if (this.activeFilter.messageType) {
      searchParams = searchParams.append('messageType', this.activeFilter.messageType);
    }

    if (this.activeFilter.messageStatus) {
      searchParams = searchParams.append('messageStatus', this.activeFilter.messageStatus);
    }

    if (this.activeFilter.notificationStatus) {
      searchParams = searchParams.append('notificationStatus', this.activeFilter.notificationStatus);
    }

    if (this.activeFilter.fromPartyId) {
      searchParams = searchParams.append('fromPartyId', this.activeFilter.fromPartyId);
    }

    if (this.activeFilter.toPartyId) {
      searchParams = searchParams.append('toPartyId', this.activeFilter.toPartyId);
    }

    if (this.activeFilter.originalSender) {
      searchParams = searchParams.append('originalSender', this.activeFilter.originalSender);
    }

    if (this.activeFilter.finalRecipient) {
      searchParams = searchParams.append('finalRecipient', this.activeFilter.finalRecipient);
    }

    if (this.activeFilter.refToMessageId) {
      searchParams = searchParams.append('refToMessageId', this.activeFilter.refToMessageId);
    }

    if (this.activeFilter.receivedFrom) {
      searchParams = searchParams.append('receivedFrom', this.activeFilter.receivedFrom.getTime());
    }

    if (this.activeFilter.receivedTo) {
      searchParams = searchParams.append('receivedTo', this.activeFilter.receivedTo.getTime());
    }

    if (this.activeFilter.isTestMessage) {
      searchParams = searchParams.append('messageSubtype', this.activeFilter.isTestMessage ? 'TEST' : null)
    }

    return searchParams;
  }

  getMessageLogEntries(offset: number, pageSize: number): Observable<MessageLogResult> {
    let searchParams = this.createSearchParams();

    searchParams = searchParams.append('page', offset.toString());
    searchParams = searchParams.append('pageSize', pageSize.toString());

    return this.http.get<MessageLogResult>(MessageLogComponent.MESSAGE_LOG_URL, {params: searchParams});
  }

  /**
   * The method is the actual implementation of the abstract method declared in the base abstract class
   */
  page(offset, pageSize) {
    this.loading = true;
    super.resetFilters();
    this.getMessageLogEntries(offset, pageSize).subscribe((result: MessageLogResult) => {
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
      this['filter'] = result.filter;

      this.mshRoles = result.mshRoles;
      this.msgTypes = result.msgTypes;
      this.msgStatuses = result.msgStatus.sort();
      this.notifStatus = result.notifStatus;

      this.loading = false;
    }, (error) => {
      console.log('error getting the message log:', error);
      this.loading = false;
      this.alertService.exception('Error occurred: ', error);
    });
  }

  onPage(event) {
    this.page(event.offset, event.pageSize);
  }

  /**
   * The method is an override of the abstract method defined in SortableList mixin
   */
  public reload() {
    this.page(0, this.rowLimiter.pageSize);
  }

  onActivate(event) {
    if ('dblclick' === event.type) {
      this.details(event.row);
    }
  }

  changePageSize(newPageLimit: number) {
    this.page(0, newPageLimit);
  }

  search() {
    super.setActiveFilter();
    console.log('search by:', this.activeFilter);
    this.page(0, this.rowLimiter.pageSize);
  }

  resendDialog() {
    this.dialogsService.openResendDialog().then(resend => {
      if (resend) {
        this.resend(this.selected[0].messageId);
        this.selected = [];
        this.messageResent.subscribe(() => {
          this.page(0, this.rowLimiter.pageSize);
        });
      }
    });
  }

  resend(messageId: string) {
    console.log('Resending message with id ', messageId);

    let url = MessageLogComponent.RESEND_URL.replace('${messageId}', encodeURIComponent(messageId));

    this.http.put(url, {}, {}).subscribe(res => {
      this.alertService.success('The operation resend message completed successfully');
      setTimeout(() => {
        this.messageResent.emit();
      }, 500);
    }, err => {
      this.alertService.exception('The message ' + messageId + ' could not be resent.', err);
    });
  }

  isResendButtonEnabledAction(row): boolean {
    return this.isRowResendButtonEnabled(row);
  }

  isResendButtonEnabled() {
    return this.isOneRowSelected() && !this.selected[0].deleted
      && this.isRowResendButtonEnabled(this.selected[0]);
  }

  private isRowResendButtonEnabled(row): boolean {
    return !row.deleted
      && (row.messageStatus === 'SEND_FAILURE' || row.messageStatus === 'SEND_ENQUEUED')
      && !this.isSplitAndJoinMessage(row);
  }

  private isSplitAndJoinMessage(row) {
    return row.messageFragment || row.sourceMessage;
  }

  isDownloadButtonEnabledAction(row): boolean {
    return this.isRowDownloadButtonEnabled(row);
  }

  isDownloadButtonEnabled(): boolean {
    return this.isOneRowSelected() && this.isRowDownloadButtonEnabled(this.selected[0]);
  }

  private isRowDownloadButtonEnabled(row): boolean {
    return !row.deleted && row.messageType !== 'SIGNAL_MESSAGE'
      && !this.isSplitAndJoinMessage(row);
  }

  private isOneRowSelected() {
    return this.selected && this.selected.length == 1;
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

    super.resetFilters();
    DownloadService.downloadNative(MessageLogComponent.MESSAGE_LOG_URL + '/csv?' + this.createSearchParams().toString());
  }

  details(selectedRow: any) {
    this.dialog.open(MessagelogDetailsComponent, {
      data: {message: selectedRow, fourCornerEnabled: this.fourCornerEnabled}
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

    this.conversationIdValue = null;
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

  onMessageTypeChanged($event: MatSelectChange) {
    this.canSearchByConversationId = (this.filter.messageType == 'USER_MESSAGE');
    if (this.canSearchByConversationId) {
      this.filter.conversationId = this.conversationIdValue;
    } else {
      this.conversationIdValue = this.filter.conversationId;
      this.filter.conversationId = null;
    }
  }
}

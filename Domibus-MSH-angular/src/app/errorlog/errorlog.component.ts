﻿import {ChangeDetectorRef, Component, ElementRef, OnInit, Renderer2, TemplateRef, ViewChild} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {ErrorLogResult} from './errorlogresult';
import {AlertService} from '../common/alert/alert.service';
import {ErrorlogDetailsComponent} from 'app/errorlog/errorlog-details/errorlog-details.component';
import {MatDialog, MatDialogRef} from '@angular/material';
import mix from '../common/mixins/mixin.utils';
import BaseListComponent from '../common/mixins/base-list.component';
import FilterableListMixin from '../common/mixins/filterable-list.mixin';
import SortableListMixin from '../common/mixins/sortable-list.mixin';
import {ServerPageableListMixin} from '../common/mixins/pageable-list.mixin';

@Component({
  moduleId: module.id,
  templateUrl: 'errorlog.component.html',
  providers: [],
  styleUrls: ['./errorlog.component.css']
})

export class ErrorLogComponent extends mix(BaseListComponent)
  .with(FilterableListMixin, SortableListMixin, ServerPageableListMixin)
  implements OnInit {

  dateFormat: String = 'yyyy-MM-dd HH:mm:ssZ';

  @ViewChild('rowWithDateFormatTpl', {static: false}) rowWithDateFormatTpl: TemplateRef<any>;

  timestampFromMaxDate: Date = new Date();
  timestampToMinDate: Date = null;
  timestampToMaxDate: Date = new Date();

  notifiedFromMaxDate: Date = new Date();
  notifiedToMinDate: Date = null;
  notifiedToMaxDate: Date = new Date();

  mshRoles: string[];
  errorCodes: string[];

  // advancedSearch: boolean;

  static readonly ERROR_LOG_URL: string = 'rest/errorlogs';
  static readonly ERROR_LOG_CSV_URL: string = ErrorLogComponent.ERROR_LOG_URL + '/csv?';

  constructor(private elementRef: ElementRef, private http: HttpClient, private alertService: AlertService,
              public dialog: MatDialog, private changeDetector: ChangeDetectorRef) {
    super();
  }

  ngOnInit() {
    super.ngOnInit();

    super.orderBy = 'timestamp';
    super.asc = false;
    super.sortedColumns = [{prop: 'timestamp', dir: 'desc'}];

    this.filterData();
  }

  ngAfterViewInit() {
    this.columnPicker.allColumns = [
      {
        name: 'Signal Message Id',
        prop: 'errorSignalMessageId'
      },
      {
        name: 'AP Role',
        prop: 'mshRole',
        width: 50
      },
      {
        name: 'Message Id',
        prop: 'messageInErrorId',
      },
      {
        name: 'Error Code',
        // width: 50
      },
      {
        name: 'Error Detail',
        width: 350
      },
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Timestamp',
        // width: 180
      },
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Notified',
        width: 50
      }

    ];

    this.columnPicker.selectedColumns = this.columnPicker.allColumns.filter(col => {
      return ['Message Id', 'Error Code', 'Timestamp'].indexOf(col.name) != -1
    });
  }

  ngAfterViewChecked() {
    this.changeDetector.detectChanges();
  }

  // createAndSetParameters(): HttpParams {
  //   let searchParams = new HttpParams();
  //
  //   if (this.orderBy) {
  //     searchParams = searchParams.append('orderBy', this.orderBy);
  //   }
  //   if (this.asc != null) {
  //     searchParams = searchParams.append('asc', this.asc.toString());
  //   }
  //
  //   if (this.activeFilter.errorSignalMessageId) {
  //     searchParams = searchParams.append('errorSignalMessageId', this.activeFilter.errorSignalMessageId);
  //   }
  //   if (this.activeFilter.mshRole) {
  //     searchParams = searchParams.append('mshRole', this.activeFilter.mshRole);
  //   }
  //   if (this.activeFilter.messageInErrorId) {
  //     searchParams = searchParams.append('messageInErrorId', this.activeFilter.messageInErrorId);
  //   }
  //   if (this.activeFilter.errorCode) {
  //     searchParams = searchParams.append('errorCode', this.activeFilter.errorCode);
  //   }
  //   if (this.activeFilter.errorDetail) {
  //     searchParams = searchParams.append('errorDetail', this.activeFilter.errorDetail);
  //   }
  //   if (this.activeFilter.timestampFrom != null) {
  //     searchParams = searchParams.append('timestampFrom', this.activeFilter.timestampFrom.getTime());
  //   }
  //   if (this.activeFilter.timestampTo != null) {
  //     searchParams = searchParams.append('timestampTo', this.activeFilter.timestampTo.getTime());
  //   }
  //   if (this.activeFilter.notifiedFrom != null) {
  //     searchParams = searchParams.append('notifiedFrom', this.activeFilter.notifiedFrom.getTime());
  //   }
  //   if (this.activeFilter.notifiedTo != null) {
  //     searchParams = searchParams.append('notifiedTo', this.activeFilter.notifiedTo.getTime());
  //   }
  //
  //   return searchParams;
  // }

  protected get name(): string {
    return 'Error Logs';
  }

  protected get GETUrl(): string {
    return ErrorLogComponent.ERROR_LOG_URL;
  }

  // getServerData(): Promise<ErrorLogResult> {
  //   let searchParams = this.createAndSetParameters();
  //
  //   searchParams = searchParams.append('page', this.offset.toString());
  //   searchParams = searchParams.append('pageSize', this.rowLimiter.pageSize.toString());
  //
  //   return this.http.get<ErrorLogResult>(ErrorLogComponent.ERROR_LOG_URL, {params: searchParams})
  //     .toPromise();
  // }

  public setServerResults(result: ErrorLogResult) {
    super.count = result.count;
    super.rows = result.errorLogEntries;

    if (result.filter.timestampFrom) {
      result.filter.timestampFrom = new Date(result.filter.timestampFrom);
    }
    if (result.filter.timestampTo) {
      result.filter.timestampTo = new Date(result.filter.timestampTo);
    }
    if (result.filter.notifiedFrom) {
      result.filter.notifiedFrom = new Date(result.filter.notifiedFrom);
    }
    if (result.filter.notifiedTo) {
      result.filter.notifiedTo = new Date(result.filter.notifiedTo);
    }

    super.filter = result.filter;
    this.mshRoles = result.mshRoles;
    this.errorCodes = result.errorCodes;
  }

  onTimestampFromChange(event) {
    this.timestampToMinDate = event.value;
  }

  onTimestampToChange(event) {
    this.timestampFromMaxDate = event.value;
  }

  onNotifiedFromChange(event) {
    this.notifiedToMinDate = event.value;
  }

  onNotifiedToChange(event) {
    this.notifiedFromMaxDate = event.value;
  }

  // toggleAdvancedSearch(): boolean {
  //   this.advancedSearch = !this.advancedSearch;
  //   return false;//to prevent default navigation
  // }

  // onActivate(event) {
  //   if ('dblclick' === event.type) {
  //     this.showDetails(event.row);
  //   }
  // }

  showDetails(selectedRow: any) {
    let dialogRef: MatDialogRef<ErrorlogDetailsComponent> = this.dialog.open(ErrorlogDetailsComponent);
    dialogRef.componentInstance.message = selectedRow;
    // dialogRef.componentInstance.currentSearchSelectedSource = this.currentSearchSelectedSource;
    dialogRef.afterClosed().subscribe(result => {
      //Todo:
    });
  }

  public get csvUrl(): string {
    return ErrorLogComponent.ERROR_LOG_CSV_URL + this.createAndSetParameters().toString();
  }
}

import {ChangeDetectorRef, Component, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {AuditService} from './audit.service';
import {UserService} from '../user/user.service';
import {AlertService} from '../common/alert/alert.service';
import {AuditCriteria, AuditResponseRo} from './audit';
import {Observable} from 'rxjs/Observable';
import mix from '../common/mixins/mixin.utils';
import BaseListComponent from '../common/mixins/base-list.component';
import FilterableListMixin from '../common/mixins/filterable-list.mixin';
import {ServerPageableListMixin} from '../common/mixins/pageable-list.mixin';
import {AlertsResult} from '../alerts/alertsresult';
import {HttpClient, HttpParams} from '@angular/common/http';

/**
 * @author Thomas Dussart
 * @since 4.0
 *
 * In charge of retrieving audit information from the backend.
 */

@Component({
  selector: 'app-audit',
  providers: [AuditService, UserService],
  templateUrl: './audit.component.html',
  styleUrls: ['./audit.component.css']
})
export class AuditComponent extends mix(BaseListComponent)
  .with(FilterableListMixin, ServerPageableListMixin) implements OnInit {

  @ViewChild('rowWithDateFormatTpl', {static: false}) rowWithDateFormatTpl: TemplateRef<any>;

// --- Search components binding ---
  existingAuditTargets = [];
  existingUsers = [];
  existingActions = [];

  timestampFromMaxDate: Date;
  timestampToMinDate: Date;
  timestampToMaxDate: Date;

  // advancedSearch: boolean;

// --- Table binding ---
  dateFormat: String = 'yyyy-MM-dd HH:mm:ssZ';

  constructor(private auditService: AuditService, private userService: UserService, private alertService: AlertService,
              private changeDetector: ChangeDetectorRef, private http: HttpClient) {
    super();
  }

  ngOnInit() {
    super.ngOnInit();

// --- lets init the component's data ---
    this.existingUsers = [];
    const userObservable = this.userService.getUserNames();
    userObservable.subscribe((userNames: string[]) => this.existingUsers.push(...userNames));

    this.existingActions = [];
    const actionObservable = this.auditService.listActions();
    actionObservable.subscribe((action: string) => this.existingActions.push(action));

    this.existingAuditTargets = [];
    const existingTargets = this.auditService.listTargetTypes();
    existingTargets.subscribe((targets: string[]) => this.existingAuditTargets.push(...targets));

    this.timestampFromMaxDate = new Date();
    this.timestampToMinDate = null;
    this.timestampToMaxDate = new Date();

// --- lets count the records and fill the table.---
    this.filterData();
  }

  filterData() {
    super.filterData();
    this.countRecords();
  }

  ngAfterViewInit() {
    this.initColumns();
  }

  ngAfterViewChecked() {
    this.changeDetector.detectChanges();
  }

  // public async getDataAndSetResults(): Promise<any> {
  //   return this.searchAuditLog();
  // }

  countRecords() {
    // this.auditService.countAuditLogs(this.buildCriteria()).toPromise()

    this.auditService.countAuditLogs(this.createAndSetParameters()).toPromise()
      .then(auditCount => {
        super.count = auditCount;
      }, err => {
        this.alertService.exception('Error counting audit entries: ', err);
      });
  }

  protected get GETUrl(): string {
    return 'rest/audit/list';
  }

  public setServerResults(result: AuditResponseRo[]) {
    super.rows = result;
  }

  // toggleAdvancedSearch() {
  //   this.advancedSearch = !this.advancedSearch;
  //   return false; // to prevent default navigation
  // }

  protected createAndSetParameters(): HttpParams {
    let filterParams = super.createAndSetParameters();

    filterParams = filterParams.set('start', this.offset * this.rowLimiter.pageSize);
    filterParams = filterParams.set('max', this.rowLimiter.pageSize);

    return filterParams;
  }

  // searchAuditLog(): Promise<any> {
  //   const auditCriteria: AuditCriteria = this.buildCriteria();
  //   return this.auditService.listAuditLogs(auditCriteria).toPromise()
  //     .then((response: AuditResponseRo[]) => {
  //       super.rows = response;
  //     });
  // }

  // buildCriteria(): AuditCriteria {
  //   const auditCriteria: AuditCriteria = new AuditCriteria();
  //
  //   auditCriteria.auditTargetName = this.activeFilter.auditTargetName;
  //   auditCriteria.user = this.activeFilter.user;
  //   auditCriteria.action = this.activeFilter.action;
  //   auditCriteria.from = this.activeFilter.from;
  //   auditCriteria.to = this.activeFilter.to;
  //
  //   auditCriteria.start = this.offset * this.rowLimiter.pageSize;
  //   auditCriteria.max = this.rowLimiter.pageSize;
  //
  //   return auditCriteria;
  // }

  initColumns() {
    this.columnPicker.allColumns = [
      {
        name: 'Table',
        prop: 'auditTargetName',
        width: 20,
        sortable: false
      },
      {
        name: 'User',
        prop: 'user',
        width: 20,
        sortable: false
      },
      {
        name: 'Action',
        prop: 'action',
        width: 20,
        sortable: false
      },
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Changed',
        prop: 'changed',
        width: 80,
        sortable: false
      },
      {
        name: 'Id',
        prop: 'id',
        width: 300,
        sortable: false
      }
    ];
    this.columnPicker.selectedColumns = this.columnPicker.allColumns.filter(col => {
      return ['Table', 'User', 'Action', 'Changed', 'Id'].indexOf(col.name) != -1
    })
  }

  onTimestampFromChange(event) {
    this.timestampToMinDate = event.value;
  }

  onTimestampToChange(event) {
    this.timestampFromMaxDate = event.value;
  }

  public get csvUrl(): string {
    return 'rest/audit/csv?' + this.createAndSetParameters().toString();
  }
}

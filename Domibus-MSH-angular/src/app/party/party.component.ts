import {Component, OnInit} from '@angular/core';
import {MdDialog, MdDialogRef} from '@angular/material';
import {RowLimiterBase} from 'app/common/row-limiter/row-limiter-base';
import {ColumnPickerBase} from 'app/common/column-picker/column-picker-base';
import {PartyService} from './party.service';
import {CertificateRo, PartyFilteredResult, PartyResponseRo, ProcessRo} from './party';
import {Observable} from 'rxjs/Observable';
import {AlertService} from '../common/alert/alert.service';
import {AlertComponent} from '../common/alert/alert.component';
import {PartyDetailsComponent} from './party-details/party-details.component';
import {DirtyOperations} from '../common/dirty-operations';
import {CancelDialogComponent} from '../common/cancel-dialog/cancel-dialog.component';
import {CurrentPModeComponent} from '../pmode/current/currentPMode.component';
import {Http} from '@angular/http';
import {FilterableListComponent} from '../common/filterable-list.component';

/**
 * @author Thomas Dussart
 * @since 4.0
 */

@Component({
  selector: 'app-party',
  providers: [PartyService],
  templateUrl: './party.component.html',
  styleUrls: ['./party.component.css']
})

export class PartyComponent extends FilterableListComponent implements OnInit, DirtyOperations {
  rows: PartyResponseRo[];
  allRows: PartyResponseRo[];
  selected: PartyResponseRo[];

  rowLimiter: RowLimiterBase = new RowLimiterBase();
  columnPicker: ColumnPickerBase = new ColumnPickerBase();

  offset: number;
  count: number;
  loading: boolean;

  newParties: PartyResponseRo[];
  updatedParties: PartyResponseRo[];
  deletedParties: PartyResponseRo[];

  allProcesses: string[];

  pModeExists: boolean;
  isBusy: boolean;

  constructor(public dialog: MdDialog, public partyService: PartyService, public alertService: AlertService, private http: Http) {
    super();
  }

  async ngOnInit() {
    super.ngOnInit();

    this.isBusy = false;
    this.rows = [];
    this.allRows = [];
    this.selected = [];

    this.offset = 0;
    this.count = 0;
    this.loading = false;

    this.newParties = [];
    this.updatedParties = [];
    this.deletedParties = [];

    this.initColumns();

    const res = await this.http.get(CurrentPModeComponent.PMODE_URL + '/current').toPromise();
    if (res && res.text()) {
      this.pModeExists = true;
      this.search();
    } else {
      this.pModeExists = false;
    }
  }

  isDirty(): boolean {
    return this.newParties.length + this.updatedParties.length + this.deletedParties.length > 0;
  }

  resetDirty() {
    this.newParties.length = 0;
    this.updatedParties.length = 0;
    this.deletedParties.length = 0;
  }

  searchIfOK() {
    this.checkIsDirtyAndThen(() => {
      this.search();
    });
  }

  private search() {
    super.setActiveFilter();
    this.listPartiesAndProcesses();
  }

  listPartiesAndProcesses() {
    this.offset = 0;
    return Observable.forkJoin([
      this.partyService.listParties(this.activeFilter.name, this.activeFilter.endPoint, this.activeFilter.partyID,
        this.activeFilter.process, this.activeFilter.process_role),
      this.partyService.listProcesses()
    ])
      .subscribe((data: any[]) => {
          const partiesRes: PartyFilteredResult = data[0];
          const processes: ProcessRo[] = data[1];

          this.allProcesses = processes.map(el => el.name);

          this.rows = partiesRes.data;
          this.allRows = partiesRes.allData;
          this.count = this.allRows.length;
          this.selected.length = 0;

          this.loading = false;
          this.resetDirty();
        },
        error => {
          this.alertService.error('Could not load parties due to: "' + error + '"');
          this.loading = false;
        }
      );
  }

  refresh() {
    // ugly but the grid does not feel the paging changes otherwise
    this.loading = true;
    const rows = this.rows;
    this.rows = [];

    setTimeout(() => {
      this.rows = rows;
      this.selected.length = 0;

      this.loading = false;
      this.resetDirty();
    }, 50);
  }

  initColumns() {
    this.columnPicker.allColumns = [
      {
        name: 'Party Name',
        prop: 'name',
        width: 10
      },
      {
        name: 'End Point',
        prop: 'endpoint',
        width: 150
      },
      {
        name: 'Party Id',
        prop: 'joinedIdentifiers',
        width: 10
      },
      {
        name: 'Process (I=Initiator, R=Responder, IR=Both)',
        prop: 'joinedProcesses',
        width: 200
      }
    ];
    this.columnPicker.selectedColumns = this.columnPicker.allColumns.filter(col => {
      return ['name', 'endpoint', 'joinedIdentifiers', 'joinedProcesses'].indexOf(col.prop) !== -1
    })
  }

  changePageSize(newPageLimit: number) {
    super.resetFilters();
    this.offset = 0;
    this.rowLimiter.pageSize = newPageLimit;
    this.refresh();
  }

  onPageChange(event: any) {
    super.resetFilters();
    this.offset = event.offset;
  }

  saveAsCSV() {
    if (this.rows.length > AlertComponent.MAX_COUNT_CSV) {
      this.alertService.error(AlertComponent.CSV_ERROR_MESSAGE);
      return;
    }

    super.resetFilters();
    this.partyService.saveAsCsv(this.activeFilter.name, this.activeFilter.endPoint, this.activeFilter.partyID,
      this.activeFilter.process, this.activeFilter.process_role);
  }

  onActivate(event) {
    if ('dblclick' === event.type) {
      this.edit(event.row);
    }
  }

  canAdd() {
    return !!this.pModeExists && !this.isBusy;
  }

  canSave() {
    return this.isDirty() && !this.isBusy;
  }

  canEdit() {
    return !!this.pModeExists && this.selected.length === 1 && !this.isBusy;
  }

  canCancel() {
    return this.isDirty() && !this.isBusy;
  }

  canDelete() {
    return !!this.pModeExists && this.selected.length === 1 && !this.isBusy;
  }

  cancel() {
    if (this.isBusy) return;

    super.resetFilters();
    this.listPartiesAndProcesses();
  }

  save() {
    if (this.isBusy) return;

    try {
      this.partyService.validateParties(this.rows)
    } catch (err) {
      this.alertService.exception('Party validation error:', err, false);
      return;
    }

    this.isBusy = true;
    this.partyService.updateParties(this.rows)
      .then(() => {
        this.resetDirty();
        this.isBusy = false;
        this.alertService.success('Parties saved successfully.', false);
      })
      .catch(err => {
        this.isBusy = false;
        this.alertService.exception('Party update error:', err, false);
      })
  }

  async add() {
    if (this.isBusy) return;

    const newParty = this.partyService.initParty();
    this.rows.push(newParty);
    this.allRows.push(newParty);

    this.selected.length = 0;
    this.selected.push(newParty);
    this.count++;

    this.newParties.push(newParty);
    const ok = await this.edit(newParty);
    if (!ok) {
      this.remove();
    }
  }

  remove() {
    if (this.isBusy) return;

    const deletedParty = this.selected[0];
    if (!deletedParty) return;

    this.rows.splice(this.rows.indexOf(deletedParty), 1);
    this.allRows.splice(this.rows.indexOf(deletedParty), 1);

    this.selected.length = 0;
    this.count--;

    if (this.newParties.indexOf(deletedParty) < 0)
      this.deletedParties.push(deletedParty);
    else
      this.newParties.splice(this.newParties.indexOf(deletedParty), 1);
  }

  async edit(row): Promise<boolean> {
    row = row || this.selected[0];

    await this.manageCertificate(row);

    const rowCopy = JSON.parse(JSON.stringify(row));
    const allProcessesCopy = JSON.parse(JSON.stringify(this.allProcesses));

    const dialogRef: MdDialogRef<PartyDetailsComponent> = this.dialog.open(PartyDetailsComponent, {
      data: {
        edit: rowCopy,
        allProcesses: allProcessesCopy
      }
    });

    const ok = await dialogRef.afterClosed().toPromise();
    if (ok) {
      if (JSON.stringify(row) === JSON.stringify(rowCopy))
        return; // nothing changed

      Object.assign(row, rowCopy);
      if (this.updatedParties.indexOf(row) < 0)
        this.updatedParties.push(row);
    }

    return ok;
  }

  manageCertificate(party: PartyResponseRo): Promise<CertificateRo> {
    return new Promise((resolve, reject) => {
      if (!party.certificate) {
        this.partyService.getCertificate(party.name)
          .subscribe((cert: CertificateRo) => {
            party.certificate = cert;
            resolve(party);
          }, err => {
            resolve(party);
          });
      } else {
        resolve(party);
      }
    });
  }

  checkIsDirtyAndThen(func: Function) {
    if (this.isDirty()) {
      this.dialog.open(CancelDialogComponent).afterClosed().subscribe(yes => {
        if (yes) {
          func.call(this);
        }
      });
    } else {
      func.call(this);
    }
  }

  OnSort() {
    super.resetFilters();
  }
}

import {Component, OnInit} from "@angular/core";
import {MdDialog} from "@angular/material";
import {RowLimiterBase} from "app/common/row-limiter/row-limiter-base";
import {ColumnPickerBase} from "app/common/column-picker/column-picker-base";
import {PartyService} from "./party.service";
import {PartyResponseRo} from "./party";
import {Observable} from "rxjs/Observable";
import {AlertService} from "../alert/alert.service";
import {AlertComponent} from "../alert/alert.component";

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

export class PartyComponent implements OnInit {

  name: string;
  endPoint: string;
  partyID: string;
  process: string;

  rows = [];
  rowLimiter: RowLimiterBase = new RowLimiterBase();
  columnPicker: ColumnPickerBase = new ColumnPickerBase();
  offset: number = 0;
  count: number = 0;
  loading: boolean = false;

  constructor(public dialog: MdDialog, public partyService: PartyService, public alertService: AlertService) {
  }

  ngOnInit() {
    this.initColumns();
    this.searchAndCount();
    //let dialogRef: MdDialogRef<PartyDetailsComponent> = this.dialog.open(PartyDetailsComponent);
  }

  searchAndCount() {
    this.offset = 0;
    this.loading = true;
    let pageStart = this.offset * this.rowLimiter.pageSize;
    let pageSize = this.rowLimiter.pageSize;

    let partyObservable: Observable<PartyResponseRo[]> = this.partyService.listParties(
      this.name,
      this.endPoint,
      this.partyID,
      this.process,
      pageStart,
      pageSize);

    let countObservable: Observable<number> = this.partyService.countParty(
      this.name,
      this.endPoint,
      this.partyID,
      this.process);

    Observable.zip(partyObservable, countObservable).subscribe((response) => {
        this.rows = response[0];
        this.count = response[1];
        this.loading = false;
        if(this.count > AlertComponent.MAX_COUNT_CSV) {
          this.alertService.error("Maximum number of rows reached for downloading CSV");
        }
      },
      error => {
        this.alertService.error("Could not load parties" + error);
        this.loading = false;
      }
    );
  }

  search(){
    this.loading = true;
    let pageStart = this.offset * this.rowLimiter.pageSize;
    let pageSize = this.rowLimiter.pageSize;

    this.partyService.listParties(
      this.name,
      this.endPoint,
      this.partyID,
      this.process,
      pageStart,
      pageSize).subscribe((response) => {
        this.rows = response;
        this.loading = false;
      },
      error => {
        this.alertService.error("Could not load parties" + error);
        this.loading = false;
      }
    );

  }

  initColumns() {
    this.columnPicker.allColumns = [
      {
        name: 'Name',
        prop: 'name',
        width: 10
      },
      {
        name: 'End point',
        prop: 'endpoint',
        width: 200
      },
      {
        name: 'Party id',
        prop: 'joinedIdentifiers',
        width: 20
      },
      {
        name: 'Process',
        prop: 'joinedProcesses',
        width: 150
      }
    ];
    this.columnPicker.selectedColumns = this.columnPicker.allColumns.filter(col => {
      return ["Name", "End point", "Party id", 'Process'].indexOf(col.name) != -1
    })
  }

  changePageSize(newPageLimit: number) {
    this.offset = 0;
    this.rowLimiter.pageSize = newPageLimit;
    this.searchAndCount();
  }

  onPage(event) {
    console.log('Page Event', event);
    this.offset = event.offset;
    this.search();
  }

  isSaveAsCSVButtonEnabled() {
    return (this.count < AlertComponent.MAX_COUNT_CSV);
  }

  saveAsCSV() {
    this.partyService.saveAsCsv(this.name, this.endPoint, this.partyID, this.process);
  }

}

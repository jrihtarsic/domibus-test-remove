﻿import {Component, OnInit, TemplateRef, ViewChild} from "@angular/core";
import {ColumnPickerBase} from "../common/column-picker/column-picker-base";
import {RowLimiterBase} from "../common/row-limiter/row-limiter-base";
import {Http, Headers, Response} from "@angular/http";
import {AlertService} from "app/alert/alert.service";
import {MdDialog} from "@angular/material";
import {isNullOrUndefined} from "util";
import {PmodeUploadComponent} from "./pmode-upload/pmode-upload.component";
import * as FileSaver from "file-saver";
import {CancelDialogComponent} from "../common/cancel-dialog/cancel-dialog.component";
import {RollbackDialogComponent} from "app/pmode/rollback-dialog/rollback-dialog.component";
import {SaveDialogComponent} from "../common/save-dialog/save-dialog.component";
import {DirtyOperations} from "../common/dirty-operations";
import {RollbackDirtyDialogComponent} from "./rollback-dirty-dialog/rollback-dirty-dialog.component";
import {PmodeDirtyUploadComponent} from "./pmode-dirty-upload/pmode-dirty-upload.component";
import {Observable} from "rxjs/Observable";
import {DateFormatService} from "../customDate/dateformat.service";
import {DownloadService} from "../download/download.service";
import {AlertComponent} from "../alert/alert.component";

@Component({
  moduleId: module.id,
  templateUrl: 'pmode.component.html',
  providers: [],
  styleUrls: ['./pmode.component.css']
})

/**
 * PMode Component Typescript
 */
export class PModeComponent implements OnInit, DirtyOperations {
  private ERROR_PMODE_EMPTY = "As PMode is empty, no file was downloaded.";

  @ViewChild('rowWithDateFormatTpl') public rowWithDateFormatTpl: TemplateRef<any>;
  @ViewChild('rowActions') rowActions: TemplateRef<any>;

  loading: boolean = false;

  public pModeExists = false;
  private pModeContents: string = '';

  allPModes = [];
  tableRows = [];
  selected = [];
  columnPicker: ColumnPickerBase = new ColumnPickerBase();
  rowLimiter: RowLimiterBase = new RowLimiterBase();
  count: number = 0;
  offset: number = 0;

  disabledSave = true;
  disabledCancel = true;
  disabledDownload = true;
  disabledDelete = true;
  disabledRollback = true;

  actualId: number = 0;
  actualRow: number = 0;

  deleteList = [];

  // needed for the first request after upload
  // datatable was empty if we don't do the request again
  // resize window shows information
  // check: @selectedIndexChange(value)
  private uploaded = false;

  private headers = new Headers({'Content-Type': 'application/json'});

  static readonly PMODE_URL: string = "rest/pmode";
  static readonly PMODE_CSV_URL: string = PModeComponent.PMODE_URL + "/csv";

  /**
   * Constructor
   * @param {Http} http Http object used for the requests
   * @param {AlertService} alertService Alert Service object used for alerting success and error messages
   * @param {MdDialog} dialog Object used for opening dialogs
   */
  constructor(private http: Http, private alertService: AlertService, public dialog: MdDialog) {
  }

  /**
   * NgOnInit method
   */
  ngOnInit() {
    this.initializeArchivePmodes();
  }

  /**
   * Initialize columns and gets all PMode entries from database
   */
  initializeArchivePmodes() {
    this.columnPicker.allColumns = [
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Configuration Date'
      },
      {
        name: 'Username'
      },
      {
        name: 'Description'
      },
      {
        cellTemplate: this.rowActions,
        name: 'Actions',
        width: 80,
        sortable: false
      }
    ];

    this.columnPicker.selectedColumns = this.columnPicker.allColumns.filter(col => {
      return ["Configuration Date", "Username", "Description", "Actions"].indexOf(col.name) != -1
    });

    this.getAllPModeEntries();
  }

  /**
   * Change Page size for a @newPageLimit value
   * @param {number} newPageLimit New value for page limit
   */
  changePageSize(newPageLimit: number) {
    console.log('New page limit:', newPageLimit);
    this.rowLimiter.pageSize = newPageLimit;
    this.page(0, newPageLimit);
  }

  /**
   * Gets all the PMode
   * @returns {Observable<Response>}
   */
  getResultObservable():Observable<Response>{
    return this.http.get(PModeComponent.PMODE_URL + "/list")
    .publishReplay(1).refCount();
  }

  /**
   * Gets all the PModes Entries
   */
  getAllPModeEntries() {
    this.getResultObservable().subscribe((response: Response) => {
      this.allPModes = response.json();
      this.allPModes[0].current = true;
      this.actualId = this.allPModes[0].id;
      this.getActivePMode();
      this.actualRow = 0;
      this.count = response.json().length;
      if(this.count > AlertComponent.MAX_COUNT_CSV) {
        this.alertService.error("Maximum number of rows reached for downloading CSV");
      }
    },
      () => {},
      () => {
        this.tableRows = this.allPModes.slice(0, this.rowLimiter.pageSize);
        this.tableRows[0].current = true;
        this.tableRows[0].description = "[CURRENT]: " + this.allPModes[0].description;
      });
  }

  /**
   *
   * @param offset
   * @param pageSize
   */
  page(offset, pageSize) {
    this.loading = true;

    this.offset = offset;
    this.rowLimiter.pageSize = pageSize;

    this.tableRows = this.allPModes.slice(this.offset * this.rowLimiter.pageSize, (this.offset + 1) * this.rowLimiter.pageSize);

    this.loading = false;
  }

  /**
   *
   * @param event
   */
  onPage(event) {
    console.log('Page Event', event);
    this.page(event.offset, event.pageSize);
  }

  /**
   * Disable All the Buttons
   * used mainly when no row is selected
   */
  private disableAllButtons() {
    this.disabledSave = true;
    this.disabledCancel = true;
    this.disabledDownload = true;
    this.disabledDelete = true;
    this.disabledRollback = true;
  }

  /**
   * Enable Save and Cancel buttons
   * used when changes occurred (deleted entries)
   */
  private enableSaveAndCancelButtons() {
    this.disabledSave = false;
    this.disabledCancel = false;
    this.disabledDownload = true;
    this.disabledDelete = true;
    this.disabledRollback = true;
  }

  /**
   * Method called by NgxDatatable on selection/deselection
   * @param {any} selected selected/unselected object
   */
  onSelect({selected}) {
    console.log('Select Event', selected, this.selected);
    if (isNullOrUndefined(selected) || selected.length == 0) {
      this.disableAllButtons();
      return;
    }

    this.disabledDownload = !(this.selected[0] != null && this.selected.length == 1);
    this.disabledDelete = this.selected.findIndex(sel => sel.id === this.actualId) != -1;
    this.disabledRollback = !(this.selected[0] != null && this.selected.length == 1 && this.selected[0].id !== this.actualId);
  }

  /**
   * Method used when button save is clicked
   */
  saveButton(withDownloadCSV: boolean) {
    let dialogRef = this.dialog.open(SaveDialogComponent);
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.http.delete(PModeComponent.PMODE_URL,{params: {ids: JSON.stringify(this.deleteList)}}).subscribe(() => {
            this.alertService.success("The operation 'update pmodes' completed successfully.", false);
            this.disableAllButtons();
            this.selected = [];
            this.deleteList = [];
            if(withDownloadCSV) {
              DownloadService.downloadNative(PModeComponent.PMODE_CSV_URL);
            }
          },
          () => {
            this.alertService.error("The operation 'update pmodes' not completed successfully.", false);
            this.getAllPModeEntries();
            this.disableAllButtons();
            this.selected = [];
          });
      } else {
        if(withDownloadCSV) {
          DownloadService.downloadNative(PModeComponent.PMODE_CSV_URL);
        }
      }
    });
  }

  /**
   * Method used when Cancel button is clicked
   */
  cancelButton() {
    let dialogRef = this.dialog.open(CancelDialogComponent);
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.deleteList = [];
        this.initializeArchivePmodes();
        this.disabledSave = true;
        this.disabledCancel = true;
      } else {
        this.disabledSave = false;
        this.disabledCancel = false;
      }
    });
    this.disabledDownload = true;
    this.disabledDelete = true;
    this.disabledRollback = true;
    this.selected = [];
  }

  /**
   * Method called when Download button is clicked
   * @param row The selected row
   */
  downloadArchive(row) {
    this.download(this.tableRows[row].id);
  }

  /**
   * Method called when Action Delete icon is clicked
   * @param row Row where Delete icon is located
   */
  deleteArchiveAction(row) {

    // workaround to delete one entry from the array
    // since "this.rows.splice(row, 1);" doesn't work...
    let array = this.tableRows.slice();
    this.deleteList.push(array[row].id);
    array.splice(row, 1);
    array = array.concat(this.allPModes[this.offset * this.rowLimiter.pageSize + this.rowLimiter.pageSize]);
    this.allPModes.splice(this.offset * this.rowLimiter.pageSize + row, 1);
    this.tableRows = array.slice();
    this.count--;

    setTimeout(() => {
      this.selected = [];
      this.enableSaveAndCancelButtons();
    }, 100);
  }

  /**
   * Method called when Delete button is clicked
   * All the selected rows will be deleted
   */
  deleteArchive() {
    for (let i = this.selected.length - 1; i >= 0; i--) {
      let array = this.tableRows.slice();
      array.splice(this.selected[i].$$index, 1);
      array = array.concat(this.allPModes[this.offset * this.rowLimiter.pageSize + this.rowLimiter.pageSize]);
      this.allPModes.splice(this.offset * this.rowLimiter.pageSize + this.selected[i].$$index, 1);
      this.tableRows = array.slice();
      this.deleteList.push(this.selected[i].id);
      this.count--;
    }

    this.enableSaveAndCancelButtons();
    this.selected = [];
  }

  /**
   * Method called when Rollback button is clicked
   * Rollbacks the PMode for the selected row
   * - Creates a similar entry like @selectedRow
   * - Sets that entry as current
   *
   * @param selectedRow Selected Row
   */
  rollbackArchive(selectedRow) {
    if (!this.isDirty()) {
      let dialogRef = this.dialog.open(RollbackDialogComponent);
      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.allPModes[this.actualRow].current = false;
          this.http.put(PModeComponent.PMODE_URL + "/rollback/" + selectedRow.id, null,{headers: this.headers}).subscribe(res => {
            this.actualRow = 0;

            this.getAllPModeEntries();

            this.disableAllButtons();
            this.selected = [];
          });
        }
      });
    } else {
      let dialogRef = this.dialog.open(RollbackDirtyDialogComponent);
      dialogRef.afterClosed().subscribe(result => {
        if (result === 'ok') {
          this.http.delete(PModeComponent.PMODE_URL, {params: {ids: JSON.stringify(this.deleteList)}}).subscribe(result => {
              this.deleteList = [];
              this.disableAllButtons();
              this.selected = [];
              this.allPModes[this.actualRow].current = false;
              this.http.put(PModeComponent.PMODE_URL + "/rollback/" + selectedRow.id, null, {headers: this.headers}).subscribe(res => {
                this.actualRow = 0;
                this.getAllPModeEntries();
              });
            },
            error => {
              this.alertService.error("The operation 'update pmodes' not completed successfully.", false);
              this.enableSaveAndCancelButtons();
              this.selected = [];
            });
        } else if (result === 'rollback_only') {
          this.deleteList = [];
          this.allPModes[this.actualRow].current = false;
          this.http.put(PModeComponent.PMODE_URL + "/rollback/" + selectedRow.id, null, {headers: this.headers}).subscribe(res => {
            this.actualRow = 0;
            this.getAllPModeEntries();
          });
          this.disableAllButtons();
        }
        this.selected = [];
      });
    }
    this.page(0,this.rowLimiter.pageSize);
  }

  /**
   * Get Request for the Active PMode XML
   */
  getActivePMode() {
    if (!isNullOrUndefined(PModeComponent.PMODE_URL)) {
      this.http.get(PModeComponent.PMODE_URL + "/" + this.actualId + "?noAudit=true ").subscribe(res => {

        const HTTP_OK = 200;
        if (res.status == HTTP_OK) {
          this.pModeExists = true;
          this.pModeContents = res.text();
        }
      }, err => {
        this.pModeExists = false;
      })
    }
  }

  /**
   * Method called when Upload button is clicked
   */
  upload() {
    if (this.isDirty()) {
      let dialogRef = this.dialog.open(PmodeDirtyUploadComponent);
      dialogRef.afterClosed().subscribe(result => {
        if (result === 'ok') {
          this.http.delete(PModeComponent.PMODE_URL,{params: {ids: JSON.stringify(this.deleteList)}}).subscribe(result => {
              this.deleteList = [];
              this.disableAllButtons();
              this.selected = [];

              let dialogRef = this.dialog.open(PmodeUploadComponent);
              dialogRef.afterClosed().subscribe(result => {
                this.getAllPModeEntries();
              });
              this.uploaded = true;
            },
            error => {
              this.alertService.error("The operation 'update pmodes' not completed successfully.", false);
              this.enableSaveAndCancelButtons();
              this.selected = [];
            });
        } else if (result === 'upload_only') {
          this.deleteList = [];
          let dialogRef = this.dialog.open(PmodeUploadComponent);
          dialogRef.afterClosed().subscribe(result => {
            this.getAllPModeEntries();
          });
          this.uploaded = true;
        }
      });
    } else {
      let dialogRef = this.dialog.open(PmodeUploadComponent);
      dialogRef.afterClosed().subscribe(result => {
        this.getAllPModeEntries();
      });
      this.uploaded = true;
    }
  }

  /**
   * Method called when Download button or icon is clicked
   * @param id The id of the selected entry on the DB
   */
  download(id) {
    if (this.pModeExists) {
      this.http.get(PModeComponent.PMODE_URL + "/" + id).subscribe(res => {
        var uploadDateStr: string = "";
        if(this.selected.length == 1 && this.selected[0].id == id) {
          uploadDateStr = DateFormatService.format(new Date(this.selected[0].configurationDate));
        } else {
          uploadDateStr = DateFormatService.format(new Date(this.tableRows[0].configurationDate));
        }
        PModeComponent.downloadFile(res.text(), uploadDateStr);
      }, err => {
        this.alertService.error(err._body);
      });
    } else {
      this.alertService.error(this.ERROR_PMODE_EMPTY)
    }
  }

  /**
   * Method that checks if CSV Button export can be enabled
   * @returns {boolean} true, if button can be enabled; and false, otherwise
   */
  isSaveAsCSVButtonEnabled() : boolean {
    return this.allPModes.length < AlertComponent.MAX_COUNT_CSV;
  }

  /**
   * Saves the content of the datatable into a CSV file
   */
  saveAsCSV() {
    if (this.isDirty()) {
      this.saveButton(true);
    } else {
      DownloadService.downloadNative(PModeComponent.PMODE_CSV_URL);
    }
  }

  /**
   * Downloader for the XML file
   * @param data
   */
  private static downloadFile(data: any, date: string) {
    const blob = new Blob([data], {type: 'text/xml'});
    let filename: string = "PMode";
    if(date != "") {
      filename += "-"+date;
    }
    filename += ".xml";
    FileSaver.saveAs(blob, filename);
  }

  /**
   * IsDirty method used for the IsDirtyOperations
   * @returns {boolean}
   */
  isDirty(): boolean {
    return !this.disabledCancel;
  }

  /**
   * Method called every time a tab changes
   * @param value Tab Position
   */
  selectedIndexChange(value){
    if(value==1 && this.uploaded) { // Archive Tab
      this.getResultObservable().
      map((response: Response) => response.json()).
      map((response)=>response.slice(this.offset * this.rowLimiter.pageSize, (this.offset + 1) * this.rowLimiter.pageSize)).
      subscribe((response) => {
          this.tableRows = response;
          if(this.offset == 0) {
            this.tableRows[0].current = true;
            this.tableRows[0].description = "[CURRENT]: " + response[0].description;
          }
          this.uploaded = false;
        }, () => {
        },
        () => {
          this.allPModes[0].current = true;
          this.actualId = this.allPModes[0].id;
          this.actualRow = 0;
          this.count = this.allPModes.length;
        });
    }
  }
}


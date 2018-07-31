import {Component, Inject, OnInit, ViewChild} from '@angular/core';
import {MD_DIALOG_DATA, MdDialogRef, MdDialog, MdDialogConfig} from '@angular/material';
import {ColumnPickerBase} from 'app/common/column-picker/column-picker-base';
import {CertificateRo, IdentifierRo, PartyFilteredResult, PartyIdTypeRo, PartyResponseRo, ProcessInfoRo, ProcessRo} from '../party';
import {PartyIdentifierDetailsComponent} from '../party-identifier-details/party-identifier-details.component';
import {PartyService} from '../party.service';
import {AlertService} from '../../alert/alert.service';

@Component({
  selector: 'app-party-details',
  providers: [PartyService],
  templateUrl: './party-details.component.html',
  styleUrls: ['./party-details.component.css']
})
export class PartyDetailsComponent implements OnInit {

  processesRows: ProcessInfoRo[] = [];
  allProcesses: string[];

  identifiersRowColumnPicker: ColumnPickerBase = new ColumnPickerBase();
  processesRowColumnPicker: ColumnPickerBase = new ColumnPickerBase();

  party: PartyResponseRo;
  identifiers: Array<IdentifierRo>;
  selectedIdentifiers = [];
  dateFormat: String = 'yyyy-MM-dd HH:mm:ssZ';

  @ViewChild('fileInput')
  private fileInput;

  constructor (public dialogRef: MdDialogRef<PartyDetailsComponent>,
               @Inject(MD_DIALOG_DATA) public data: any,
               private dialog: MdDialog,
               public partyService: PartyService,
               public alertService: AlertService) {
    this.party = data.edit;
    this.identifiers = this.party.identifiers;
    this.allProcesses = data.allProcesses;

    this.formatProcesses();
  }

  // transform processes to view-model
  private formatProcesses () {
    const processesWithPartyAsInitiator = this.party.processesWithPartyAsInitiator.map(el => el.name);
    const processesWithPartyAsResponder = this.party.processesWithPartyAsResponder.map(el => el.name);
    for (const proc of this.allProcesses) {
      const row = new ProcessInfoRo();
      row.name = proc;
      if (processesWithPartyAsInitiator.indexOf(proc) >= 0)
        row.isInitiator = true;
      if (processesWithPartyAsResponder.indexOf(proc) >= 0)
        row.isResponder = true;

      this.processesRows.push(row);
    }

    this.processesRows.sort((a, b) => {
        if (!!a.isInitiator > !!b.isInitiator) return -1;
        if (!!a.isInitiator < !!b.isInitiator) return 1;
        if (!!a.isResponder > !!b.isResponder) return -1;
        if (!!a.isResponder < !!b.isResponder) return 1;
        if (a.name < b.name) return -1;
        if (a.name > b.name) return 1;
        return 0;
      }
    );
  }

  ngOnInit () {
    this.initColumns();
  }

  uploadCertificate () {
    const fi = this.fileInput.nativeElement;
    const file = fi.files[0];

    const reader = new FileReader();
    reader.onload = (e) => {
      const arrayBuffer = reader.result;
      const array = new Uint8Array(arrayBuffer);
      const binaryString = String.fromCharCode.apply(null, array);
      this.party.certificateContent = binaryString;

      this.partyService.uploadCertificate({content: binaryString}, this.party.name)
        .subscribe(res => {
            this.party.certificate = res;
          },
          err => {
            this.alertService.exception('Error uploading certificate file ' + file.name, err);
          }
        );
    };
    reader.onerror = (err) => {
      this.alertService.exception('Error reding certificate file ' + file.name, err);
    };

    reader.readAsArrayBuffer(file);
  }

  initColumns () {
    this.identifiersRowColumnPicker.allColumns = [
      {
        name: 'Party ID',
        prop: 'partyId',
        width: 100
      },
      {
        name: 'Party Id Type',
        prop: 'partyIdType.name',
        width: 150
      },
      {
        name: 'Party Id value',
        prop: 'partyIdType.value',
        width: 280
      }
    ];
    this.identifiersRowColumnPicker.selectedColumns = this.identifiersRowColumnPicker.allColumns.filter(col => {
      return ['Party ID', 'Party Id Type', 'Party Id value'].indexOf(col.name) != -1
    });
  }

  editIdentifier () {
    const identifierRow = this.selectedIdentifiers[0];
    const rowClone = Object.assign({}, identifierRow);

    const dialogRef: MdDialogRef<PartyIdentifierDetailsComponent> = this.dialog.open(PartyIdentifierDetailsComponent, {
      data: {
        edit: rowClone
      }
    });
    dialogRef.afterClosed().subscribe(ok => {
      const editForm = dialogRef.componentInstance;
      if (ok) {
        const test = rowClone;
        Object.assign(identifierRow, editForm.data.edit);
      }
    });
  }

  removeIdentifier () {
    const identifierRow = this.selectedIdentifiers[0];
    this.party.identifiers.splice(this.party.identifiers.indexOf(identifierRow), 1);
    this.selectedIdentifiers.length = 0;
  }

  addIdentifier () {
    const identifierRow = {entityId: 0, partyId: 'new', partyIdType: {name: '', value: ''}};
    this.party.identifiers.push(identifierRow);
  }

  ok () {
    this.persistProcesses();
    this.party.joinedIdentifiers = this.party.identifiers.map(el => el.partyId).join(', ');
    this.dialogRef.close(true);
  }

  persistProcesses () {
    this.party.processesWithPartyAsInitiator = [];
    this.party.processesWithPartyAsResponder = [];
    const rowsToProcess = this.processesRows.filter(el => el.isResponder || el.isInitiator);

    for (const proc of rowsToProcess) {
      if (proc.isInitiator) {
        this.party.processesWithPartyAsInitiator.push({entityId: 0, name: proc.name})
      }
      if (proc.isResponder) {
        this.party.processesWithPartyAsResponder.push({entityId: 0, name: proc.name})
      }
    }

    // set the string column too
    const initiatorElements = rowsToProcess.filter(el => el.isInitiator && !el.isResponder).map(el => el.name);
    const responderElements = rowsToProcess.filter(el => el.isResponder && !el.isInitiator).map(el => el.name);
    const bothElements = rowsToProcess.filter(el => el.isInitiator && el.isResponder).map(el => el.name);

    this.party.joinedProcesses = ((initiatorElements.length > 0) ? initiatorElements.join('(I), ') + '(I), ' : '')
      + ((responderElements.length > 0) ? responderElements.join('(R), ') + '(R), ' : '')
      + ((bothElements.length > 0) ? bothElements.join('(IR), ') + '(IR)' : '');

    if (this.party.joinedProcesses.endsWith(', '))
      this.party.joinedProcesses = this.party.joinedProcesses.substr(0, this.party.joinedProcesses.length - 2);
  }

  cancel () {
    this.dialogRef.close(false);
  }

  onActivate (event) {
    if ('dblclick' === event.type) {
      this.editIdentifier();
    }
  }

}

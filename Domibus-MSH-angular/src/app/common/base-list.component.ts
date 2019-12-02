import {AlertComponent} from './alert/alert.component';
import {AlertService} from './alert/alert.service';
import {IBaseList} from './ibase-list';
import {DownloadService} from './download.service';
import {OnInit} from '@angular/core';

/**
 * Base class for list components;
 * empty now but common functionality will be added in time
 *
 * @since 4.1
 */
export interface Constructable {
  new(...args);
}

export function ConstructableDecorator(constructor: Constructable) {
}

@ConstructableDecorator
export default class BaseListComponent<T> implements IBaseList<T>, OnInit {

  public rows: T[];
  public count: number;

  constructor(private alertService: AlertService) {
  }

  public get csvUrl(): string {
    return undefined;
  }

  public async saveAsCSV() {
    if (this.hasMethod('saveIfNeeded')) {
      // @ts-ignore
      await this.saveIfNeeded();
    }

    // if (this.getTotalCount() > AlertComponent.MAX_COUNT_CSV) {
    if (this.count > AlertComponent.MAX_COUNT_CSV) {
      this.alertService.error(AlertComponent.CSV_ERROR_MESSAGE);
      return;
    }

    if (this.hasMethod('resetFilters')) {
      // @ts-ignore
      this.resetFilters();
    }

    DownloadService.downloadNative(this.csvUrl);
  }

  private hasMethod(name: string) {
    return this[name] && this[name] instanceof Function;
  }

  ngOnInit(): void {
    this.rows = [];
    this.count = 0;
  }

};




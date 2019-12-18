import {Constructable} from './base-list.component';
import {OnInit, TemplateRef, ViewChild} from '@angular/core';
import {instanceOfModifiableList, instanceOfPageableList} from './type.utils';
import {IFilterableList} from './ifilterable-list';
import {HttpParams} from '@angular/common/http';
import {PaginationType} from './ipageable-list';

/**
 * @author Ion Perpegel
 * @since 4.1
 *
 * A mixin for components that display a list of items that can be filtered
 */
let FilterableListMixin = (superclass: Constructable) => class extends superclass
  implements IFilterableList, OnInit {

  public filter: any;
  public activeFilter: any;

  public advancedSearch: boolean;

  public advancedFilters: Set<string>;

  constructor(...args) {
    super(...args);
    this.filter = {};
    this.advancedFilters = new Set<string>();
  }

  ngOnInit() {
    if (super.ngOnInit) {
      super.ngOnInit();
    }

    this.filter = {};
    this.activeFilter = {};
  }

  /**
   * The method is trying to call the search if the component doesn't have unsaved changes, otherwise raises a popup to the client
   */
  public async tryFilter(): Promise<boolean> {
    const canFilter = await this.canProceedToFilter();
    if (canFilter) {
      this.setActiveFilter();
      this.filterData();
    }
    return canFilter;
  }

  /**
   * The method is supposed to be overridden in derived classes to implement actual search
   */
  public filterData() {
    this.setActiveFilter();

    if (instanceOfPageableList(this)) {
      this.offset = 0;
    }

    return this.loadServerData();
  }

  public onResetAdvancedSearchParams() {
  }

  public resetAdvancedSearchParams() {
    this.advancedFilters.forEach(filterName => {
      if (typeof this.filter[filterName] === 'boolean') {
        this.filter[filterName] = false;
      } else {
        this.filter[filterName] = null;
      }
    });
    this.onResetAdvancedSearchParams();
  }

  protected createAndSetParameters(): HttpParams {
    let filterParams = super.createAndSetParameters();

    Object.keys(this.activeFilter).forEach((key: string) => {
      let value = this.activeFilter[key];
      if (value) {
        if (value instanceof Date) {
          filterParams = filterParams.append(key, value.toISOString());
        } else if (value instanceof Array) {
          value.forEach(el => filterParams = filterParams.append(key, el));
        } else {
          filterParams = filterParams.append(key, value);
        }
      }
    });

    return filterParams;
  }

  /**
   * The method takes the filter params set through widgets and copies them to the active params
   * active params are the ones that are used for actual filtering of data and can be different from the ones set by the user in the UI
   */
  public setActiveFilter() {
    //just in case ngOnInit wasn't called from corresponding component class
    if (!this.activeFilter) {
      this.activeFilter = {};
    }
    Object.assign(this.activeFilter, this.filter);
  }

  /**
   * The method takes the actual filter params and copies them to the UI bound params thus synchronizing the pair so what you see it is what you get
   */
  public resetFilters() {
    this.filter = {};
    Object.assign(this.filter, this.activeFilter);
  }

  private canProceedToFilter(): Promise<boolean> {
    if (instanceOfModifiableList(this) && this.isDirty()) {
      return this.dialogsService.openCancelDialog();
    }
    return Promise.resolve(true);
  }
};

export default FilterableListMixin;

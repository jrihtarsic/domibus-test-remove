import {RowLimiterBase} from '../row-limiter/row-limiter-base';
import {Constructable} from '../base-list.component';

/**
 * @author Ion Perpegel
 * @since 4.1
 * A mixin for components that display a list of items that can be ordered
 * More functionality will be added
 * */

let SortableListMixin = (superclass: Constructable) => class extends superclass {
  public orderBy: string;
  public asc: boolean;

  constructor (...args) {
    super(...args);
  }

  /**
   * The method is abstract so the derived, actual components implement it
   */
  public reload () {
  }

  /**
   * The method is abstract so the derived, actual components implement it
   */
  public onBeforeSort () {
  }

  /**
   * The method is called from grid sorting and it is referred in the grid params as it is visible in the derived components
   */
  public onSort (event) {
    this.doSort(event);
  }

  doSort (event) {
    this.onBeforeSort();

    this.orderBy = event.column.prop;
    this.asc = (event.newValue === 'desc') ? false : true;

    this.reload();
  }
};

export default SortableListMixin;

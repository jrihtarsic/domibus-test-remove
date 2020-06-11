import {HttpClient, HttpParams} from '@angular/common/http';
import {AlertService} from 'app/common/alert/alert.service';
import {Injectable} from '@angular/core';

@Injectable()
export class PropertiesService {
  static readonly PROPERTIES_URL: string = 'rest/configuration/properties';

  regularExpressions: Map<string, RegExp> = new Map<string, RegExp>();

  constructor(private http: HttpClient, private alertService: AlertService) {
  }

  async loadPropertyTypes(): Promise<any> {
    const types = await this.http.get<any[]>(PropertiesService.PROPERTIES_URL + '/metadata/types').toPromise();
    const result = new Map(types.filter(el => el.regularExpression != null).map(i => [i.name, new RegExp(i.regularExpression)]));
    this.regularExpressions = result;
  }

  async getProperty(propName: string): Promise<PropertyModel> {
    const result = await this.http.get<PropertyModel>(PropertiesService.PROPERTIES_URL + '/' + propName).toPromise();
    return result;
  }

  updateProperty(prop: any, isDomain: boolean = true): Promise<void> {
    this.validateValue(prop);

    let value = prop.value;
    if (value === '') { // sanitize empty value: the api needs the body to be present, even if empty
      value = ' ';
    }

    return this.http.put(PropertiesService.PROPERTIES_URL + '/' + prop.name, value, {params: {isDomain: isDomain.toString()}})
      .map(() => {
      }).toPromise()
  }

  validateValue(prop) {
    const propType = prop.type;
    const regexp = this.regularExpressions.get(propType);
    if (!regexp) {
      return;
    }

    if (!regexp.test(prop.value)) {
      throw new Error(`Value '${prop.value}' for property '${prop.name}' is not of type '${prop.type}'`);
    }
  }

  async getUploadSizeLimitProperty(): Promise<PropertyModel> {
    return this.getProperty('domibus.file.upload.maxSize');
  }

  async getCsvMaxRowsProperty(): Promise<PropertyModel> {
    return this.getProperty('domibus.ui.csv.rows.max');
  }

}

export interface PropertyModel {
  value: string;
  name: string;
  type: string;
  usageText: string;
  isComposable: boolean;
  withFallback: boolean;
  clusterAware: boolean;
  section: string;
  description: string;
  module: string;
  writable: boolean;
  encrypted: boolean;
}

export interface PropertyListModel {
  count: number;
  items: PropertyModel[];
}

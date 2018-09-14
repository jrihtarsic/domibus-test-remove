import {Http, Response} from "@angular/http";
import {AlertService} from "app/alert/alert.service";
import {Injectable} from "@angular/core";
import {Observable} from "rxjs/Observable";
import {TrustStoreEntry} from "./trustore.model";

/**
 * @Author Dussart Thomas
 * @Since 3.3
 */

@Injectable()
export class TrustStoreService {

  url = "rest/truststore";
  constructor(private http: Http, private alertService: AlertService) {

  }

  getEntries(): Observable<TrustStoreEntry[]> {
    return this.http.get(this.url + '/list')
      .map(this.extractData)
      .catch(err => this.alertService.handleError(err));
  }

  saveTrustStore(file, password): Observable<Response> {
    let input = new FormData();
    input.append('truststore', file);
    input.append('password', password);
    return this.http.post(this.url + '/save', input);
  }

  private extractData(res: Response) {
    let body = res.json();
    return body || {};
  }

}

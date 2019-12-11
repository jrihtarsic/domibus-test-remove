import {Injectable} from '@angular/core';
import {AuditCriteria, AuditResponseRo} from './audit';
import {Observable} from 'rxjs/Observable';
import {HttpClient, HttpParams} from '@angular/common/http';
import {DownloadService} from '../common/download.service';
import 'rxjs-compat/add/operator/mergeMap';
import 'rxjs-compat/add/observable/from';

/**
 * @author Thomas Dussart
 * @since 4.0
 *
 * In charge of retrieving audit information from the backend.
 */
@Injectable()
export class AuditService {

  constructor(private http: HttpClient) {
  }

  countAuditLogs(searchParams: HttpParams): Observable<number> {
    return this.http.get<number>('rest/audit/count', {params: searchParams});
  }

  // listAuditLogs(auditCriteria: AuditCriteria): Observable<AuditResponseRo[]> {
  //   return this.http.post<AuditResponseRo[]>('rest/audit/list', auditCriteria);
  // }
  //
  // countAuditLogs(auditCriteria: AuditCriteria): Observable<number> {
  //   return this.http.post<number>('rest/audit/count', auditCriteria);
  // }

  listTargetTypes(): Observable<string[]> {
    return this.http.get<string[]>('rest/audit/targets');
  }

  listActions(): Observable<string> {
    return Observable.from(['Created', 'Modified', 'Deleted', 'Downloaded', 'Resent', 'Moved']);
  }

}

import {UserResponseRO} from './user';
import {Injectable} from '@angular/core';
import {Http, Response} from '@angular/http';
import {AlertService} from '../alert/alert.service';
import {Observable} from 'rxjs/Observable';
import {SecurityService} from '../security/security.service';
import {DomainService} from '../security/domain.service';

@Injectable()
export class UserService {

  constructor (private http: Http,
               private alertService: AlertService,
               private securityService: SecurityService,
               private domainService: DomainService) {
  }

  getUsers (filter: UserSearchCriteria): Observable<UserResponseRO[]> {
    return this.http.get('rest/user/users')
      .map(this.extractData)
      .filter(this.filterData(filter))
      .catch(err => this.alertService.handleError(err));
  }

  getUserNames (): Observable<string> {
    return this.http.get('rest/user/users')
      .flatMap(res => res.json())
      .map((user: UserResponseRO) => user.userName)
      .catch(err => this.alertService.handleError(err));
  }

  getUserRoles (): Observable<String[]> {
    return this.http.get('rest/user/userroles')
      .map(this.extractData)
      .catch(err => this.alertService.handleError(err));
  }

  deleteUsers (users: Array<UserResponseRO>): void {
    this.http.post('rest/user/delete', users).subscribe(res => {
      this.alertService.success('User(s) deleted', false);
    }, err => {
      this.alertService.error(err, false);
    });
  }

  async isDomainVisible (): Promise<boolean> {
    const isMultiDomain = await this.domainService.isMultiDomain().toPromise();
    return isMultiDomain && this.securityService.isCurrentUserSuperAdmin();
  }

  private extractData (res: Response) {
    const result = res.json() || {};
    return result;
  }

  private filterData (filter: UserSearchCriteria) {
    return function (users) {
      let results = users.slice();
      if (filter.deleted != null) {
        results = users.filter(el => el.deleted === filter.deleted)
      }
      users.length = 0;
      users.push(...results);
      return users;
    }
  }

}

export class UserSearchCriteria {
  authRole: string;
  userName: string;
  deleted: boolean;
}


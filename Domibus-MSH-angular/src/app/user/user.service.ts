import {UserResponseRO} from './user';
import {Injectable} from '@angular/core';
import {Http, Response} from '@angular/http';
import {AlertService} from '../alert/alert.service';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class UserService {

  constructor (private http: Http, private alertService: AlertService) {

  }

  getUsers (): Observable<UserResponseRO[]> {
    return this.http.get('rest/user/users')
      .map(this.extractData)
      // temporary mock
      .map(this.mockDomain)
      .catch(this.handleError);
  }

  getUserNames (): Observable<string> {
    return this.http.get('rest/user/users')
      .flatMap(res => res.json())
      .map((user: UserResponseRO) => user.userName);
  }

  getUserRoles (): Observable<String[]> {
    return this.http.get('rest/user/userroles')
      .map(this.extractData)
      .catch(this.handleError);
  }

  getUserDomains (): Observable<String[]> {
    return this.http.get('rest/user/userroles')
      .map(this.mockDomains)
      .catch(this.handleError);
  }

  deleteUsers (users: Array<UserResponseRO>): void {
    this.http.post('rest/user/delete', users).subscribe(res => {
      this.alertService.success('User(s) deleted', false);
    }, err => {
      this.alertService.error(err, false);
    });
  }

  saveUsers (users: Array<UserResponseRO>): void {
    this.http.post('rest/user/save', users).subscribe(res => {
      this.changeUserStatus(users);
      this.alertService.success('User saved', false);
    }, err => {
      this.alertService.error(err, false);
    });
  }

  changeUserStatus (users: Array<UserResponseRO>) {
    for (let u in users) {
      users[u].status = 'PERSISTED';
      users[u].password = '';
    }
  }

  private extractData (res: Response) {
    const body = res.json();
    return body || {};
  }

  private handleError (error: Response | any) {
    this.alertService.error(error, false);
    let errMsg: string;
    if (error instanceof Response) {
      const body = error.json() || '';
      const err = body.error || JSON.stringify(body);
      errMsg = `${error.status} - ${error.statusText || ''} ${err}`;
    } else {
      errMsg = error.message ? error.message : error.toString();
    }
    console.error(errMsg);
    return Promise.reject(errMsg);
  }

  // temporary mock
  private mockDomains (res: Response) {
    return ['MyDomain1', 'MyDomain2', 'MyDomain3'];
  }

  private mockDomain (users: any[]) {
    for (const u of users) {
      if (u.userName == 'admin' || u.userName == 'user')
        u.domain = 'MyDomain1';
      else
        u.domain = 'MyDomain2';
    }
    return users;
  }

}

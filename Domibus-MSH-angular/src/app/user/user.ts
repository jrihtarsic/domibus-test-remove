export enum UserState {
  PERSISTED,
  NEW,
  UPDATED
}

export class UserResponseRO {
  userName: string;
  email: string;
  password: string;
  active: boolean;
  authorities: Array<string>;
  roles: string = '';
  domain: string = null;
  status: string;
  suspended: boolean;

  constructor (userName: string, email: string, password: string, active: boolean, status: string, authorities: Array<string>, suspended: boolean, domain: string) {
    this.userName = userName;
    this.email = email;
    this.password = password;
    this.status = status;
    this.active = active;
    this.suspended = suspended;
    this.authorities = authorities;
    for (let authority in authorities) {
      this.roles = this.roles.concat(authorities[authority]).concat(' ');
    }
    this.domain = domain;
  }
}

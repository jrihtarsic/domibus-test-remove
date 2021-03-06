﻿import {Injectable} from '@angular/core';
import {NavigationEnd, NavigationStart, Router} from '@angular/router';
import {Observable} from 'rxjs';
import {Subject} from 'rxjs/Subject';
import {Response} from '@angular/http';

@Injectable()
export class AlertService {
  private subject = new Subject<any>();
  private previousRoute: string;
  private needsExplicitClosing: boolean;

  // TODO move the logic in the ngInit block
  constructor(private router: Router) {
    this.previousRoute = '';
    // clear alert message on route change
    router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        if (this.isRouteChanged(event.url)) {
          // console.log('Clearing alert when navigating from [' + this.previousRoute + '] to [' + event.url + ']');
          if (!this.needsExplicitClosing) {
            this.clearAlert();
          }
        } else {
          // console.log('Alert kept when navigating from [' + this.previousRoute + '] to [' + event.url + ']');
        }
      } else if (event instanceof NavigationEnd) {
        const navigationEnd: NavigationEnd = event;
        this.previousRoute = navigationEnd.url;
      }
    });
  }

  // called from the alert component explicitly by the user
  public close(): void {
    this.subject.next();
  }

  public clearAlert(): void {
    if (this.needsExplicitClosing) {
      return;
    }
    this.close();
  }

  public success(message: string, keepAfterNavigationChange = false) {
    this.needsExplicitClosing = keepAfterNavigationChange;
    this.subject.next({type: 'success', text: message});
  }

  public error(message: Response | string | any, keepAfterNavigationChange = false,
               fadeTime: number = 0) {
    if (message.handled) return;
    if (message instanceof Response && (message.status === 401 || message.status === 403)) return;
    if (message.toString().indexOf('Response with status: 403 Forbidden') >= 0) return;

    this.needsExplicitClosing = keepAfterNavigationChange;
    const errMsg = this.formatError(message);
    this.subject.next({type: 'error', text: errMsg});
    if (fadeTime) {
      setTimeout(() => this.clearAlert(), fadeTime);
    }
  }

  public exception(message: string, error: any, keepAfterNavigationChange = false,
                   fadeTime: number = 0) {
    const errMsg = this.formatError(error, message);
    this.error(errMsg, keepAfterNavigationChange, fadeTime);
  }

  public getMessage(): Observable<any> {
    return this.subject.asObservable();
  }

  public handleError(error: Response | any) {

    this.error(error, false);

    let errMsg: string;
    if (error instanceof Response) {
      const body = error.headers && error.headers.get('content-type') !== 'text/html;charset=utf-8' ? error.json() || '' : error.toString();
      const err = body.error || JSON.stringify(body);
      errMsg = `${error.status} - ${error.statusText || ''} ${err}`;
    } else {
      errMsg = error.message ? this.escapeHtml(error.message) : error.toString();
    }
    console.error(errMsg);
    return Promise.reject({reason: errMsg, handled: true});
  }

  private getPath(url: string): string {
    var parser = document.createElement('a');
    parser.href = url;
    return parser.pathname;
  }

  private isRouteChanged(currentRoute: string): boolean {
    let result = false;
    const previousRoutePath = this.getPath(this.previousRoute);
    const currentRoutePath = this.getPath(currentRoute);
    if (previousRoutePath !== currentRoutePath) {
      result = true;
    }
    return result;
  }

  escapeHtml(unsafe: string): string {
    if (!unsafe) return '';
    if (!unsafe.replace) unsafe = unsafe.toString();
    return unsafe.replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  private formatError(error: Response | string | any, message: string = null): string {
    let errMsg: string = typeof error === 'string' ? error : error.message ? this.escapeHtml(error.message) : null;

    if (!errMsg) {
      try {
        if (error.headers && error.headers.get('content-type') !== 'text/html;charset=utf-8') {
          if (error.json) {
            if (error.json().hasOwnProperty('message')) {
              errMsg = this.escapeHtml(error.json().message);
            } else {
              errMsg = error.json().toString();
            }
          } else {
            errMsg = error._body;
          }
        } else {
          errMsg = error._body ? error._body.match(/<h1>(.+)<\/h1>/)[1] : error;
        }
      } catch (e) {
      }
    }
    if (errMsg && errMsg.replace) {
      errMsg = errMsg.replace('Uncaught (in promise):', '');
      errMsg = errMsg.replace('[object ProgressEvent]', '');
    }
    return (message ? message + ' \n' : '') + (errMsg || '');
  }

}

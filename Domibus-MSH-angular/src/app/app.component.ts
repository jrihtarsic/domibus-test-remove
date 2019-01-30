import {Component, OnInit, ViewChild} from '@angular/core';
import {SecurityService} from './security/security.service';
import {NavigationStart, Router, RouterOutlet} from '@angular/router';
import {SecurityEventService} from './security/security.event.service';
import {Http, Response} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import {DomainService} from './security/domain.service';
import {HttpEventService} from './common/http/http.event.service';
import {ReplaySubject} from "rxjs";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  fullMenu: boolean = true;
  menuClass: string = this.fullMenu ? 'menu-expanded' : 'menu-collapsed';
  fourCornerEnabled: boolean = true;
  extAuthProviderEnabled: boolean = false;

  @ViewChild(RouterOutlet)
  outlet: RouterOutlet;

  constructor (private securityService: SecurityService,
               private router: Router,
               private securityEventService: SecurityEventService,
               private http: Http,
               private httpEventService: HttpEventService,
               private domainService: DomainService) {

    this.domainService.setAppTitle();
  }

  ngOnInit () {
    this.readFourCornerEnabled();
    this.readExtAuthProviderEnabled();

    this.httpEventService.subscribe((error) => {
      if (error && (error.status === 403 || error.status === 401)) {
        console.log('Received forbidden request event');
        this.securityService.logout();
      }
    });

    this.securityEventService.onLogoutSuccessEvent().subscribe(
      data => {
        this.router.navigate([this.isExtAuthProviderEnabled() ? '/logout' : '/login']);
      });

    //TODO to be addressed by UI refactoring
    this.router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        if (event.url.startsWith('/login')) {
          const currentUser = this.securityService.getCurrentUser();
          this.securityService.isAuthenticated(true).subscribe((isAuthenticated: boolean) => {
            if (isAuthenticated) {
              console.log('going to /');
              this.router.navigate(['/']);
            }
          });
        }
      }
    });
  }

  isAdmin (): boolean {
    return this.securityService.isCurrentUserAdmin();
  }

  isUser (): boolean {
    return !!this.currentUser;
  }

  isExtAuthProviderEnabled (): boolean {
    return this.extAuthProviderEnabled;
  }

  get currentUser (): string {
    const user = this.securityService.getCurrentUser();
    return user ? user.username : '';
  }

  logout(event: Event): void {
    event.preventDefault();
    console.log('do the logout');
    this.router.navigate([this.isExtAuthProviderEnabled() ? '/logout' : '/login']).then((ok) => {
      if (ok) {
        this.securityService.logout();
      }
    }).catch((error) => {
      console.log('navigate error: ' + error);
    })
  }

  toggleMenu () {
    this.fullMenu = !this.fullMenu
    this.menuClass = this.fullMenu ? 'menu-expanded' : 'menu-collapsed'
    setTimeout(() => {
      var evt = document.createEvent('HTMLEvents')
      evt.initEvent('resize', true, false)
      window.dispatchEvent(evt)
    }, 500)
    //ugly hack but otherwise the ng-datatable doesn't resize when collapsing the menu
    //alternatively this can be tried (https://github.com/swimlane/ngx-datatable/issues/193) but one has to implement it on every page
    //containing a ng-datatable and it only works after one clicks inside the table
  }

  changePassword() {
    this.router.navigate(['changePassword']);
  }

  //read four corner enabled property
  private readFourCornerEnabled() {
    this.readApplicationProperty('fourcornerenabled')
      .subscribe((appProperty: boolean) => {
        this.fourCornerEnabled = appProperty;
        console.log('fourCornerEnabled read:' + this.fourCornerEnabled);
      }, (appProperty: boolean) => {
        console.log('readFourCornerEnabled error' + appProperty);
      });
  }

  //read external authentication provider enabled
  // property and if true do the login
  private readExtAuthProviderEnabled() {
    this.readApplicationProperty('extauthproviderenabled')
      .subscribe((appProperty: boolean) => {
        this.extAuthProviderEnabled = appProperty;
        console.log('extauthproviderenabled read:' + this.extAuthProviderEnabled);
        if (this.extAuthProviderEnabled) {
          this.securityService.login_extauthprovider();
        }
      }, (appProperty: boolean) => {
        console.log('readExtAuthProviderEnabled error' + appProperty);
      });
  }

  readApplicationProperty(propertyName: string): Observable<boolean> {
    const subject = new ReplaySubject();
    this.http.get(`rest/application/${propertyName}`)
      .subscribe((res: Response) => {
        subject.next(res.json());
      }, (error: any) => {
        subject.next(null);
      });
    return subject.asObservable();
  }

}

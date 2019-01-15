import {Component, OnInit, ViewChild} from '@angular/core';
import {SecurityService} from './security/security.service';
import {NavigationStart, Router, RouterOutlet} from '@angular/router';
import {SecurityEventService} from './security/security.event.service';
import {Http, Response} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import {DomainService} from './security/domain.service';
import {HttpEventService} from './http/http.event.service';
import {User} from "./security/user";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  fullMenu: boolean = true;
  menuClass: string = this.fullMenu ? 'menu-expanded' : 'menu-collapsed';
  fourCornerEnabled: boolean = true;

  @ViewChild(RouterOutlet)
  outlet: RouterOutlet;

  constructor (private securityService: SecurityService,
               private router: Router,
               private securityEventService: SecurityEventService,
               private http: Http,
               private httpEventService: HttpEventService,
               private domainService: DomainService) {

    this.domainService.setAppTitle();

    const fourCornerModelResponse: Observable<Response> = this.http.get('rest/application/fourcornerenabled');

    fourCornerModelResponse.subscribe((name: Response) => {
      this.fourCornerEnabled = name.json();
    });
  }

  ngOnInit () {
    this.httpEventService.subscribe((error) => {
      if (error && (error.status === 403 || error.status === 401)) {
        console.log('Received forbidden request event');
        this.securityService.logout();
      }
    });

    this.securityEventService.onLogoutSuccessEvent().subscribe(
      data => {
        this.router.navigate(['/login']);
      });

    this.router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        if (event.url == '/login') {
          const currentUser = this.securityService.getCurrentUser();
          if (!!currentUser) {
            this.router.navigate(['/']);
          }
        }
      }
    });

    //first time redirect from auth external provider
    if (this.isAuthExternalProviderEnabled()) {
      console.log('auth external provider = true: ');

      //get the user from server and write it in local storage
      this.securityService.getCurrentUserFromServer()
        .subscribe((user: User) => {
          if (user) {
            this.securityService.updateCurrentUser(user);
            this.domainService.setAppTitle();
          }
        }, (user: User) => {
          console.log('getCurrentUserFromServer error' + user);
          return user;
        });
    }


  }

  isAuthExternalProviderEnabled(): boolean {
    let params = new URLSearchParams(window.location.search);
    let ticketParam = params.get('ticket');
    return ticketParam ? ticketParam.indexOf("ST") >= 0 : false;
  }

  isAdmin (): boolean {
    return this.securityService.isCurrentUserAdmin();
  }

  isUser (): boolean {
    return !!this.currentUser;
  }

  isUserFromExternalAuthProvider (): boolean {
    //console.log('isUserFromExternalAuthProvider: ' + this.securityService.isUserFromExternalAuthProvider());
    return this.securityService.isUserFromExternalAuthProvider();
  }

  get currentUser (): string {
    const user = this.securityService.getCurrentUser();
    return user ? user.username : '';
  }

  logout (event: Event): void {
    event.preventDefault();
    if (this.isUserFromExternalAuthProvider()) {
      console.log('going to logout from external auth provider');
      this.router.navigate(['/logout']).then((ok) => {

        if (ok) {
          this.securityService.logout();
        }
      })
    } else {
      this.router.navigate(['/login'], {queryParams: {force: true}}).then((ok) => {
        if (ok) {
          this.securityService.logout();
        }
      })
    }

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
}

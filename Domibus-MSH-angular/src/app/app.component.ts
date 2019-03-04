import {Component, OnInit, ViewChild} from '@angular/core';
import {SecurityService} from './security/security.service';
import {Router, RouterOutlet} from '@angular/router';
import {SecurityEventService} from './security/security.event.service';
import {Http} from '@angular/http';
import {DomainService} from './security/domain.service';
import {HttpEventService} from './common/http/http.event.service';
import {DomibusInfoService} from "./common/appinfo/domibusinfo.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  fullMenu: boolean = true;
  menuClass: string = this.fullMenu ? 'menu-expanded' : 'menu-collapsed';
  extAuthProviderEnabled: boolean = false;

  @ViewChild(RouterOutlet)
  outlet: RouterOutlet;

  constructor (private securityService: SecurityService,
               private router: Router,
               private securityEventService: SecurityEventService,
               private http: Http,
               private httpEventService: HttpEventService,
               private domainService: DomainService,
               private domibusInfoService: DomibusInfoService) {

    this.domainService.setAppTitle();
  }

  async ngOnInit () {

    this.extAuthProviderEnabled = await this.domibusInfoService.isExtAuthProviderEnabled();
    if (this.extAuthProviderEnabled) {
      this.securityService.login_extauthprovider();
      console.log('going to redirect to /messagelog');
      //just redirect to context path
      this.router.navigate(['/messagelog']);
    }

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
  }

  isAdmin (): boolean {
    return this.securityService.isCurrentUserAdmin();
  }

  isUser (): boolean {
    return this.securityService.hasCurrentUserPrivilegeUser();
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

    this.securityService.logout();
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

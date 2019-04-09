﻿import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {SecurityService} from '../../security/security.service';
import {DomibusInfoService} from "../appinfo/domibusinfo.service";

/**
 * It will handle for each route where is defined:
 * - authentication
 * - authorization - only if the route has data: checkRoles initialized
 */
@Injectable()
export class AuthenticatedAuthorizedGuard implements CanActivate {

  constructor(private router: Router, private securityService: SecurityService, private domibusInfoService: DomibusInfoService) {
  }

  async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    let canActivate = false;

    const isUserFromExternalAuthProvider = await this.domibusInfoService.isExtAuthProviderEnabled();
    const isAuthenticated = await this.securityService.isAuthenticated();
    console.log('AuthenticatedAuthorizedGuard - isAuthenticated=' + isAuthenticated);

    if (isAuthenticated) {
      canActivate = true;

      //check also authorization
      const allowedRoles = route.data.checkRoles;
      if (!!allowedRoles) { //only if there are roles to check
        const isAuthorized = this.securityService.isAuthorized(allowedRoles);
        console.log('AuthenticatedAuthorizedGuard - isAuthorized=' + isAuthorized);
        if (!isAuthorized) {
          canActivate = false;

          this.router.navigate([isUserFromExternalAuthProvider ? '/notAuthorized' : '/']);
        }
      }
    } else {
      // not logged in so redirect to login page with the return url
      // todo: the call to clear is not cohesive, should refactor
      this.securityService.clearSession();
      // todo: the redirect is duplicated, should refactor
      if (!isUserFromExternalAuthProvider) {
        this.router.navigate(['/login'], {queryParams: {returnUrl: state.url}});
      } else {
        //ECAS redirect to logout
        this.router.navigate(['/logout']);
      }
    }
    return canActivate;
  }

}

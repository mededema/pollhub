import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private keycloakInstance: Keycloak | null = null;
  private authenticated = false;

  async init(): Promise<boolean> {
    this.keycloakInstance = new Keycloak({
      url: 'http://localhost:8082',
      realm: 'pollhub',
      clientId: 'pollhub-frontend'
    });

    try {
      this.authenticated = await this.keycloakInstance.init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
        checkLoginIframe: false
      });
      return this.authenticated;
    } catch (error) {
      console.error('Erreur initialisation Keycloak', error);
      return false;
    }
  }

  isLoggedIn(): boolean {
    return this.authenticated;
  }

  getUsername(): string {
    return this.keycloakInstance?.tokenParsed?.['preferred_username'] || 'Anonyme';
  }

  getToken(): Promise<string> {
    return new Promise((resolve, reject) => {
      if (!this.keycloakInstance) {
        return resolve('');
      }
      this.keycloakInstance.updateToken(30)
        .then(() => resolve(this.keycloakInstance?.token || ''))
        .catch(() => {
          this.login();
          reject('Token expiré');
        });
    });
  }

  login(): void {
    this.keycloakInstance?.login();
  }

  logout(): void {
    this.keycloakInstance?.logout({ redirectUri: window.location.origin });
  }
}

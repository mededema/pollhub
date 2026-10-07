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
      url: window.location.port === '4200' ? 'http://localhost:8082' : window.location.origin + '/auth',
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

  // Version ultra-sécurisée qui renvoie le token instantanément sans bloquer l'application
  async getToken(): Promise<string> {
    if (this.keycloakInstance && this.keycloakInstance.token) {
      return this.keycloakInstance.token;
    }
    return '';
  }

  login(): void {
    this.keycloakInstance?.login();
  }

  logout(): void {
    this.keycloakInstance?.logout({ redirectUri: window.location.origin });
  }
}

import { Component, OnInit, ViewEncapsulation, AfterContentChecked } from '@angular/core';
import { NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { Account, LoginModalService, Principal, LoginService } from '../shared';

@Component({
    selector: 'jhi-home',
    templateUrl: './home.component.html',
    styleUrls: [
        'home.css'
    ],
    encapsulation: ViewEncapsulation.None
})
export class HomeComponent implements OnInit, AfterContentChecked {
    account: Account;
    modalRef: NgbModalRef;
    gateway_jwt_link: string;
    cumulosity_jwt_link: string;
    hostName: string;

    constructor(
        private principal: Principal,
        private loginModalService: LoginModalService,
        private loginService: LoginService,
        private eventManager: JhiEventManager
    ) {
        this.gateway_jwt_link = '';
        this.hostName = window.location.hostname;
    }

    ngOnInit() {
        this.principal.identity().then((account) => {
            this.account = account;
        });
        this.registerAuthenticationSuccess();
    }

    ngAfterContentChecked() {
        this.updateJwt();
    }
    registerAuthenticationSuccess() {
        this.eventManager.subscribe('authenticationSuccess', (message) => {
            this.principal.identity().then((account) => {
                this.account = account;
                this.updateJwt();
            });
        });
    }

    private updateJwt() {
        if (this.isAuthenticated() === true) {
            const path = '/#/token?accesstoken=';
            const hostPort = 'http://' + this.hostName + ':8080';
            this.gateway_jwt_link = hostPort + path + this.loginService.getToken();
            this.cumulosity_jwt_link = 'http://' + this.hostName + ':9090' + path + this.loginService.getToken();
        }
    }
    isAuthenticated() {
        return this.principal.isAuthenticated();
    }

    login() {
        this.modalRef = this.loginModalService.open();
    }
}

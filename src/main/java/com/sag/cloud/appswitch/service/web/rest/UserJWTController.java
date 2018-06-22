package com.sag.cloud.appswitch.service.web.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sag.cloud.appswitch.service.security.jwt.JWTConfigurer;
import com.sag.cloud.appswitch.service.security.jwt.TokenProvider;
import com.sag.cloud.appswitch.service.web.rest.vm.LoginVM;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.sag.cloud.appswitch.service.web.rest.vm.UserInfo;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import java.util.Arrays;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
public class UserJWTController {

    private final TokenProvider tokenProvider;

    private final AuthenticationManager authenticationManager;

    public UserJWTController(TokenProvider tokenProvider, AuthenticationManager authenticationManager) {
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    //    @PostMapping("/authenticate")
    @Timed
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM) {

        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());

        Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        boolean rememberMe = (loginVM.isRememberMe() == null) ? false : loginVM.isRememberMe();
        String jwt = tokenProvider.createToken(authentication, rememberMe);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWTConfigurer.AUTHORIZATION_HEADER, "Bearer " + jwt);
        return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
    }


    //    @PostMapping("/keycloak/authenticate")
    @PostMapping("/authenticate")
    @Timed
    public ResponseEntity<JWTToken> generateToken(@Valid @RequestBody LoginVM loginVM) {


        String jwt = getTokenFromKeyCloak(loginVM.getUsername(), loginVM.getPassword());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWTConfigurer.AUTHORIZATION_HEADER, "Bearer " + jwt);
        return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
    }

    private String getTokenFromKeyCloak(String userName, String password) {

        RestTemplate restTemplate = new RestTemplate();
        // HttpHeaders
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(new MediaType[]{MediaType.ALL}));
        // Request to return JSON format
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);


        // HttpEntity<String>: To get result as String.
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();

        map.add("username", userName);
        map.add("password", password);
        map.add("client_id", "web_app");
        map.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        ResponseEntity<KeycloakToken> authTokenResponse = restTemplate.postForEntity("http://localhost:9080/auth/realms/jhipster/protocol/openid-connect/token", request, KeycloakToken.class);
        KeycloakToken keycloakToken = authTokenResponse.getBody();


        return keycloakToken.getAccess_token();

    }


    /**
     * {
     * "access_token": "ksfjksfk .lskfjs. sfjsf",
     * "expires_in": 300,
     * "refresh_expires_in": 1800,
     * "refresh_token": "sfsfsf.sfsf.sfsfs",
     * "token_type": "bearer",
     * "not-before-policy": 0,
     * "session_state": "1cab4895-db34-4589-bb54-405bc5f020d3"
     * }
     */

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KeycloakToken {

        private String access_token;

        @JsonProperty("access_token")
        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class JWTToken {

        private String idToken;

        JWTToken(String idToken) {
            this.idToken = idToken;
        }

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }

        void setIdToken(String idToken) {
            this.idToken = idToken;
        }
    }
}

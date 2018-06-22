package com.sag.cloud.appswitch.service.security.jwt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sag.cloud.appswitch.service.service.dto.UserDTO;
import com.sag.cloud.appswitch.service.web.rest.UserJWTController;
import io.github.jhipster.config.JHipsterProperties;

import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import com.sag.cloud.appswitch.service.web.rest.vm.UserInfo;

import io.jsonwebtoken.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class TokenProvider {

    private final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    private static final String AUTHORITIES_KEY = "auth";

    private String secretKey;

    private long tokenValidityInMilliseconds;

    private long tokenValidityInMillisecondsForRememberMe;

    private final JHipsterProperties jHipsterProperties;

    public TokenProvider(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @PostConstruct
    public void init() {
        this.secretKey =
            jHipsterProperties.getSecurity().getAuthentication().getJwt().getSecret();

        this.tokenValidityInMilliseconds =
            1000 * jHipsterProperties.getSecurity().getAuthentication().getJwt().getTokenValidityInSeconds();
        this.tokenValidityInMillisecondsForRememberMe =
            1000 * jHipsterProperties.getSecurity().getAuthentication().getJwt().getTokenValidityInSecondsForRememberMe();
    }

    public String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity;
        if (rememberMe) {
            validity = new Date(now + this.tokenValidityInMillisecondsForRememberMe);
        } else {
            validity = new Date(now + this.tokenValidityInMilliseconds);
        }

        return Jwts.builder()
            .setSubject(authentication.getName())
            .claim(AUTHORITIES_KEY, authorities)
            .signWith(SignatureAlgorithm.HS512, secretKey)
            .setExpiration(validity)
            .compact();
    }

    public String createTokenService(UserInfo userInfo, boolean rememberMe) {
//        String authorities = userInfo.getAuthorities().stream()
//            .map(GrantedAuthority::getAuthority)
//            .collect(Collectors.joining(","));

        String authorities = "";


        //At present hardcoded
        switch (userInfo.getUsername()) {
            case "admin":
                authorities = "ROLE_ADMIN,ROLE_USER";
                break;
            case "user":
                authorities = "ROLE_USER";
                break;
        }

        long now = (new Date()).getTime();
        Date validity;
        if (rememberMe) {
            validity = new Date(now + this.tokenValidityInMillisecondsForRememberMe);
        } else {
            validity = new Date(now + this.tokenValidityInMilliseconds);
        }

        return Jwts.builder()
            .setSubject(userInfo.getUsername())
            .claim(AUTHORITIES_KEY, authorities)
            .signWith(SignatureAlgorithm.HS512, "my-secret-token-to-change-in-production")
            .setExpiration(validity)
            .compact();
    }

//    public Authentication getAuthentication(String token) {
//        Claims claims = Jwts.parser()
//            .setSigningKey(secretKey)
//            .parseClaimsJws(token)
//            .getBody();
//
//        Collection<? extends GrantedAuthority> authorities =
//            Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
//                .map(SimpleGrantedAuthority::new)
//                .collect(Collectors.toList());
//
//        User principal = new User(claims.getSubject(), "", authorities);
//
//        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
//    }
    public Authentication getAuthentication(String token) {
//        Claims claims = Jwts.parser()
//            .setSigningKey(secretKey)
//            .parseClaimsJws(token)
//            .getBody();

        UserInfoKeycloak keyCloakUserInfo = getKeyCloakUserInfo(token);
        Collection<? extends GrantedAuthority> authorities =
           keyCloakUserInfo.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        User principal = new User(keyCloakUserInfo.getSub(), "", authorities);

        return new UsernamePasswordAuthenticationToken(convertToUserDTO(keyCloakUserInfo), token, authorities);
    }


    private UserDTO convertToUserDTO(UserInfoKeycloak userInfoKeycloak){

        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName(userInfoKeycloak.getPreferred_username());
        userDTO.setId(Long.valueOf(userInfoKeycloak.getSub().hashCode()));
        userDTO.setLangKey("en");
        userDTO.setEmail(userInfoKeycloak.getEmail());
        userDTO.setLastName(userInfoKeycloak.getFamily_name());
        userDTO.setAuthorities(new HashSet<String>(userInfoKeycloak.getRoles()));
        userDTO.setLogin(userInfoKeycloak.getPreferred_username());


        return userDTO;

    }

    private UserInfoKeycloak getKeyCloakUserInfo(String jwt) {

        RestTemplate restTemplate = new RestTemplate();
        // HttpHeaders
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(new MediaType[]{MediaType.ALL}));
        // Request to return JSON format
        headers.set("Authorization","Bearer "+jwt);
        HttpEntity<String> entity = new HttpEntity<String>(headers);

        ResponseEntity<UserInfoKeycloak> userInfoKeycloakEntity = restTemplate.exchange("http://localhost:9080/auth/realms/jhipster/protocol/openid-connect/userinfo", HttpMethod.GET,entity,UserInfoKeycloak.class);
        UserInfoKeycloak userInfoKeycloak = userInfoKeycloakEntity.getBody();


        return userInfoKeycloak;

    }
    /**
     {
     "sub": "347e82fe-f145-4b8e-ba56-35ae4c71b3ee",
     "roles": [
     "offline_access",
     "uma_authorization",
     "ROLE_USER"
     ],
     "name": "User",
     "preferred_username": "user",
     "given_name": "",
     "family_name": "User",
     "email": "user@localhost"
     }
     */
    static class UserInfoKeycloak {
        private  String sub;
        private  ArrayList<String> roles;
        private String name;
        private String given_name;
        private String family_name;
        private String email;
        private String preferred_username;

        @JsonProperty("sub")
        public String getSub() {
            return sub;
        }

        public void setSub(String sub) {
            this.sub = sub;
        }

        @JsonProperty("roles")
        public ArrayList<String> getRoles() {
            return roles;
        }

        public void setRoles(ArrayList<String> roles) {
            this.roles = roles;
        }

        @JsonProperty("name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonProperty("given_name")
        public String getGiven_name() {
            return given_name;
        }

        public void setGiven_name(String given_name) {
            this.given_name = given_name;
        }

        @JsonProperty("family_name")
        public String getFamily_name() {
            return family_name;
        }

        public void setFamily_name(String family_name) {
            this.family_name = family_name;
        }

        @JsonProperty("email")
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @JsonProperty("preferred_username")
        public String getPreferred_username() {
            return preferred_username;
        }

        public void setPreferred_username(String preferred_username) {
            this.preferred_username = preferred_username;
        }
    }
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            log.info("Invalid JWT signature.");
            log.trace("Invalid JWT signature trace: {}", e);
        } catch (MalformedJwtException e) {
            log.info("Invalid JWT token.");
            log.trace("Invalid JWT token trace: {}", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token.");
            log.trace("Expired JWT token trace: {}", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token.");
            log.trace("Unsupported JWT token trace: {}", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT token compact of handler are invalid.");
            log.trace("JWT token compact of handler are invalid trace: {}", e);
        }
        return false;
    }
}

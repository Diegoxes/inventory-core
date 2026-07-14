package com.smarthome.security.jwt;

import com.smarthome.security.SessionPrincipal;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/** Carga permisos Spring Security a partir del JWT parseado. */
public interface JwtAuthorityLoader {
    Collection<? extends GrantedAuthority> loadAuthorities(SessionPrincipal session);
}

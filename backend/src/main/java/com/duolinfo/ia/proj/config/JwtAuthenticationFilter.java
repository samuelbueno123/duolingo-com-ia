package com.duolinfo.ia.proj.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.exception.ResourceNotFoundException;
import com.duolinfo.ia.proj.service.AuthService;
import com.duolinfo.ia.proj.service.JwtService;
import com.duolinfo.ia.proj.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;
    private final AuthService authService;
    private final UserAuthCache userAuthCache;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserService userService,
            AuthService authService,
            UserAuthCache userAuthCache) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.authService = authService;
        this.userAuthCache = userAuthCache;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = null;
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7).trim();
        } else if (request.getParameter("token") != null) {
            token = request.getParameter("token").trim();
        }

        if (token != null && !token.isBlank()) {
            authenticate(token);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        if (token.isBlank() || !jwtService.isAccessTokenValid(token)) {
            return;
        }

        var userIdOpt = jwtService.getUserIdFromToken(token);
        User user = null;
        List<SimpleGrantedAuthority> authorities = null;

        if (userIdOpt.isPresent()) {
            Long userId = userIdOpt.get();
            var cached = userAuthCache.getById(userId);
            if (cached != null) {
                user = cached.user();
                authorities = cached.authorities();
            } else {
                try {
                    user = userService.findById(userId);
                } catch (ResourceNotFoundException exception) {
                    return;
                }
            }
        } else {
            String email = jwtService.getEmailFromToken(token);
            var cached = userAuthCache.getByEmail(email);
            if (cached != null) {
                user = cached.user();
                authorities = cached.authorities();
            } else {
                user = userService.findByEmail(email);
            }
        }

        if (user == null) {
            return;
        }

        if (authorities == null) {
            authorities = authService.resolveRoles(user)
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
            userAuthCache.put(user, authorities);
        }

        var authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(),
            null,
            authorities);
        authentication.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

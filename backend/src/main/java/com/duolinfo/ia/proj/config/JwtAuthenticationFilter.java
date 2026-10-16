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

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserService userService,
            AuthService authService) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authenticate(authorization.substring(7).trim());
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        if (token.isBlank() || !jwtService.isTokenValid(token)) {
            return;
        }

        var userId = jwtService.getUserIdFromToken(token);
        User user;
        if (userId.isPresent()) {
            try {
                user = userService.findById(userId.get());
            } catch (ResourceNotFoundException exception) {
                return;
            }
        } else {
            user = userService.findByEmail(jwtService.getEmailFromToken(token));
        }
        if (user == null) {
            return;
        }

        List<SimpleGrantedAuthority> authorities = authService.resolveRoles(user)
            .stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();

        var authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(),
            null,
            authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

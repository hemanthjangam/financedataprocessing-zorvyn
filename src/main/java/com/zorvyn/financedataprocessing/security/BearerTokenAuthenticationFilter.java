package com.zorvyn.financedataprocessing.security;

import com.zorvyn.financedataprocessing.domain.UserAccount;
import com.zorvyn.financedataprocessing.domain.UserStatus;
import com.zorvyn.financedataprocessing.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final PasetoTokenService pasetoTokenService;
    private final UserAccountRepository userAccountRepository;

    public BearerTokenAuthenticationFilter(PasetoTokenService pasetoTokenService, UserAccountRepository userAccountRepository) {
        this.pasetoTokenService = pasetoTokenService;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawToken = pasetoTokenService.extractBearerToken(authorizationHeader);
        PasetoTokenService.ParsedToken parsedToken;
        try {
            parsedToken = pasetoTokenService.parseAndValidate(rawToken);
        } catch (Exception exception) {
            filterChain.doFilter(request, response);
            return;
        }

        UserAccount user = userAccountRepository.findById(parsedToken.userId()).orElse(null);
        if (user == null
                || user.getStatus() == UserStatus.INACTIVE
                || !user.getEmail().equalsIgnoreCase(parsedToken.email())
                || user.getRole() != parsedToken.role()
                || (user.getTokenInvalidBefore() != null && !parsedToken.issuedAt().isAfter(user.getTokenInvalidBefore()))) {
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}

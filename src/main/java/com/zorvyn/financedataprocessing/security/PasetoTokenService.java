package com.zorvyn.financedataprocessing.security;

import com.zorvyn.financedataprocessing.domain.Role;
import com.zorvyn.financedataprocessing.domain.UserAccount;
import com.zorvyn.financedataprocessing.exception.UnauthorizedException;
import dev.paseto.jpaseto.Paseto;
import dev.paseto.jpaseto.Pasetos;
import dev.paseto.jpaseto.Version;
import dev.paseto.jpaseto.lang.Keys;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasetoTokenService {

    private final KeyPair keyPair;
    private final String issuer;
    private final String audience;
    private final Duration tokenTtl;

    public PasetoTokenService(
            @Value("${app.auth.issuer:finance-data-processing}") String issuer,
            @Value("${app.auth.audience:finance-dashboard}") String audience,
            @Value("${app.auth.token-ttl-hours:12}") long tokenTtlHours
    ) {
        this.keyPair = Keys.keyPairFor(Version.V2);
        this.issuer = issuer;
        this.audience = audience;
        this.tokenTtl = Duration.ofHours(tokenTtlHours);
    }

    public IssuedToken issueToken(UserAccount user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenTtl);

        String token = Pasetos.V2.PUBLIC.builder()
                .setIssuer(issuer)
                .setAudience(audience)
                .setSubject(user.getId().toString())
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .setTokenId(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .setPrivateKey(keyPair.getPrivate())
                .compact();

        return new IssuedToken(token, expiresAt);
    }

    public ParsedToken parseAndValidate(String token) {
        try {
            Paseto paseto = Pasetos.parserBuilder()
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .setPublicKey(keyPair.getPublic())
                    .build()
                    .parse(token);

            String subject = paseto.getClaims().getSubject();
            String email = paseto.getClaims().get("email", String.class);
            String role = paseto.getClaims().get("role", String.class);
            Instant issuedAt = paseto.getClaims().getIssuedAt();
            Instant expiration = paseto.getClaims().getExpiration();

            return new ParsedToken(Long.parseLong(subject), email, Role.valueOf(role), issuedAt, expiration);
        } catch (Exception exception) {
            throw new UnauthorizedException("Invalid or expired token");
        }
    }

    public String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Authorization header must use Bearer token");
        }

        String tokenValue = authorizationHeader.substring(7).trim();
        if (tokenValue.isBlank()) {
            throw new UnauthorizedException("Authorization header must include a token");
        }
        return tokenValue;
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }

    public record ParsedToken(Long userId, String email, Role role, Instant issuedAt, Instant expiresAt) {
    }
}

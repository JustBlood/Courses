package ru.just.securityservice.config.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.just.securityservice.model.Token;

import java.text.ParseException;
import java.util.UUID;
import java.util.function.Function;

@RequiredArgsConstructor
@Slf4j
public class RefreshTokenStringDeserializer implements Function<String, Token> {
    private final JWEDecrypter jweDecrypter;

    @Override
    public Token apply(String s) {
        try {
            final EncryptedJWT encryptedJWT = EncryptedJWT.parse(s);
            encryptedJWT.decrypt(jweDecrypter);
            var claims = encryptedJWT.getJWTClaimsSet();
            return new Token(UUID.fromString(claims.getJWTID()),
                    claims.getSubject(),
                    claims.getStringListClaim("authorities"),
                    claims.getIssueTime().toInstant(),
                    claims.getExpirationTime().toInstant());
        } catch (ParseException | JOSEException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}

package ru.just.securityservice.config.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.just.securityservice.config.token.model.Token;

import java.text.ParseException;
import java.util.UUID;
import java.util.function.Function;

@RequiredArgsConstructor
@Slf4j
public class AccessTokenStringDeserializer implements Function<String, Token> {
    private final JWSVerifier verifier;

    @Override
    public Token apply(String s) {
        try {
            var signedJwt = SignedJWT.parse(s);
            if (!signedJwt.verify(verifier)) {
                throw new JOSEException("Incorrect token");
            }
            var claimsSet = signedJwt.getJWTClaimsSet();
            return new Token(UUID.fromString(claimsSet.getJWTID()),
                    claimsSet.getSubject(),
                    UUID.fromString(claimsSet.getStringClaim("device_id")),
                    claimsSet.getStringListClaim("authorities"),
                    claimsSet.getIssueTime().toInstant(),
                    claimsSet.getExpirationTime().toInstant());
        } catch (ParseException | JOSEException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}

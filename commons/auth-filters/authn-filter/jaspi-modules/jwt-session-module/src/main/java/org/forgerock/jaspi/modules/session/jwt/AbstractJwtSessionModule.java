/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.jaspi.modules.session.jwt;

import static org.forgerock.caf.authentication.framework.AuditTrail.AUDIT_SESSION_ID_KEY;
import static org.forgerock.caf.authentication.framework.AuthenticationFramework.LOG;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.callback.CallerPrincipalCallback;

import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.exceptions.JweDecryptionException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedEncryptedJwt;
import org.forgerock.json.jose.jws.handlers.HmacSigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.security.keystore.KeyStoreBuilder;
import org.forgerock.security.keystore.KeyStoreManager;
import org.forgerock.security.keystore.KeyStoreType;
import org.forgerock.util.Utils;
import org.forgerock.util.encode.Base64;

/**
 * A JASPI Session Module which creates a JWT when securing the response from a successful authentication and sets it
 * as a Cookie on the response. Then on subsequent requests checks for the presents of the JWT as a Cookie on the
 * request and validates the signature and decrypts it and checks the expiration time of the JWT.
 *
 * <C> The cookie type.
 *
 * @since 1.0.0
 */
abstract class AbstractJwtSessionModule<C extends JwtSessionCookie> {

    private static final String DEFAULT_JWT_SESSION_COOKIE_NAME = "session-jwt";
    private static final String SKIP_SESSION_PARAMETER_NAME = "skipSession";
    /** Request attribute for logout requests which will cause this module to delete the session cookie. */
    public static final String LOGOUT_SESSION_REQUEST_ATTRIBUTE_NAME = "logoutSession";

    /** The Key Alias configuration property key. */
    public static final String KEY_ALIAS_KEY = "keyAlias";
    /** The Private Key password configuration property key. */
    public static final String PRIVATE_KEY_PASSWORD_KEY = "privateKeyPassword";
    /** The Keystore type configuration property key. */
    public static final String KEYSTORE_TYPE_KEY = "keystoreType";
    /** The Keystore file path property key. */
    public static final String KEYSTORE_FILE_KEY = "keystoreFile";
    /** The Keystore password configuration property key. */
    public static final String KEYSTORE_PASSWORD_KEY = "keystorePassword";
    /** The Jwt Session Cookie Name configuration property key. */
    public static final String SESSION_COOKIE_NAME_KEY = "sessionCookieName";
    /** The Jwt Token Idle timeout configuration property key in minutes. */
    public static final String TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY = "tokenIdleTimeMinutes";
    /** The Jwt Token Maximum life configuration property key in minutes. */
    public static final String MAX_TOKEN_LIFE_IN_MINUTES_KEY = "maxTokenLifeMinutes";
    /** The Jwt Token Idle timeout configuration property key in seconds. */
    public static final String TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY = "tokenIdleTimeSeconds";
    /** The Jwt Token Maximum life configuration property key in seconds. */
    public static final String MAX_TOKEN_LIFE_IN_SECONDS_KEY = "maxTokenLifeSeconds";
    /** The Jwt Validated configuration property key. */
    public static final String JWT_VALIDATED_KEY = "jwtValidated";
    /** Whether the JWT should persist between browser restarts property key. */
    public static final String BROWSER_SESSION_ONLY_KEY = "sessionOnly";
    /** Whether the JWT should be Http Only, ie not accessible by client browser property key. */
    public static final String HTTP_ONLY_COOKIE_KEY = "isHttpOnly";
    /** Whether the JWT should always be encrypted when sent to client browser property key. */
    public static final String SECURE_COOKIE_KEY = "isSecure";
    /** The domains the cookie should be set on property key. */
    public static final String COOKIE_DOMAINS_KEY = "cookieDomains";
    /** HMAC signing key. */
    public static final String HMAC_SIGNING_KEY = "hmacKey";
    private static final JwsAlgorithm SIGNING_ALGORITHM = JwsAlgorithm.HS256;

    private final JwtBuilderFactory jwtBuilderFactory;

    private CallbackHandler handler;

    private String keyAlias;
    private String privateKeyPassword;
    private KeyStoreType keystoreType;
    private String keystoreFile;
    private String keystorePassword;
    String sessionCookieName;
    /** Stores the token idle time in seconds. */
    private int tokenIdleTime;
    /** Stores the max token lifetime in seconds. */
    private int maxTokenLife;
    private boolean browserSessionOnly;
    boolean isHttpOnly;
    boolean isSecure;
    Collection<String> cookieDomains;
    private SigningHandler signingHandler;


    /**
     * Constructs an instance of the AbstractJwtSessionModule.
     */
    AbstractJwtSessionModule() {
        jwtBuilderFactory = new JwtBuilderFactory();
    }

    /**
     * Constructs an instance of the AbstractJwtSessionModule.
     *
     * @param jwtBuilderFactory An instance of the jwtBuilderFactory.
     */
    AbstractJwtSessionModule(JwtBuilderFactory jwtBuilderFactory) {
        this.jwtBuilderFactory = jwtBuilderFactory;
    }

    /**
     * Initialises the module by getting the Keystore and Key alias properties out of the module configuration.
     *
     * @param handler {@code CallbackHandler} used to request information.
     * @param options A {@code Map} of module-specific configuration properties.
     */
    public void initialize(CallbackHandler handler, Map options) throws AuthenticationException {
        this.handler = handler;
        this.keyAlias = (String) options.get(KEY_ALIAS_KEY);
        this.privateKeyPassword = (String) options.get(PRIVATE_KEY_PASSWORD_KEY);
        this.keystoreType = Utils.asEnum((String) options.get(KEYSTORE_TYPE_KEY), KeyStoreType.class);
        this.keystoreFile = (String) options.get(KEYSTORE_FILE_KEY);
        this.keystorePassword = (String) options.get(KEYSTORE_PASSWORD_KEY);
        this.sessionCookieName = (String) options.get(SESSION_COOKIE_NAME_KEY);
        if (isEmpty(sessionCookieName)) {
            this.sessionCookieName = DEFAULT_JWT_SESSION_COOKIE_NAME;
        }
        final String tokenIdleTimeMinutes = (String) options.get(TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY);
        final String tokenIdleTimeSeconds = (String) options.get(TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY);
        if (!isEmpty(tokenIdleTimeMinutes) && !isEmpty(tokenIdleTimeSeconds)) {
            throw new AuthenticationException("Can't use both " + TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY
                    + " setting and the " + TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY + " setting.");
        } else if (!isEmpty(tokenIdleTimeMinutes)) {
            this.tokenIdleTime = Integer.parseInt(tokenIdleTimeMinutes) * 60;
        } else if (!isEmpty(tokenIdleTimeSeconds)) {
            this.tokenIdleTime = Integer.parseInt(tokenIdleTimeSeconds);
        } else {
            this.tokenIdleTime = 0;
        }
        final String maxTokenLifeMinutes = (String) options.get(MAX_TOKEN_LIFE_IN_MINUTES_KEY);
        final String maxTokenLifeSeconds = (String) options.get(MAX_TOKEN_LIFE_IN_SECONDS_KEY);
        if (!isEmpty(maxTokenLifeMinutes) && !isEmpty(maxTokenLifeSeconds)) {
            throw new AuthenticationException("Can't use both the " + MAX_TOKEN_LIFE_IN_MINUTES_KEY
                    + " setting and the " + MAX_TOKEN_LIFE_IN_SECONDS_KEY + " setting.");
        } else if (!isEmpty(maxTokenLifeMinutes)) {
            this.maxTokenLife = Integer.parseInt(maxTokenLifeMinutes) * 60;
        } else if (!isEmpty(maxTokenLifeSeconds)) {
            this.maxTokenLife = Integer.parseInt(maxTokenLifeSeconds);
        } else {
            this.maxTokenLife = 0;
        }
        Boolean sessionOnly = (Boolean) options.get(BROWSER_SESSION_ONLY_KEY);
        this.browserSessionOnly = sessionOnly == null ? false : sessionOnly;
        Boolean httpOnly = (Boolean) options.get(HTTP_ONLY_COOKIE_KEY);
        this.isHttpOnly = httpOnly == null ? false : httpOnly;
        Boolean secure = (Boolean) options.get(SECURE_COOKIE_KEY);
        this.isSecure = secure == null ? false : secure;
        cookieDomains = (Collection<String>) options.get(COOKIE_DOMAINS_KEY);
        if (cookieDomains == null || cookieDomains.isEmpty()) {
            cookieDomains = Collections.singleton(null);
        }
        final byte[] signingKey = Base64.decode((String) options.get(HMAC_SIGNING_KEY));
        if (signingKey == null || signingKey.length < 32) {
            throw new AuthenticationException("Signing key must be at least 256-bits base64 encoded");
        }
        this.signingHandler = new HmacSigningHandler(signingKey);
        Arrays.fill(signingKey, (byte) 0);
    }

    /**
     * Checks for the presence of the JWT as a Cookie on the request and validates the signature and decrypts it and
     * checks the expiration time of the JWT. If all these checks pass then the method return AuthStatus.SUCCESS,
     * otherwise returns AuthStatus.SEND_FAILURE.
     *
     * @param messageInfo The message context info for this request.
     * @param clientSubject A {@code Subject} that represents the subject of this request.
     * @return If the Jwt is valid then AuthStatus.SUCCESS is returned, otherwise AuthStatus.SEND_FAILURE is returned.
     * @throws AuthenticationException If there is a problem validating the request.
     */
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject) throws AuthenticationException {

        Jwt jwt = validateJwtSessionCookie(messageInfo);
        if (jwt == null) {
            LOG.debug("Session JWT NOT valid");
            return AuthStatus.SEND_FAILURE;
        } else {
            LOG.debug("Session JWT valid");
            try {
                handler.handle(new Callback[]{
                    new CallerPrincipalCallback(clientSubject, jwt.getClaimsSet().getClaim("prn", String.class))
                });
                //TODO also include ATTRIBUTE_AUTHCID!
                Map<String, Object> context = getContextMap(messageInfo);
                Map<String, Object> claimsSetContext = jwt.getClaimsSet()
                        .getClaim(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, Map.class);
                if (claimsSetContext != null) {
                    context.putAll(claimsSetContext);
                }
                messageInfo.getMap().put(AUDIT_SESSION_ID_KEY, jwt.getClaimsSet().get("sessionId").asString());
            } catch (IOException | UnsupportedCallbackException e) {
                LOG.error("Error setting user principal", e);
                throw new AuthenticationException(e.getMessage(), e);
            }
            return AuthStatus.SUCCESS;
        }
    }

    /**
     * Checks if the String is non-null and a non-empty String.
     *
     * @param s The String to check.
     * @return <code>true</code> if the String is non-null and non-empty.
     */
    private boolean isEmpty(String s) {
        return s == null || "".equals(s);
    }

    /**
     * Validates if the Jwt Session Cookie is valid and the idle timeout or max life has expired.
     *
     * @param messageInfo The MessageInfo instance.
     * @return The Jwt if successfully validated otherwise null.
     */
    public Jwt validateJwtSessionCookie(MessageInfo messageInfo) {

        C jwtSessionCookie = findJwtSessionCookie(messageInfo);
        if (jwtSessionCookie != null) {
            LOG.debug("Session JWT cookie found");
        }

        if (jwtSessionCookie != null && !isEmpty(jwtSessionCookie.getValue())) {

            final Jwt jwt;
            try {
                jwt = verifySessionJwt(jwtSessionCookie.getValue());
            } catch (JweDecryptionException e) {
                LOG.debug("Failed to decrypt Jwt", e);
                return null;
            } catch (FileNotFoundException e) {
                LOG.debug("Unable to load keystore", e);
                return null;
            }
            if (jwt != null) {
                //if all goes well!
                setClaimsOnRequest(messageInfo, jwt);

                // If request is made within one minute of the Jwt being issued the idle timeout is not reset.
                // This helps reduce overheads when the client makes multiple requests for a single operation.
                if (hasCoolOffPeriodExpired(jwt)) {
                    // reset tokenIdleTime
                    try {
                        resetIdleTimeout(jwt, messageInfo);
                    } catch (FileNotFoundException e) {
                        LOG.debug("Unable to load keystore", e);
                        return null;
                    }
                }

                messageInfo.getMap().put(JWT_VALIDATED_KEY, true);

                return jwt;
            }
        } else {
            LOG.debug("Session JWT cookie not set");
        }

        return null;
    }

    abstract C findJwtSessionCookie(MessageInfo messageInfo);

    abstract void setClaimsOnRequest(MessageInfo messageInfo, Jwt jwt);

    /**
     * Ensures the context map exists within the messageInfo object, and then returns the context map to be used.
     *
     * @param messageInfo The MessageInfo instance.
     * @return The context map internal to the messageInfo's map.
     */
    public Map<String, Object> getContextMap(MessageInfo messageInfo) {
        Map<String, Object> internalMap = (Map<String, Object>) messageInfo.getMap()
                .get(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT);

        if (internalMap == null) {
            internalMap = new HashMap<>();
            messageInfo.getMap().put(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, internalMap);
        }

        return internalMap;
    }

    /**
     * Verifies that the JWT has a valid signature and can be decrypted and that the JWT expiration time has not
     * passed.
     *
     * The method will return null in the case where the JWT is not valid.
     *
     * @param sessionJwt The JWT string.
     * @return The validated decrypted JWT.
     */
    private Jwt verifySessionJwt(String sessionJwt) throws FileNotFoundException {
        final KeyStore keyStore = new KeyStoreBuilder()
                .withKeyStoreFile(keystoreFile)
                .withPassword(keystorePassword)
                .withKeyStoreType(keystoreType)
                .build();
        final KeyStoreManager keyStoreManager = new KeyStoreManager(keyStore);

        Key privateKey = keyStoreManager.getPrivateKey(keyAlias, privateKeyPassword);

        SignedEncryptedJwt jwt = jwtBuilderFactory.reconstruct(sessionJwt, SignedEncryptedJwt.class);
        if (!jwt.verify(signingHandler)) {
            return null;
        }
        jwt.decrypt(privateKey);

        Date expirationTime = jwt.getClaimsSet().getExpirationTime();
        Date tokenIdleTime = new Date(jwt.getClaimsSet().getClaim(TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, Integer.class)
                .longValue() * 1000L);

        Date now = new Date(System.currentTimeMillis());

        if ((now.getTime() < expirationTime.getTime()) && (now.getTime() < tokenIdleTime.getTime())) {
            return jwt;
        }

        return null;
    }

    /**
     * Determines if the request was made within one minute of the Jwt being issued.
     *
     * @param jwt The Jwt, which has been decrypted and validated prior to this call.
     * @return If the request was made one minute after the Jwt was issued.
     */
    private boolean hasCoolOffPeriodExpired(Jwt jwt) {

        Date issuedAtTime = jwt.getClaimsSet().getIssuedAtTime();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, -1);

        return calendar.getTime().compareTo(issuedAtTime) > 0;
    }

    /**
     * Resets the idle timeout value on the Jwt, as well as the issued at time and not before time.
     *
     * @param jwt The Jwt, which has been decrypted and validated prior to this call.
     * @param messageInfo The {@code MessageInfo} which contains the response with the Jwt Session Cookie.
     * @throws FileNotFoundException If unable to load keystore.
     */
    private void resetIdleTimeout(Jwt jwt, MessageInfo messageInfo) throws FileNotFoundException {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        final Date now = calendar.getTime();
        Date nbf = now;
        Date iat = now;
        calendar.add(Calendar.SECOND, tokenIdleTime);
        Date tokenIdleTime = calendar.getTime();
        Date exp = jwt.getClaimsSet().getExpirationTime();

        jwt.getClaimsSet().setIssuedAtTime(iat);
        jwt.getClaimsSet().setNotBeforeTime(nbf);
        jwt.getClaimsSet().setClaim(TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, tokenIdleTime.getTime() / 1000L);

        final KeyStore keyStore = new KeyStoreBuilder()
                .withKeyStoreFile(keystoreFile)
                .withPassword(keystorePassword)
                .withKeyStoreType(keystoreType)
                .build();
        final KeyStoreManager keyStoreManager = new KeyStoreManager(keyStore);

        Key publicKey = keyStoreManager.getPublicKey(keyAlias);

        String jwtString = rebuildEncryptedJwt(jwt, publicKey);

        addCookiesToResponse(createCookies(jwtString, getCookieMaxAge(now, exp), "/"), messageInfo);
    }

    abstract Collection<C> createCookies(String value, int maxAge, String path);

    abstract void addCookiesToResponse(Collection<C> cookies, MessageInfo messageInfo);

    /**
     * Recreates the Encrypted Session Jwt.
     *
     * @param jwt The orginal Session Jwt.
     * @param publicKey The public key.
     * @return The Session Jwt.
     */
    protected String rebuildEncryptedJwt(Jwt jwt, Key publicKey) {
        return buildJwtString(jwt.getClaimsSet(), publicKey);
    }

    /**
     * Creates a JWT after a successful authentication and sets it as a Cookie on the response. An expiration time
     * is included in the JWT to limit the life of the JWT.
     *
     * @param messageInfo The message context info for this request.
     * @return {@code AuthStatus} representing the completion status of the processing. See
     * {@link javax.security.auth.message.module.ServerAuthModule#secureResponse(
     * javax.security.auth.message.MessageInfo, Subject)} for the allowed
     * {@code AuthStatus} values. Note {@link AuthStatus#SEND_CONTINUE} is not supported by this
     * interface
     * @throws AuthenticationException If message processing failed without establishing a failure response
     * message in the {@code MessageInfo}.
     */
    public AuthStatus secureResponse(MessageInfo messageInfo) throws AuthenticationException {

        Map<String, Object> jwtParameters = new HashMap<>();

        Map<String, Object> map = messageInfo.getMap();
        String principal = getPrincipalFromRequest(messageInfo);

        if (principal != null) {
            jwtParameters.put("prn", principal);
        }

        if (map.containsKey(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT)) {
            jwtParameters.put(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, getContextMap(messageInfo));
        }

        if (map.containsKey(SKIP_SESSION_PARAMETER_NAME) && ((Boolean) map.get(SKIP_SESSION_PARAMETER_NAME))) {
            LOG.debug("Skipping creating session as jwtParameters contains, {}", SKIP_SESSION_PARAMETER_NAME);
            return AuthStatus.SEND_SUCCESS;
        }

        if (isLogoutRequest(messageInfo)) {
            deleteSessionJwtCookie(messageInfo);
            return AuthStatus.SEND_SUCCESS;
        }

        boolean jwtValidated = messageInfo.getMap().containsKey(JWT_VALIDATED_KEY);
        if (!jwtValidated) {
            // create jwt
            String sessionId = UUID.randomUUID().toString();
            jwtParameters.put("sessionId", sessionId);
            messageInfo.getMap().put(AUDIT_SESSION_ID_KEY, sessionId);
            try {
                addCookiesToResponse(createSessionJwtCookies(jwtParameters), messageInfo);
            } catch (FileNotFoundException e) {
                LOG.debug("Unable to load keystore", e);
                return AuthStatus.FAILURE;
            }
        }

        return AuthStatus.SEND_SUCCESS;
    }

    abstract String getPrincipalFromRequest(MessageInfo messageInfo);

    abstract boolean isLogoutRequest(MessageInfo messageInfo);

    /**
     * Creates the session JWT, including the custom parameters in the payload and adding the expiration time and then
     * sets the JWT onto the response as a Cookie.
     *
     * @param jwtParameters The parameters that should be added to the JWT payload.
     * @throws AuthenticationException If there is a problem creating and encrypting the JWT.
     * @throws FileNotFoundException If unable to load keystore.
     */
    private Collection<C> createSessionJwtCookies(Map<String, Object> jwtParameters)
            throws AuthenticationException, FileNotFoundException {

        final KeyStore keyStore = new KeyStoreBuilder()
                .withKeyStoreFile(keystoreFile)
                .withPassword(keystorePassword)
                .withKeyStoreType(keystoreType)
                .build();
        final KeyStoreManager keyStoreManager = new KeyStoreManager(keyStore);

        Key publicKey = keyStoreManager.getPublicKey(keyAlias);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.MILLISECOND, 0);
        final Date now = calendar.getTime();
        calendar.add(Calendar.SECOND, maxTokenLife);
        final Date exp = calendar.getTime();
        Date nbf = now;
        Date iat = now;
        calendar.setTime(now);
        calendar.add(Calendar.SECOND, tokenIdleTime);
        Date tokenIdleTime = calendar.getTime();
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claimsSet = jwtBuilderFactory.claims()
                .jti(jti)
                .exp(exp)
                .nbf(nbf)
                .iat(iat)
                .claim(TOKEN_IDLE_TIME_IN_SECONDS_CLAIM_KEY, tokenIdleTime.getTime() / 1000L)
                .claims(jwtParameters)
                .build();

        String jwtString = buildJwtString(claimsSet, publicKey);

        return createCookies(jwtString, getCookieMaxAge(now, exp), "/");
    }

    private String buildJwtString(JwtClaimsSet claimsSet, Key publicKey) {
        return jwtBuilderFactory
                .jwe(publicKey)
                .headers()
                .alg(JweAlgorithm.RSAES_PKCS1_V1_5)
                .enc(EncryptionMethod.A128CBC_HS256)
                .done()
                .claims(claimsSet)
                .sign(signingHandler, SIGNING_ALGORITHM)
                .build();
    }

    /**
     * Returns the max age for the cookie, based on whether the cookie should be browser session only.
     * <br/>
     * If the cookie is only meant to last the same length the users browser is open, then the max age is set to -1.
     * Otherwise the max age is set to expiry time.
     *
     * @param now The Date at which the cookie was created.
     * @param exp The expiry Date of the cookie.
     */
    private int getCookieMaxAge(Date now, Date exp) {
        if (!browserSessionOnly) {
            return new Long((exp.getTime() - now.getTime()) / 1000L).intValue();
        } else {
            return -1;
        }
    }

    /**
     * Provides a way to delete the Jwt Session Cookie, by setting a new cookie with the same name, null value and
     * max age 0.
     *
     * @param messageInfo The {@code MessageInfo} which contains the Response with the Jwt Session Cookie.
     */
    public void deleteSessionJwtCookie(MessageInfo messageInfo) {
        addCookiesToResponse(createCookies(null, 0, "/"), messageInfo);
    }
}

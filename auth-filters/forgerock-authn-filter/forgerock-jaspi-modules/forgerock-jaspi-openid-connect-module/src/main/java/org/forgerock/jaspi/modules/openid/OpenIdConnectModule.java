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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.jaspi.modules.openid;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.LogFactory;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverServiceConfigurator;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverServiceConfiguratorImpl;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.exceptions.JwtReconstructionException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.utils.KeystoreManagerException;

/**
 * OpenID Connect module that allows access when a valid OpenID Connect JWT which
 * our server trusts is presented in the specific header field.
 */
public class OpenIdConnectModule implements ServerAuthModule {

    public static final String KEYSTORE_LOCATION_KEY = "keystoreLocation";
    public static final String KEYSTORE_PASSWORD_KEY = "keystorePassword";
    public static final String KEYSTORE_TYPE_KEY = "keystoreType";
    public static final String HEADER_KEY = "openIdConnectHeader";
    public static final String RESOLVERS_KEY = "resolvers";

    private static final DebugLogger DEBUG = LogFactory.getDebug();

    private final JwtReconstruction constructor;
    private final OpenIdResolverServiceConfigurator resolverConfigurator;

    private String openIdConnectHeader;

    private OpenIdResolverService resolverService;

    private CallbackHandler callbackHandler;

    /**
     * Default constructor
     */
    public OpenIdConnectModule() {
        constructor = new JwtReconstruction();
        resolverConfigurator = new OpenIdResolverServiceConfiguratorImpl();
    }

    /**
     * Used for tests
     *
     * @param resolverConfigurator Configurator device for setting up our resolver service
     * @param constructor Builder for creating our JWTs out of their representation
     */
    OpenIdConnectModule(final OpenIdResolverServiceConfigurator resolverConfigurator,
                        final JwtReconstruction constructor) {
        this.resolverConfigurator = resolverConfigurator;
        this.constructor = constructor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(final MessagePolicy requestPolicy, final MessagePolicy responsePolicy,
                           final CallbackHandler callbackHandler, final Map config) throws AuthException {

        this.openIdConnectHeader = (String) config.get(OpenIdConnectModule.HEADER_KEY);
        this.callbackHandler = callbackHandler;

        final String keystoreLocation = (String) config.get(OpenIdConnectModule.KEYSTORE_LOCATION_KEY);
        final String keystorePassword = (String) config.get(OpenIdConnectModule.KEYSTORE_PASSWORD_KEY);
        final String keystoreType = (String) config.get(OpenIdConnectModule.KEYSTORE_TYPE_KEY);

        final List<Map<String, String>> resolvers =
                (List<Map<String, String>>) config.get(OpenIdConnectModule.RESOLVERS_KEY);

        try {
            resolverService = resolverConfigurator.setupService(keystoreType, keystoreLocation, keystorePassword);
        } catch (KeystoreManagerException kme) {
            DEBUG.debug("KeystoreManagerException thrown while generating OpenIDResolverService", kme);
            throw new AuthException("OpenIdConnectModule configuration is invalid.");
        }

        //if we weren't able to set up the service, or any one of the supplied resolver configs was invalid,
        // error out here
        if (resolverService == null || !resolverConfigurator.configureService(resolverService, resolvers)) {
            DEBUG.debug("OpenIdConnectModule config is invalid. You must configure at least one valid resolver.");
            throw new AuthException("OpenIdConnectModule configuration is invalid.");
        }

    }

    /**
     * Attempts to retrieve the value of the specified OpenID Connect header from the messageInfo, then
     * converts this to a Jwt and attempts to decrypt. If both these steps succeed, we verify the Jwt
     * through the {@link org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver} interface to ensure that we are the intended audience,
     * the token has not expired and the issuer was an expected source.
     *
     * If all of these validate, we return SUCCESS, otherwise SEND_FAILURE.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return AuthStatus.SUCCESS is sent if everything validates. Otherwise AuthStatus.SEND_FAILURE.
     * @throws AuthException if there are issues handling the request caused by improper config
     */
    @Override
    public AuthStatus validateRequest(final MessageInfo messageInfo, final Subject clientSubject,
                                      final Subject serviceSubject) throws AuthException {

        final HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        final String jwtValue = request.getHeader(openIdConnectHeader);

        if (jwtValue == null || jwtValue.isEmpty()) {
            return AuthStatus.SEND_FAILURE;
        }

        final SignedJwt retrievedJwt;

        try {
            retrievedJwt = constructor.reconstructJwt(jwtValue, SignedJwt.class);
        } catch (InvalidJwtException ije) {
            DEBUG.debug("Invalid JWS in supplied header", ije);
            return AuthStatus.SEND_FAILURE;
        } catch (JwtReconstructionException jre) {
            DEBUG.debug("Unable to reconstruct JWS from supplied header", jre);
            return AuthStatus.SEND_FAILURE;
        }

        final JwtClaimsSet jwtClaimSet = retrievedJwt.getClaimsSet();

        OpenIdResolver resolver = resolverService.getResolverForIssuer(jwtClaimSet.getIssuer());

        //if no resolver for this issuer found, abort
        if (resolver == null) {
            DEBUG.debug("No resolver found for the issuer: " + jwtClaimSet.getIssuer());
            return AuthStatus.SEND_FAILURE;
        }

        try {
            resolver.validateIdentity(retrievedJwt);

            callbackHandler.handle(new Callback[]{
                new CallerPrincipalCallback(clientSubject, jwtClaimSet.getPrincipal())
            });

        } catch (OpenIdConnectVerificationException oice) {
            DEBUG.error("Unable to validate authenticated identity from JWT.", oice);
            return AuthStatus.SEND_FAILURE;
        } catch (IOException e) {
            DEBUG.error("Error setting user principal", e);
            throw new AuthException(e.getMessage());
        } catch (UnsupportedCallbackException e) {
            DEBUG.error("Error setting user principal", e);
            throw new AuthException(e.getMessage());
        }

        return AuthStatus.SUCCESS;
    }

    /**
     * Sends SEND_SUCCESS automatically. As we're on our way out of the system at this point, there's
     * no need to hold them up, or append anything new to the response.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(final MessageInfo messageInfo, final Subject subject) throws AuthException {
        return AuthStatus.SEND_SUCCESS;
    }

    /**
     * Nothing to clean
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public void cleanSubject(final MessageInfo messageInfo, final Subject subject) throws AuthException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{ HttpServletRequest.class, HttpServletResponse.class };
    }

}

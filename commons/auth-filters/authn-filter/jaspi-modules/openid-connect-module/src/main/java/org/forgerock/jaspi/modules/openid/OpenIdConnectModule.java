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
* Copyright 2014-2015 ForgeRock AS.
*/

package org.forgerock.jaspi.modules.openid;

import static javax.security.auth.message.AuthStatus.SEND_FAILURE;
import static javax.security.auth.message.AuthStatus.SEND_SUCCESS;
import static org.forgerock.caf.authentication.framework.AuthenticationFramework.LOG;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.forgerock.caf.authentication.api.AsyncServerAuthModule;
import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.caf.authentication.api.MessageInfoContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverServiceConfigurator;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverServiceConfiguratorImpl;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverServiceImpl;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.exceptions.JwtReconstructionException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.util.promise.Promise;

/**
 * OpenID Connect module that allows access when a valid OpenID Connect JWT which
 * our server trusts is presented in the specific header field.
 */
public class OpenIdConnectModule implements AsyncServerAuthModule {

    /**
     * Default read timeout for HTTP connections.
     */
    private static final int DEFAULT_READ_TIMEOUT = 5_000;

    /**
     * Default connection timeout for HTTP connections.
     */
    private static final int DEFAULT_CONN_TIMEOUT = 5_000;

    /**
     * Lookup key for the configured HTTP connection's read timeout for this module.
     */
    public static final String READ_TIMEOUT_KEY = "readTimeout";

    /**
     * Lookup key for the configured HTTP connection's connection timeout for this module.
     */
    public static final String CONNECTION_TIMEOUT_KEY = "connectionTimeout";

    /**
     * Lookup key for the configured HTTP header used by this module to locate JWSs.
     */
    public static final String HEADER_KEY = "openIdConnectHeader";

    /**
     * Lookup key for the configured resolvers which will be used by this module.
     */
    public static final String RESOLVERS_KEY = "resolvers";

    private final JwtReconstruction constructor;
    private final OpenIdResolverServiceConfigurator serviceConfigurator;

    private String openIdConnectHeader;

    private OpenIdResolverService resolverService;

    private CallbackHandler callbackHandler;

    /**
     * Default constructor.
     */
    public OpenIdConnectModule() {
        constructor = new JwtReconstruction();
        serviceConfigurator = new OpenIdResolverServiceConfiguratorImpl();
    }

    /**
     * Used for tests.
     *
     * @param serviceConfigurator Configurator device for setting up our resolver service
     * @param constructor Builder for creating our JWTs out of their representation
     */
    OpenIdConnectModule(final OpenIdResolverServiceConfigurator serviceConfigurator,
                        final JwtReconstruction constructor,
                        final OpenIdResolverService service,
                        final CallbackHandler callback,
                        final String openIdConnectHeader) {
        this.serviceConfigurator = serviceConfigurator;
        this.constructor = constructor;
        this.resolverService = service;
        this.callbackHandler = callback;
        this.openIdConnectHeader = openIdConnectHeader;
    }

    @Override
    public String getModuleId() {
        return "OpenIdConnect";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
            CallbackHandler callbackHandler, Map<String, Object> config) {

        this.openIdConnectHeader = (String) config.get(OpenIdConnectModule.HEADER_KEY);
        this.callbackHandler = callbackHandler;

        Integer readTimeout = (Integer) config.get(OpenIdConnectModule.READ_TIMEOUT_KEY);
        Integer connTimeout = (Integer) config.get(OpenIdConnectModule.CONNECTION_TIMEOUT_KEY);

        if (openIdConnectHeader == null || openIdConnectHeader.isEmpty()) {
            LOG.debug("OpenIdConnectModule config is invalid. You must include the header key parameter");
            return newExceptionPromise(new AuthenticationException("OpenIdConnectModule configuration is invalid."));
        }

        if (readTimeout == null || readTimeout < 0) {
            LOG.debug("Read Timeout setting invalid, set to default: {}", DEFAULT_READ_TIMEOUT);
            readTimeout = DEFAULT_READ_TIMEOUT;
        }

        if (connTimeout == null || connTimeout < 0) {
            LOG.debug("Connection Timeout setting invalid, set to default: {}", DEFAULT_CONN_TIMEOUT);
            connTimeout = DEFAULT_CONN_TIMEOUT;
        }

        final List<Map<String, String>> resolvers =
                (List<Map<String, String>>) config.get(OpenIdConnectModule.RESOLVERS_KEY);

        resolverService = new OpenIdResolverServiceImpl(readTimeout, connTimeout);

        //if we weren't able to set up the service, or any one of the supplied resolver configs was invalid,
        //error out here
        if (!serviceConfigurator.configureService(resolverService, resolvers)) {
            LOG.debug("OpenIdConnectModule config is invalid. You must configure at least one valid resolver.");
            return newExceptionPromise(new AuthenticationException("OpenIdConnectModule configuration is invalid."));
        }

        return newResultPromise(null);
    }

    /**
     * Attempts to retrieve the value of the specified OpenID Connect header from the messageInfo, then
     * converts this to a Jwt and attempts to decrypt. If both these steps succeed, we verify the Jwt
     * through the {@link org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver} interface
     * to ensure that we are the intended audience, the token has not expired and the issuer was an expected source.
     *
     * If all of these validate, we return SUCCESS, otherwise SEND_FAILURE.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return A Promise completed with AuthStatus.SUCCESS if everything validates or with AuthStatus.SEND_FAILURE
     * in the case of a failure, or completed with an exception if there are issues handling the request caused
     * by improper config.
     */
    @Override
    public Promise<AuthStatus, AuthenticationException> validateRequest(MessageInfoContext messageInfo,
            Subject clientSubject, Subject serviceSubject) {

        final Request request = messageInfo.getRequest();
        final String jwtValue = request.getHeaders().getFirst(openIdConnectHeader);

        if (jwtValue == null || jwtValue.isEmpty()) {
            return newResultPromise(SEND_FAILURE);
        }

        final SignedJwt retrievedJwt;

        try {
            retrievedJwt = constructor.reconstructJwt(jwtValue, SignedJwt.class);
        } catch (InvalidJwtException ije) {
            LOG.debug("Invalid JWS in supplied header", ije);
            return newResultPromise(SEND_FAILURE);
        } catch (JwtReconstructionException jre) {
            LOG.debug("Unable to reconstruct JWS from supplied header", jre);
            return newResultPromise(SEND_FAILURE);
        }

        final JwtClaimsSet jwtClaimSet = retrievedJwt.getClaimsSet();

        OpenIdResolver resolver = resolverService.getResolverForIssuer(jwtClaimSet.getIssuer());

        //if no resolver for this issuer found, abort
        if (resolver == null) {
            LOG.debug("No resolver found for the issuer: {}", jwtClaimSet.getIssuer());
            return newResultPromise(SEND_FAILURE);
        }

        try {
            resolver.validateIdentity(retrievedJwt);

            callbackHandler.handle(new Callback[]{
                new CallerPrincipalCallback(clientSubject, jwtClaimSet.getSubject())
            });

        } catch (OpenIdConnectVerificationException oice) {
            LOG.debug("Unable to validate authenticated identity from JWT.", oice);
            return newResultPromise(SEND_FAILURE);
        } catch (IOException | UnsupportedCallbackException e) {
            LOG.debug("Error setting user principal", e);
            return newExceptionPromise(new AuthenticationException(e.getMessage()));
        }

        return newResultPromise(AuthStatus.SUCCESS);
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
    public Promise<AuthStatus, AuthenticationException> secureResponse(MessageInfoContext messageInfo,
            Subject subject) {
        return newResultPromise(SEND_SUCCESS);
    }

    /**
     * Nothing to clean.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public Promise<Void, AuthenticationException> cleanSubject(MessageInfoContext messageInfo, Subject subject) {
        return newResultPromise(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Class<?>> getSupportedMessageTypes() {
        return Arrays.asList(new Class<?>[]{Request.class, Response.class});
    }
}

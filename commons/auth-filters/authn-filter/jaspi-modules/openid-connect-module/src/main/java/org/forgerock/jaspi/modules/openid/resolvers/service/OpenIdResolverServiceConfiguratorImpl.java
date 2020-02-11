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

package org.forgerock.jaspi.modules.openid.resolvers.service;

import static org.forgerock.caf.authentication.framework.AuthenticationFramework.LOG;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;

/**
 * Implementation of the {@link OpenIdResolverServiceConfigurator} interface which
 * applies a simple priority ordering when reading a service configuration.
 */
public class OpenIdResolverServiceConfiguratorImpl implements OpenIdResolverServiceConfigurator {

    /**
     * This implementation includes a priority system for ensuring invalid configs still
     * attempt to be loaded.
     *
     * Priority is:
     * - OpenIDConfiguration
     * - JWK Location
     * - Keystore Location
     * - Secret Key
     * - Failure
     *
     * @param service to configure
     * @param resolvers the configuration
     * @return false if any resolver configuration fails true otherwise
     */
    public boolean configureService(final OpenIdResolverService service, final List<Map<String, String>> resolvers) {

        if (resolvers == null || resolvers.size() < 1) {
            return false;
        }

        boolean atLeastOne = false;

        for (Map<String, String> resolverConfig : resolvers) {

            final String keyAlias = resolverConfig.get(OpenIdResolver.KEY_ALIAS_KEY);
            final String clientSecret = resolverConfig.get(OpenIdResolver.CLIENT_SECRET_KEY);
            final String jwk = resolverConfig.get(OpenIdResolver.JWK);
            final String openIdConfig = resolverConfig.get(OpenIdResolver.WELL_KNOWN_CONFIGURATION);

            if (openIdConfig != null) {
                atLeastOne = openIdConfiguration(service, openIdConfig);
                continue;
            }

            final String issuer = resolverConfig.get(OpenIdResolver.ISSUER_KEY);

            if (issuer == null) {
                LOG.debug("No issuer name found for non-Open ID Configuration configured resolver");
                continue;
            }

            if (jwk != null) {
                atLeastOne = jwkConfiguration(service, jwk, issuer);
                continue;
            }

            if (keyAlias != null) {

                final String keystoreLocation = resolverConfig.get(OpenIdResolver.KEYSTORE_LOCATION_KEY);
                final String keystorePass = resolverConfig.get(OpenIdResolver.KEYSTORE_PASS_KEY);
                final String keystoreType = resolverConfig.get(OpenIdResolver.KEYSTORE_TYPE_KEY);

                atLeastOne = keystoreConfiguration(service, keystoreLocation, keystorePass, keystoreType,
                        keyAlias, issuer);
                continue;
            }

            if (clientSecret != null) {
                atLeastOne = sharedSecretConfiguration(service, clientSecret, issuer);
            }
        }

        return atLeastOne;

    }

    /**
     * Configures the service to hold a resolver whose configuration relies on keys stored
     * in trust stores.
     *
     * @param service The service to configure with this resolver
     * @param keystoreLocation The location of the trust store file
     * @param keystorePass The password to the keystore
     * @param keystoreType The type of keystore to which the location param points
     * @param keyAlias The name under which the key is stored in the trust store
     * @param issuer The provider (issuer) of the JWS
     * @return True if the service is populated with a new resolver, false otherwise
     */
    private boolean keystoreConfiguration(OpenIdResolverService service, String keystoreLocation, String keystorePass,
                                          String keystoreType, String keyAlias, String issuer) {

        if ((keystoreLocation == null || keystoreLocation.isEmpty())
                || (keystoreType == null || keystoreType.isEmpty())
                || (keystorePass == null || keystorePass.isEmpty())) {
            LOG.debug("Unable to configure resolver using keyAlias for {}", issuer);
            return false;
        }

        if (!service.configureResolverWithKey(issuer, keyAlias,
                keystoreLocation, keystoreType, keystorePass)) {
            LOG.debug("Unable to configure resolver using keyAlias for {}", issuer);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Configures the service to hold a resolver which is generated by the use of a
     * SharedSecret (String) converted into a SecretKey via HMAC.
     *
     * @param service The service to configure with this resolver
     * @param secret The shared secret, known to both provider and client
     * @param issuer The provider (issuer) of the JWS
     * @return True if the service is populated with a new resolver, false otherwise
     */
    private boolean sharedSecretConfiguration(OpenIdResolverService service, String secret, String issuer) {
        if (!service.configureResolverWithSecret(issuer, secret)) {
            LOG.debug("Unable to configure resolver using sharedSecret for {}", issuer);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Configures the service to hold a resolver whose configuration has been drawn from
     * a JWK set's URL.
     *
     * @param service The service to configure with this resolver
     * @param jwk The URL of the configuration to use to generate the resolver
     * @param issuer The issuer's name to which this resolver will respond through the service
     * @return True if the service is populated with a new resolver, false otherwise
     */
    private boolean jwkConfiguration(OpenIdResolverService service, String jwk, String issuer) {

        final URL jwkUrl;
        try {
            jwkUrl = new URL(jwk);
        } catch (MalformedURLException e) {
            LOG.debug("Supplied JWKs URL at {} is invalid.", jwk);
            return false;
        }

        if (!service.configureResolverWithJWK(issuer, jwkUrl)) {
            LOG.debug("Unable to configure resolver using JWK for {}", issuer);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Configures the service to hold a resolver whose configuration has been drawn from
     * an Open ID Configuration URL.
     *
     * @param service The service to configure with this resolver
     * @param openIdConfig The configuration to use to generate the resolver
     * @return True if the service is populated with a new resolver, false otherwise
     */
    private boolean openIdConfiguration(OpenIdResolverService service, String openIdConfig) {

        final URL configUrl;
        try {
            configUrl = new URL(openIdConfig);
        } catch (MalformedURLException e) {
            LOG.debug("Supplied JWKs URL at {} is invalid.", openIdConfig);
            return false;
        }

        if (!service.configureResolverWithWellKnownOpenIdConfiguration(configUrl)) {
            LOG.debug("Unable to configure resolver using Open ID Configuration at url: {}", openIdConfig);
            return false;
        } else {
            return true;
        }
    }

}

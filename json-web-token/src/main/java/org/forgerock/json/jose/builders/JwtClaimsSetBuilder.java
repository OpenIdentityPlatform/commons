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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.json.jose.builders;

import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.jwt.JwtType;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of a JWT Claims Set builder that provides a fluent builder pattern to creating JWT Claims Sets.
 * <p>
 * See {@link org.forgerock.json.jose.jwt.JwtClaimsSet} for information on the JwtClaimsSet object that this builder
 * creates.
 *
 * @author Phill Cunnington
 * @since 2.0.0
 */
public class JwtClaimsSetBuilder {

    private final Map<String, Object> claims = new HashMap<String, Object>();

    /**
     * Adds a custom claim to the JWT Claims Set.
     * <p>
     * @see JwtClaimsSet#setClaim(String, Object)
     *
     * @param key The claim name.
     * @param claim The claim value.
     * @return This JwtClaimsSetBuilder.
     */
    public JwtClaimsSetBuilder claim(String key, Object claim) {
        claims.put(key, claim);
        return this;
    }

    /**
     * Sets all of the claims the JWT Claims Set with the values contained in the specified map.
     * <p>
     * @see JwtClaimsSet#setClaims(java.util.Map)
     *
     * @param claims The Map to use to set the claims.
     * @return This JwtClaimsSetBuilder.
     */
    public JwtClaimsSetBuilder claims(Map<String, Object> claims) {
        this.claims.putAll(claims);
        return this;
    }

    /**
     * Sets the type of the contents of the Claims Set.
     * <p>
     * @see JwtClaimsSet#getType()
     *
     * @param typ The Claims Set content type.
     * @return This JwtClaimsSetBuilder.
     */
    public JwtClaimsSetBuilder typ(JwtType typ) {
        return claim("typ", typ);
    }

    /**
     * Sets the unique ID of the JWT.
     * <p>
     * @see JwtClaimsSet#setJwtId(String)
     *
     * @param jti The JWT's ID.
     * @return This JwtClaimsSetBuilder.
     */
    public JwtClaimsSetBuilder jti(String jti) {
        return claim("jti", jti);
    }

    /**
     * Sets the issuer this JWT was issued by.
     * <p>
     * @see JwtClaimsSet#setIssuer(String)
     *
     * @param iss The JWT's issuer.
     * @return This JwtClaimsSetBuilder.
     */
    public JwtClaimsSetBuilder iss(String iss) {
        return claim("iss", iss);
    }

    /**
     * Sets the issuer this JWT was issued by.
     * <p>
     * @see JwtClaimsSet#setIssuer(java.net.URI)
     *
     * @param iss The JWT's issuer.
     * @return This JwtClaimsSetBuilder.
     */
    public JwtClaimsSetBuilder iss(URI iss) {
        return claim("iss", iss);
    }

    /**
     * Sets the principal this JWT is issued to.
     * <p>
     * @see JwtClaimsSet#setPrincipal(String)
     *
     * @param prn The JWT's principal.
     * @return This JwtClaimsSetBuilder.
     */
    public JwtClaimsSetBuilder prn(String prn) {
        return claim("prn", prn);
    }

    /**
     * Sets the principal this JWT is issued to.
     * <p>
     * @see JwtClaimsSet#setPrincipal(java.net.URI)
     *
     * @param prn The JWT's principal.
     * @return This JwtClaimsSetBuilder.
     */
    public JwtClaimsSetBuilder prn(URI prn) {
        return claim("prn", prn);
    }

    /**
     * Sets the JWT's intended audience list in the Claims Set.
     * <p>
     * @see JwtClaimsSet#addAudience(String)
     *
     * @param aud The JWT's audience.
     * @return This JwtClaimsSetBuilder.
     */
    public JwtClaimsSetBuilder aud(List<String> aud) {
        return claim("aud", aud);
    }

    /**
     * Sets the time the JWT was issued at, in the Claims Set.
     * <p>
     * @see JwtClaimsSet#setIssuedAtTime(java.util.Date)
     *
     * @param iat The JWT's issued at time.
     * @return This JwtClaimsSetBuilder.
     */
    public JwtClaimsSetBuilder iat(Date iat) {
        return claim("iat", iat);
    }

    /**
     * Sets the time the JWT is not allowed to be processed before, in the Claims Set.
     * <p>
     * @see JwtClaimsSet#setNotBeforeTime(java.util.Date)
     *
     * @param nbf The JWT's not before time.
     * @return This JwtClaimsSetBuilder.
     */
    public JwtClaimsSetBuilder nbf(Date nbf) {
        return claim("nbf", nbf);
    }

    /**
     * Sets the expiration time of the JWT in the Claims Set.
     * <p>
     * @see JwtClaimsSet#setExpirationTime(java.util.Date)
     *
     * @param exp The JWT's expiration time.
     * @return This JwtClaimsSetBuilder.
     */
    public JwtClaimsSetBuilder exp(Date exp) {
        return claim("exp", exp);
    }

    /**
     * Creates a JwtClaimsSet instance from the claims set in this builder.
     *
     * @return A JwtClaimsSet instance.
     */
    public JwtClaimsSet build() {
        return new JwtClaimsSet(claims);
    }
}

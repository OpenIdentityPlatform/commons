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

package org.forgerock.json.jose.jwt;

import static java.util.Collections.*;
import static org.forgerock.json.jose.jwt.JwtClaimsSetKey.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.utils.IntDate;
import org.forgerock.json.jose.utils.StringOrURI;

/**
 * An implementation that holds a JWT's Claims Set.
 * <p>
 * Provides methods to set claims for all the reserved claim names as well as custom claims.
 *
 * @since 2.0.0
 */
public class JwtClaimsSet extends JWObject implements Payload {

    /**
     * Constructs a new, empty JwtClaimsSet.
     */
    public JwtClaimsSet() {
    }

    /**
     * Constructs a new JwtClaimsSet, with its claims set to the contents of the given Map.
     *
     * @param claims A Map containing the claims to be set in the Claims Set.
     */
    public JwtClaimsSet(Map<String, Object> claims) {
        setClaims(claims);
    }

    /**
     * Gets the type of the contents of the Claims Set.
     * <p>
     * The values used for this claim SHOULD come from the same value space as the JWT header parameter "typ",
     * with the same rules applying.
     *
     * @param type The Claims Set content type.
     */
    public void setType(String type) {
        put(TYP.value(), type);
    }

    /**
     * Gets the type of the contents of the Claims Set.
     * <p>
     * The values used for this claim SHOULD come from the same value space as the JWT header parameter "typ",
     * with the same rules applying.
     *
     * @return The Claims Set content type.
     */
    public String getType() {
        return get(TYP.value()).asString();
    }

    /**
     * Sets the unique ID of the JWT.
     *
     * @param jwtId The JWT's ID.
     */
    public void setJwtId(String jwtId) {
        put(JTI.value(), jwtId);
    }

    /**
     * Gets the unique ID of the JWT.
     *
     * @return The JWT's ID or {@code null} if claim not present.
     */
    public String getJwtId() {
        return get(JTI.value()).asString();
    }

    /**
     * Sets the issuer this JWT was issued by.
     * <p>
     * The given issuer can be any arbitrary string without any ":" characters, if the string does contain a ":"
     * character then it must be a valid URI.
     *
     * @param issuer The JWT's issuer.
     */
    public void setIssuer(String issuer) {
        StringOrURI.validateStringOrURI(issuer);
        put(ISS.value(), issuer);
    }

    /**
     * Sets the issuer this JWT was issued by.
     *
     * @param issuer The JWT's issuer.
     */
    public void setIssuer(URI issuer) {
        put(ISS.value(), issuer.toString());
    }

    /**
     *
     * Gets the issuer this JWT was issued by.
     *
     * @return The JWT's issuer or {@code null} if claim not present.
     */
    public String getIssuer() {
        return get(ISS.value()).asString();
    }

    /**
     * Sets the subject this JWT is issued to.
     * <p>
     * The given subject can be any arbitrary string without any ":" characters, if the string does contain a ":"
     * character then it must be a valid URI.
     *
     * @param subject The JWT's principal.
     * @see #setSubject(java.net.URI)
     */
    public void setSubject(String subject) {
        StringOrURI.validateStringOrURI(subject);
        put(SUB.value(), subject);
    }

    /**
     * Sets the subject this JWT is issued to.
     *
     * @param subject The JWT's principal.
     * @see #setSubject(String)
     */
    public void setSubject(URI subject) {
        put(SUB.value(), subject.toString());
    }

    /**
     * Gets the subject this JWT is issued to.
     *
     * @return The JWT's principal or {@code null} if claim not present.
     */
    public String getSubject() {
        return get(SUB.value()).asString();
    }

    /**
     * Adds an entry to the JWT's intended audience list, in the Claims Set.
     * <p>
     * The given audience can be any arbitrary string without any ":" characters, if the string does contain a ":"
     * character then it must be a valid URI.
     *
     * @param audience The JWT's intended audience.
     * @see #addAudience(java.net.URI)
     */
    public void addAudience(String audience) {
        StringOrURI.validateStringOrURI(audience);
        addAudienceWithTypeCheck(audience);
    }

    /**
     * Adds an entry to the JWT's intended audience list, in the Claims Set.
     *
     * @param audience The JWT's intended audience.
     * @see #addAudience(String)
     */
    public void addAudience(URI audience) {
        addAudienceWithTypeCheck(audience.toString());
    }

    private void addAudienceWithTypeCheck(String audience) {
        JsonValue audienceClaim = get(AUD.value());

        if (audienceClaim.isNull()) {
            put(AUD.value(), audience);
        } else if (audienceClaim.isList()) {
            audienceClaim.asList().add(audience);
        } else {
            List<String> audienceList = new ArrayList<>();
            audienceList.add(audienceClaim.asString());
            audienceList.add(audience);
            put(AUD.value(), audienceList);
        }
    }

    /**
     * Gets the intended audience for the JWT from the Claims Set.
     *
     * @return The JWT's intended audience or {@code null} if claim not present.
     */
    public List<String> getAudience() {
        JsonValue audience = get(AUD.value());
        if (audience.isNull()) {
            return null;
        } else if (audience.isList()) {
            return audience.asList(String.class);
        } else {
            return singletonList(audience.asString());
        }
    }

    /**
     * Sets the time the JWT was issued at, in the Claims Set.
     * <p>
     * The given date will be converted into an {@link IntDate} to be stored in the JWT Claims Set.
     *
     * @param issuedAtTime The JWT's issued at time.
     */
    public void setIssuedAtTime(Date issuedAtTime) {
        put(IAT.value(), IntDate.toIntDate(issuedAtTime));
    }

    /**
     * Sets the time the JWT was issued at, in the Claims Set.
     * <p>
     * This method takes a long representation of the number of <strong>seconds</strong> have passed since epoch.
     *
     * @param issuedAtTime The JWT's issued at time as a long in seconds.
     * @see #setIssuedAtTime(java.util.Date)
     */
    private void setIssuedAtTime(long issuedAtTime) {
        put(IAT.value(), issuedAtTime);
    }

    /**
     * Gets the time the JWT was issued at, from the Claims Set.
     *
     * @return The JWT's issued at time or {@code null} if claim not present.
     */
    public Date getIssuedAtTime() {
        return getDate(IAT.value());
    }

    /**
     * Sets the time the JWT is not allowed to be processed before, in the Claims Set.
     * <p>
     * The given date will be converted into an {@link IntDate} to be stored in the JWT Claims Set.
     *
     * @param notBeforeTime The JWT's not before time.
     */
    public void setNotBeforeTime(Date notBeforeTime) {
        put(NBF.value(), IntDate.toIntDate(notBeforeTime));
    }

    /**
     * Sets the time the JWT is not allowed to be processed before, in the Claims Set.
     * <p>
     * The method takes a long representation of the number of <strong>seconds</strong> have passed since epoch.
     *
     * @param notBeforeTime The JWT's not before time.
     * @see #setNotBeforeTime(java.util.Date)
     */
    private void setNotBeforeTime(long notBeforeTime) {
        put(NBF.value(), notBeforeTime);
    }

    /**
     * Gets the time the JWT is not allowed to be processed before, from the Claims Set.
     *
     * @return The JWT's not before time or {@code null} if claim not present.
     */
    public Date getNotBeforeTime() {
        return getDate(NBF.value());
    }

    /**
     * Sets the expiration time of the JWT in the Claims Set.
     * <p>
     * The given date will be converted into an {@link IntDate} to be stored in the JWT Claims Set.
     *
     * @param expirationTime The JWT's expiration time.
     */
    public void setExpirationTime(Date expirationTime) {
        put(EXP.value(), IntDate.toIntDate(expirationTime));
    }

    /**
     * Sets the expiration time of the JWT in the Claims Set.
     * <p>
     * This method takes a long representation of the number of <strong>seconds</strong> have passed since epoch.
     *
     * @param expirationTime The JWT's expiration time as a long in seconds.
     * @see #setExpirationTime(java.util.Date)
     */
    private void setExpirationTime(long expirationTime) {
        put(EXP.value(), expirationTime);
    }

    /**
     * Gets the expiration time of the JWT from the Claims Set.
     *
     * @return The JWT's expiration time or {@code null} if claim not present.
     */
    public Date getExpirationTime() {
        return getDate(EXP.value());
    }

    /**
     * Sets a claim with the specified name and value.
     * <p>
     * If the key matches one of the reserved claim names, then the relevant <tt>set</tt> method is called to set that
     * claim with the specified name and value.
     *
     * @param key The claim name.
     * @param value The claim value.
     */
    public void setClaim(String key, Object value) {

        JwtClaimsSetKey claimsSetKey = getClaimSetKey(key.toUpperCase());

        switch (claimsSetKey) {
        case TYP: {
            checkValueIsOfType(value, String.class);
            setType((String) value);
            break;
        }
        case JTI: {
            checkValueIsOfType(value, String.class);
            setJwtId((String) value);
            break;
        }
        case ISS: {
            if (isValueOfType(value, URI.class)) {
                setIssuer((URI) value);
            } else {
                checkValueIsOfType(value, String.class);
                setIssuer((String) value);
            }
            break;
        }
        case SUB: {
            if (isValueOfType(value, URI.class)) {
                setSubject((URI) value);
            } else {
                checkValueIsOfType(value, String.class);
                setSubject((String) value);
            }
            break;
        }
        case AUD: {
            if (isValueOfType(value, List.class)) {
                List<?> audienceList = (List<?>) value;
                for (Object audience : audienceList) {
                    if (isValueOfType(audience, URI.class)) {
                        addAudience((URI) audience);
                    } else {
                        checkValueIsOfType(audience, String.class);
                        addAudience((String) audience);
                    }
                }
            } else {
                if (isValueOfType(value, URI.class)) {
                    addAudience((URI) value);
                } else {
                    checkValueIsOfType(value, String.class);
                    addAudience((String) value);
                }
            }
            break;
        }
        case IAT: {
            if (isValueOfType(value, Number.class)) {
                setIssuedAtTime(((Number) value).longValue());
            } else {
                checkValueIsOfType(value, Date.class);
                setIssuedAtTime((Date) value);
            }
            break;
        }
        case NBF: {
            if (isValueOfType(value, Number.class)) {
                setNotBeforeTime(((Number) value).longValue());
            } else {
                checkValueIsOfType(value, Date.class);
                setNotBeforeTime((Date) value);
            }
            break;
        }
        case EXP: {
            if (isValueOfType(value, Number.class)) {
                setExpirationTime(((Number) value).longValue());
            } else {
                checkValueIsOfType(value, Date.class);
                setExpirationTime((Date) value);
            }
            break;
        }
        default: {
            put(key, value);
        }
        }
    }

    /**
     * Sets claims using the values contained in the specified map.
     *
     * @param claims The Map to use to set the claims.
     */
    public void setClaims(Map<String, Object> claims) {
        for (String key : claims.keySet()) {
            setClaim(key, claims.get(key));
        }
    }

    /**
     * Gets a claim value for the specified key.
     * <p>
     * If the key matches one of the reserved claim names, then the relevant <tt>get</tt> method is called to get that
     * claim value.
     *
     * @param key The claim name.
     * @return The value stored against the claim name.
     */
    public Object getClaim(String key) {

        JwtClaimsSetKey claimsSetKey = getClaimSetKey(key.toUpperCase());

        Object value;

        switch (claimsSetKey) {
        case TYP: {
            value = getType();
            break;
        }
        case JTI: {
            value = getJwtId();
            break;
        }
        case ISS: {
            value = getIssuer();
            break;
        }
        case SUB: {
            value = getSubject();
            break;
        }
        case AUD: {
            value = getAudience();
            break;
        }
        case IAT: {
            value = getIssuedAtTime();
            break;
        }
        case NBF: {
            value = getNotBeforeTime();
            break;
        }
        case EXP: {
            value = getExpirationTime();
            break;
        }
        default: {
            value = get(key).getObject();
        }
        }

        return value;
    }

    /**
     * Gets a claim value for the specified claim name and then casts it to the specified type.
     *
     * @param key The claim name.
     * @param clazz The class of the required type.
     * @param <T> The required type for the claim value.
     * @return The value stored against the claim name.
     * @see #getClaim(String)
     */
    public <T> T getClaim(String key, Class<T> clazz) {
        return clazz.cast(getClaim(key));
    }

    /**
     * Builds the JWT's Claims Set into a <code>String</code> representation of a JSON object.
     *
     * @return A JSON string.
     */
    public String build() {
        return toString();
    }

    /**
     * Returns the specified item value as a {@link Date}. If no such member value exists, then a JSON value containing
     * {@code null} is returned.
     *
     * @param key the {@code Map} key identifying the item to return.
     * @return a {@link Date} representing the value or {@code null}.
     */
    private Date getDate(final String key) {
        final JsonValue value = get(key);
        return value.isNull() ? null : IntDate.fromIntDate(value.asLong());
    }
}

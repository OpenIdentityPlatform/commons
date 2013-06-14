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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.json.jose.jwt;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.forgerock.json.jose.jwt.JwtClaimsSetKey.*;

public class JwtClaimsSet extends JWObject {

    public JwtClaimsSet() {
    }

    public JwtClaimsSet(Map<String, Object> claims) {
        setClaims(claims);
    }

    public void setType(String type) {
        put(TYP.value(), type);
    }

    public String getType() {
        return get(TYP.value()).asString();
    }

    public void setJwtId(String jwtId) {
        put(JTI.value(), jwtId);
    }

    public String getJwtId() {
        return get(JTI.value()).asString();
    }

    public void setIssuer(String issuer) {
        put(ISS.value(), issuer);
    }

    public void setIssuer(URI issuer) {
        put(ISS.value(), issuer.toString());
    }

    public String getIssuer() {
        return get(ISS.value()).asString();
    }

    public void setPrincipal(String principal) {
        put(PRN.value(), principal);
    }

    public void setPrincipal(URI principal) {
        put(PRN.value(), principal.toString());
    }

    public String getPrincipal() {
        return get(PRN.value()).asString();
    }

    public void addAudience(String audience) {
        List<String> audienceList = getAudience();
        if (audienceList == null) {
            audienceList = new ArrayList<String>();
            put(AUD.value(), audienceList);
        }
        audienceList.add(audience);
    }

    public void addAudience(URI audience) {
        addAudience(audience.toString());
    }

    public List<String> getAudience() {
        return get(AUD.value()).asList(String.class);
    }

    public void setIssuedAtTime(Date issuedAtTime) {
        put(IAT.value(), issuedAtTime.getTime() / 1000L);
    }

    private void setIssuedAtTime(long expirationTime) {
        put(IAT.value(), expirationTime * 1000L);
    }

    public Date getIssuedAtTime() {    //TODO check not null!
        return new Date(get(IAT.value()).asLong());
    }

    public void setNotBeforeTime(Date notBeforeTime) {
        put(NBF.value(), notBeforeTime.getTime() / 1000L);
    }

    private void setNotBeforeTime(long expirationTime) {
        put(NBF.value(), expirationTime * 1000L);
    }

    public Date getNotBeforeTime() {
        return new Date(get(NBF.value()).asLong());
    }

    public void setExpirationTime(Date expirationTime) {
        put(EXP.value(), expirationTime.getTime() / 1000L);  //TODO Use class level Calendar and method to set millis to 0
    }

    private void setExpirationTime(long expirationTime) {
        put(EXP.value(), expirationTime * 1000L);
    }

    public Date getExpirationTime() {
        return new Date(get(EXP.value()).asLong());
    }

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
            case PRN: {
                if (isValueOfType(value, URI.class)) {
                    setPrincipal((URI) value);
                } else {
                    checkValueIsOfType(value, String.class);
                    setPrincipal((String) value);
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

    public void setClaims(Map<String, Object> claims) {
        for (String key : claims.keySet()) {
            setClaim(key, claims.get(key));
        }
    }

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
            case PRN: {
                value = getPrincipal();
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

    public <T> T getClaim(String key, Class<T> clazz) {
        return (T) getClaim(key);
    }

    public String build() {
        return toString();
    }
}

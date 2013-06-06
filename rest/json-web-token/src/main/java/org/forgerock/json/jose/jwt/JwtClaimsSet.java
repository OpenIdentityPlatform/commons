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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.utils.StringOrURI;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JwtClaimsSet extends JsonValue {

    private String type;
    private String jwtId;
    private StringOrURI issuer;
    private StringOrURI principal;
    private Set<StringOrURI> audience = new HashSet<StringOrURI>();
    private Long issuedAtTime;
    private Long notBeforeTime;
    private Long expirationTime;

//    public final Map<String, Object> claims = new HashMap<String, Object>();

    public JwtClaimsSet() {
        //TODO need to convert Dates to Longs!!
        super(new HashMap<String, Object>());
    }

    public JwtClaimsSet(JsonValue value) {
        super(value);
    }

    public JwtClaimsSet(Map<String, Object> claims) {
        super(claims);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setJwtId(String jwtId) {
        this.jwtId = jwtId;
    }

    public String getJwtId() {
        return jwtId;
    }

    public void setIssuer(String issuer) {
        this.issuer = new StringOrURI(issuer);
    }

    public void setIssuer(URI issuer) {
        this.issuer = new StringOrURI(issuer);
    }

    public String getIssuer() {
        return issuer.toString();
    }

    public void setPrincipal(String principal) {
        this.principal = new StringOrURI(principal);
    }

    public void setPrincipal(URI principal) {
        this.principal = new StringOrURI(principal);
    }

    public String getPrincipal() {
        return principal.toString();
    }

    public void addAudience(String audience) {
        this.audience.add(new StringOrURI(audience));
    }

    public void addAudience(URI audience) {
        this.audience.add(new StringOrURI(audience));
    }

    public Set<String> getAudience() {
        Set<String> aud = new HashSet<String>();

        for (StringOrURI s : audience) {
            aud.add(s.toString());
        }

        return aud;
    }

    public void setIssuedAtTime(Date issuedAtTime) {
        this.issuedAtTime = issuedAtTime.getTime();
    }

    public Date getIssuedAtTime() {
        return new Date(issuedAtTime);
    }

    public void setNotBeforeTime(Date notBeforeTime) {
        this.notBeforeTime = notBeforeTime.getTime();
    }

    public Date getNotBeforeTime() {
        return new Date(notBeforeTime);
    }

    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime.getTime();
    }

    public Date getExpirationTime() {
        return new Date(expirationTime);
    }

    public void addClaim(String key, Object value) {
        put(key, value);          //TODO include required claims
    }

    public void addClaims(Map<String, Object> claims) {
        for (String key : claims.keySet()) {
            addClaim(key, claims.get(key));
        }
    }

    public Object getClaim(String key) {
        return get(key);                     //TODO include required claims
    }

    public <T> T getClaim(String key, T clazz) {
        return (T) getClaim(key);
    }

    public Map<String, Object> getClaims() {
        Map<String, Object> claims = new HashMap<String, Object>();
        for (String key : keys()) {
            claims.put(key, get(key));
        }
        return claims;//TODO include required claims
    }

    public String build() {
        JsonValue jsonValue = new JsonValue(this);

        addClaimToJson(jsonValue, "typ", type);
        addClaimToJson(jsonValue, "jwi", jwtId);
        addClaimToJson(jsonValue, "iss", issuer);
        addClaimToJson(jsonValue, "prn", principal);
        if (audience.size() > 0) {
            addClaimToJson(jsonValue, "aud", audience);
        }
        addClaimToJson(jsonValue, "iat", issuedAtTime);
        addClaimToJson(jsonValue, "nbf", notBeforeTime);
        addClaimToJson(jsonValue, "exp", expirationTime);

        return jsonValue.toString();
    }

    private void addClaimToJson(JsonValue jsonValue, String key, Object value) {
        if (value != null) {
            jsonValue.add(key, value);
        }
    }
}

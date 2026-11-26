//@Checkstyle:ignoreFor 30
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: WindowsDesktopSSO.java,v 1.7 2009/07/28 19:40:45 beomsuk Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions copyright 2020-2026 3A Systems LLC.
 */

package org.forgerock.jaspi.modules.iwa.wdsso;

import static org.forgerock.caf.authentication.framework.AuthenticationFramework.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.forgerock.http.protocol.Request;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.crypto.SecretHash;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;

/**
 * Windows Desktop Single Sign On implementation, extracted from OpenAM.
 */
public class WDSSO {

    private Principal userPrincipal = null;
    private Subject serviceSubject = null;
    private String servicePrincipalName = null;
    private String keyTabFile = null;
    private boolean returnRealm = false;

    /**
     * Constructor.
     */
    public WDSSO() {
    }

    /**
     * Initialize parameters.
     *
     * @param subject The subject.
     * @param sharedState The shared state.
     * @param options The options.
     */
    public void init(Subject subject, Map sharedState, Map options) {

    }

    /**
     * Returns principal of the authenticated user.
     *
     * @return Principal of the authenticated user.
     */
    public Principal getPrincipal() {
        return userPrincipal;
    }

    /**
     * Process the login attempt.
     * @param options The options.
     * @param context The context.
     * @param request The request.
     * @return The result.
     * @throws Exception If an error occurred.
     */
    public String process(Map<String, String> options, Context context, Request request) throws Exception {
        // Check to see if the Rest Auth Endpoint has signified that IWA has failed.
        if (hasWDSSOFailed(context)) {
            throw new IllegalStateException("IWA already failed for this request");
        }

        // retrieve the spnego token
        byte[] spnegoToken = getSPNEGOTokenFromHTTPRequest(request);
        if (spnegoToken == null) {
            LOG.error("IWA WDSSO: spnego token is not valid.");
            throw new RuntimeException();
        }

        // parse the spnego token and extract the kerberos mech token from it
        final byte[] kerberosToken = parseToken(spnegoToken);
        if (kerberosToken == null || kerberosToken.length == 0) {
            LOG.error("IWA WDSSO: kerberos token is not valid.");
            throw new RuntimeException();
        }

        String principal = getServicePrincipalName(options);
        String keytab = options.get("keytabFileName");
        Subject subject = getServiceSubject(principal, keytab, null);

        // authenticate the user with the kerberos token
        try {
            return authenticateToken(subject, kerberosToken);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof GSSException) {
                int major = ((GSSException) e).getMajor();
                if (major == GSSException.CREDENTIALS_EXPIRED) {
                    LOG.debug("IWA WDSSO: Credential expired. Re-establish credential... {}", e.getMessage());
                    Subject refreshed = getServiceSubject(principal, keytab, subject);
                    try {
                        String username = authenticateToken(refreshed, kerberosToken);
                        LOG.debug("IWA WDSSO: Authentication succeeded with new cred.");
                        return username;
                    } catch (Exception ee) {
                        LOG.error("IWA WDSSO: Authentication failed with new cred. {}", e.getMessage(), e);
                        throw ee;
                    }
                } else {
                    LOG.error("IWA WDSSO: Authentication failed with GSSException. {}", e.getMessage(), e);
                    throw new RuntimeException();
                }
            }
            throw e;
        } catch (GSSException e) {
            int major = e.getMajor();
            if (major == GSSException.CREDENTIALS_EXPIRED) {
                LOG.debug("IWA WDSSO: Credential expired. Re-establish credential... {}", e.getMessage());
                Subject refreshed = getServiceSubject(principal, keytab, subject);
                try {
                    String username = authenticateToken(refreshed, kerberosToken);
                    LOG.debug("IWA WDSSO: Authentication succeeded with new cred.");
                    return username;
                } catch (Exception ee) {
                    LOG.debug("IWA WDSSO: Authentication failed with new cred. {}", e.getMessage());
                    throw ee;
                }
            } else {
                LOG.debug("IWA WDSSO: Authentication failed with GSSException. {}", e.getMessage());
                throw new RuntimeException();
            }
        } catch (Exception e) {
            LOG.error("IWA WDSSO: Authentication failed with generic exception. {}", e.getMessage(), e);
            throw e;
        }
    }

    private String authenticateToken(Subject subject, final byte[] kerberosToken) throws Exception {
        return Subject.doAs(subject, new PrivilegedExceptionAction<String>() {
            @Override
            public String run() throws GSSException {
                GSSContext context = createGssContext();
                Throwable failure = null;
                try {
                    LOG.debug("IWA WDSSO: GSSContext created");
                    byte[] outToken = context.acceptSecContext(kerberosToken, 0, kerberosToken.length);
                    if (outToken != null) {
                        LOG.debug("IWA WDSSO: Token returned from acceptSecContext: {}",
                                DerValue.printByteArray(outToken, 0, outToken.length));
                    }
                    if (!context.isEstablished()) {
                        LOG.debug("IWA WDSSO: Cannot establish context!");
                        throw new RuntimeException();
                    }
                    LOG.debug("IWA WDSSO: Context established");
                    String username = getUserName(context.getSrcName().toString());
                    LOG.debug("IWA WDSSO: Found user! {}", username);
                    return username;
                } catch (GSSException | RuntimeException | Error e) {
                    failure = e;
                    throw e;
                } finally {
                    try {
                        context.dispose();
                    } catch (GSSException | RuntimeException | Error cleanupFailure) {
                        if (failure == null) {
                            throw cleanupFailure;
                        }
                        if (cleanupFailure != failure) {
                            failure.addSuppressed(cleanupFailure);
                        }
                    }
                }
            }
        });
    }

    GSSContext createGssContext() throws GSSException {
        return GSSManager.getInstance().createContext((GSSCredential) null);
    }

    /**
     * Iterate until we extract the real exception
     * from PrivilegedActionException(s).
     */
    private static Exception extractException(Exception e) {
        while (e instanceof PrivilegedActionException) {
            e = ((PrivilegedActionException) e).getException();
        }
        return e;
    }

    /**
     * Destroy the state.
     */
    public void destroyModuleState() {
        userPrincipal = null;
    }

    /**
     * Nullify any variables that might have been set.
     */
    public synchronized void nullifyUsedVars() {
        serviceSubject = null;
        servicePrincipalName = null;
        keyTabFile = null;
    }

    private String getUserName(String user) {
        String userName = user;
        if (!returnRealm) {
            int index = user.indexOf("@");
            if (index != -1) {
                userName = user.substring(0, index);
            }
        }
        return userName;
    }

    private static final byte[] SPNEGO_OID = {
        (byte) 0x06, (byte) 0x06, (byte) 0x2b, (byte) 0x06, (byte) 0x01,
        (byte) 0x05, (byte) 0x05, (byte) 0x02 };

    private static final byte[] KERBEROS_V5_OID = {
        (byte) 0x06, (byte) 0x09, (byte) 0x2a, (byte) 0x86, (byte) 0x48,
        (byte) 0x86, (byte) 0xf7, (byte) 0x12, (byte) 0x01, (byte) 0x02,
        (byte) 0x02 };

    /**
     * Checks the request for an attribute "iwa-failed".
     *
     * @param context The message info context.
     * @return If the attribute is present and set to true true is returned otherwise false is returned.
     */
    private boolean hasWDSSOFailed(Context context) {
        return Boolean.valueOf((String) context.asContext(AttributesContext.class).getAttributes().get("iwa-failed"));
    }

    private byte[] getSPNEGOTokenFromHTTPRequest(Request req) {
        byte[] spnegoToken = null;
        String header = req.getHeaders().getFirst("Authorization");
        if ((header != null) && header.startsWith("Negotiate")) {
            header = header.substring("Negotiate".length()).trim();
            // Log only a one-way hash of the SPNEGO/Authorization token, never the value (CWE-532: sensitive data in logs).
            if (LOG.isDebugEnabled()) {
                LOG.debug("IWA WDSSO: \"Authorization\" header set, token hash={}", SecretHash.hash(header));
            }
            try {
                spnegoToken = Base64.decode(header);
            } catch (Exception e) {
                LOG.error("IWA WDSSO: Failed to get SPNEGO Token from request");
            }
        } else {
            LOG.error("IWA WDSSO: \"Authorization\" header not set in reqest");
        }
        return spnegoToken;
    }

    private byte[] parseToken(byte[] rawToken) {
        byte[] token = null;
        DerValue tmpToken = new DerValue(rawToken);
        if (tmpToken.getTag() != (byte) 0x60) {
            return null;
        }

        ByteArrayInputStream tmpInput = new ByteArrayInputStream(
                tmpToken.getData());

        // check for SPNEGO OID
        byte[] oidArray = new byte[SPNEGO_OID.length];
        final int oidRead = tmpInput.read(oidArray, 0, oidArray.length);
        if (oidRead == oidArray.length && Arrays.equals(oidArray, SPNEGO_OID)) {
            tmpToken = new DerValue(tmpInput);

            // 0xa0 indicates an init token(NegTokenInit); 0xa1 indicates an
            // response arg token(NegTokenTarg). no arg token is needed for us.

            if (tmpToken.getTag() == (byte) 0xa0) {
                tmpToken = new DerValue(tmpToken.getData());
                if (tmpToken.getTag() == (byte) 0x30) {
                    tmpInput = new ByteArrayInputStream(tmpToken.getData());
                    tmpToken = new DerValue(tmpInput);

                    // In an init token, it can contain 4 optional arguments:
                    // a0: mechTypes
                    // a1: contextFlags
                    // a2: octect string(with leading char 0x04) for the token
                    // a3: message integrity value

                    while (tmpToken.getTag() != (byte) -1
                            && tmpToken.getTag() != (byte) 0xa2) {
                        // look for next mech token DER
                        tmpToken = new DerValue(tmpInput);
                    }
                    if (tmpToken.getTag() != (byte) -1) {
                        // retrieve octet string
                        tmpToken = new DerValue(tmpToken.getData());
                        if (tmpToken.getTag() == (byte) 0x04) {
                            token = tmpToken.getData();
                        }
                    }
                }
            }
        } else {
            LOG.debug("IWA WDSSO: SPENGO OID not found in the Auth Token");
            byte[] krb5Oid = new byte[KERBEROS_V5_OID.length];
            int i = 0;
            for (; i < oidArray.length; i++) {
                krb5Oid[i] = oidArray[i];
            }
            final int krb5Read = tmpInput.read(krb5Oid, i, krb5Oid.length - i);
            if (krb5Read != krb5Oid.length - i || !Arrays.equals(krb5Oid, KERBEROS_V5_OID)) {
                LOG.debug("IWA WDSSO: Kerberos V5 OID not found in the Auth Token");
            } else {
                LOG.debug("IWA WDSSO: Kerberos V5 OID found in the Auth Token");
                token = rawToken;
            }
        }
        return token;
    }

    private static String getServicePrincipalName(Map<String, String> options) {
        String principal = options.get("servicePrincipal");
        if (principal == null || principal.isEmpty() || "*".equals(principal)) {
            throw new IllegalArgumentException("A specific IWA service principal is required");
        }
        int realmSeparator = principal.indexOf('@');
        while (realmSeparator > 0 && principal.charAt(realmSeparator - 1) == '\\') {
            realmSeparator = principal.indexOf('@', realmSeparator + 1);
        }
        if (realmSeparator >= 0) {
            return principal;
        }
        String realm = options.get("kerberosRealm");
        if (realm == null || realm.isEmpty()) {
            throw new IllegalArgumentException("An IWA Kerberos realm is required for an unqualified service principal");
        }
        return principal + "@" + realm;
    }

    private synchronized Subject getServiceSubject(String principal, String keytab, Subject expired)
            throws LoginException {
        boolean sameConfiguration = principal.equals(servicePrincipalName) && Objects.equals(keytab, keyTabFile);
        if (sameConfiguration && serviceSubject == null && expired != null) {
            throw new LoginException("The IWA credentials could not be refreshed");
        }
        // A refresh can reuse credentials that another request has already replaced.
        if (!sameConfiguration || serviceSubject == null || serviceSubject == expired) {
            servicePrincipalName = principal;
            keyTabFile = keytab;
            serviceSubject = null;
            serviceSubject = serviceLogin(principal, keytab);
        }
        return serviceSubject;
    }

    Subject serviceLogin(String principal, String keytab) throws LoginException {
        if (keytab == null || keytab.isEmpty()) {
            throw new IllegalArgumentException("An IWA keytab is required");
        }
        if (!new File(keytabPath(keytab)).exists()) {
            throw new IllegalArgumentException("The IWA keytab does not exist");
        }

        WindowsDesktopSSOConfig config = new WindowsDesktopSSOConfig(null);
        config.setPrincipalName(principal);
        config.setKeyTab(keytab);

        LoginContext login = new LoginContext(WindowsDesktopSSOConfig.DEFAULT_APP_NAME, null, null, config);
        login.login();
        Subject subject = login.getSubject();
        if (!subject.getPrincipals(KerberosPrincipal.class).contains(new KerberosPrincipal(principal))) {
            throw new LoginException("The IWA credentials do not match the configured service principal");
        }
        return subject;
    }

    private static String keytabPath(String keytab) {
        for (String prefix : List.of("FILE:", "ANY:FILE:", "SRVTAB:")) {
            if (keytab.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return keytab.substring(prefix.length());
            }
        }
        return keytab;
    }

}

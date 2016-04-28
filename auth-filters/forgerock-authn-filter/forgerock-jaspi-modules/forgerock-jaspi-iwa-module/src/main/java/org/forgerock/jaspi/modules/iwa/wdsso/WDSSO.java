//@Checkstyle:ignoreFor 29
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
 */

package org.forgerock.jaspi.modules.iwa.wdsso;

import static org.forgerock.caf.authentication.framework.AuthenticationFramework.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;

import org.forgerock.http.protocol.Request;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;

/**
 * Windows Desktop Single Sign On implementation, extracted from OpenAM.
 */
public class WDSSO {

    private Principal userPrincipal = null;
    private Subject serviceSubject = null;
    private String servicePrincipalName = null;
    private String keyTabFile = null;
    private String kdcRealm   = null;
    private String kdcServer  = null;
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

    private static String user;

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
        if (hasWDSSOFailed(context)) {                 //TODO this is not required in the context of IB/IDM
            return "SEND_CONTINUE";
        }

        if (!getConfigParams(options)) {
            initWindowsDesktopSSOAuth(options);
        }

        // retrieve the spnego token
        byte[] spnegoToken = getSPNEGOTokenFromHTTPRequest(request);
        if (spnegoToken == null) {
            LOG.error("IWA WDSSO: spnego token is not valid.");
            throw new RuntimeException();
        }

        // parse the spnego token and extract the kerberos mech token from it
        final byte[] kerberosToken = parseToken(spnegoToken);
        if (kerberosToken == null) {
            LOG.error("IWA WDSSO: kerberos token is not valid.");
            throw new RuntimeException();
        }

        // authenticate the user with the kerberos token
        try {
            authenticateToken(kerberosToken);
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if (e instanceof GSSException) {
                int major = ((GSSException) e).getMajor();
                if (major == GSSException.CREDENTIALS_EXPIRED) {
                    LOG.debug("IWA WDSSO: Credential expired. Re-establish credential... {}", e.getMessage());
                    serviceLogin();
                    try {
                        authenticateToken(kerberosToken);
                        LOG.debug("IWA WDSSO: Authentication succeeded with new cred.");
                    } catch (Exception ee) {
                        LOG.error("IWA WDSSO: Authentication failed with new cred. {}", e.getMessage(), e);
                        throw ee;
                    }
                } else {
                    LOG.error("IWA WDSSO: Authentication failed with GSSException. {}", e.getMessage(), e);
                    throw new RuntimeException();
                }
            }
        } catch (GSSException e) {
            int major = e.getMajor();
            if (major == GSSException.CREDENTIALS_EXPIRED) {
                LOG.debug("IWA WDSSO: Credential expired. Re-establish credential... {}", e.getMessage());
                serviceLogin();
                try {
                    authenticateToken(kerberosToken);
                    LOG.debug("IWA WDSSO: Authentication succeeded with new cred.");
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
        return user;
    }

    private void authenticateToken(final byte[] kerberosToken) throws Exception {
        Subject.doAs(serviceSubject, new PrivilegedExceptionAction() {
            public Object run() throws Exception {
                GSSContext context = GSSManager.getInstance().createContext((GSSCredential) null);
                LOG.debug("IWA WDSSO: GSSContext created");
                byte[] outToken = context.acceptSecContext(
                        kerberosToken, 0, kerberosToken.length);
                if (outToken != null) {
                    LOG.debug("IWA WDSSO: Token returned from acceptSecContext: {}",
                            DerValue.printByteArray(outToken, 0, outToken.length));
                }
                if (!context.isEstablished()) {
                    LOG.debug("IWA WDSSO: Cannot establish context!");
                    throw new RuntimeException();
                } else {
                    LOG.debug("IWA WDSSO: Context established");
                    GSSName user = context.getSrcName();
                    WDSSO.user = getUserName(user.toString());
                    LOG.debug("IWA WDSSO: Found user! {}", WDSSO.user);
                }
                context.dispose();
                return null;
            }
        });
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
    public void nullifyUsedVars() {
        serviceSubject = null;
        servicePrincipalName = null;
        keyTabFile = null;
        kdcRealm = null;
        kdcServer = null;
    }

    private String getUserName(String user) {
        String userName = user;
        if (!returnRealm) {
            int index = user.indexOf("@");
            if (index != -1) {
                userName = user.toString().substring(0, index);
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
            LOG.debug("IWA WDSSO: \"Authorization\" header set, {}", header);
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
        byte[] token = rawToken;
        DerValue tmpToken = new DerValue(rawToken);
        if (tmpToken.getTag() != (byte) 0x60) {
            return null;
        }

        ByteArrayInputStream tmpInput = new ByteArrayInputStream(
                tmpToken.getData());

        // check for SPNEGO OID
        byte[] oidArray = new byte[SPNEGO_OID.length];
        tmpInput.read(oidArray, 0, oidArray.length);
        if (Arrays.equals(oidArray, SPNEGO_OID)) {
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
                        token = tmpToken.getData();
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
            tmpInput.read(krb5Oid, i, krb5Oid.length - i);
            if (!Arrays.equals(krb5Oid, KERBEROS_V5_OID)) {
                LOG.debug("IWA WDSSO: Kerberos V5 OID not found in the Auth Token");
                token = null;
            } else {
                LOG.debug("IWA WDSSO: Kerberos V5 OID found in the Auth Token");
            }
        }
        return token;
    }

    private boolean getConfigParams(Map<String, String> options) {
        // KDC realm in service principal must be uppercase.
        servicePrincipalName = options.get("servicePrincipal");
        keyTabFile = options.get("keytabFileName");
        kdcRealm = options.get("kerberosRealm");
        kdcServer = options.get("kerberosServerName");
        LOG.debug("IWA WDSSO: WindowsDesktopSSO params: principal: {}, keytab file: {}, realm : {}, kdc server: {}",
                servicePrincipalName, keyTabFile, kdcRealm, kdcServer);
        return serviceSubject != null;
    }

    private void initWindowsDesktopSSOAuth(Map options) throws Exception {
        verifyAttributes();
        serviceLogin();
    }

    private synchronized void serviceLogin() throws Exception {
        System.setProperty("java.security.krb5.realm", kdcRealm);
        System.setProperty("java.security.krb5.kdc", kdcServer);

        try {
            Configuration config = Configuration.getConfiguration();
            WindowsDesktopSSOConfig wtc = null;
            if (config instanceof WindowsDesktopSSOConfig) {
                wtc = (WindowsDesktopSSOConfig) config;
                wtc.setRefreshConfig("true");
            } else {
                wtc = new WindowsDesktopSSOConfig(config);
            }
            wtc.setPrincipalName(servicePrincipalName);
            wtc.setKeyTab(keyTabFile);
            Configuration.setConfiguration(wtc);

            // perform service authentication using JDK Kerberos module
            LoginContext lc = new LoginContext(WindowsDesktopSSOConfig.DEFAULT_APP_NAME);
            LOG.debug("IWA WDSSO: Attempting to run login() on the LoginContext");
            lc.login();
            LOG.debug("IWA WDSSO: LoginContext.login() successful");

            serviceSubject = lc.getSubject();
        } catch (Exception e) {
            LOG.error("IWA WDSSO: Service Login Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void verifyAttributes() throws RuntimeException {
        if (servicePrincipalName == null || servicePrincipalName.length() == 0) {
            LOG.error("IWA WDSSO: Service Principal Name not set");
            throw new RuntimeException();
        }
        if (keyTabFile == null || keyTabFile.length() == 0) {
            LOG.error("IWA WDSSO: Key Tab File not set");
            throw new RuntimeException();
        }
        if (kdcRealm == null || kdcRealm.length() == 0) {
            LOG.error("IWA WDSSO: Kerberos Realm not set");
            throw new RuntimeException();
        }
        if (kdcServer == null || kdcServer.length() == 0) {
            LOG.error("IWA WDSSO: Kerberos Server Name not set");
            throw new RuntimeException();
        }

        if (!(new File(keyTabFile)).exists()) {
            // ibm jdk needs to skip "file://" part in parameter
            if (!(new File(keyTabFile.substring(7))).exists()) {
                LOG.error("IWA WDSSO: Key Tab File does not exist");
                throw new RuntimeException();
            }
        }
    }

}

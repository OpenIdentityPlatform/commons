/**
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
 */

/**
 * Portions Copyrighted 2011-2013 ForgeRock, Inc
 */

package org.forgerock.jaspi.modules.iwa.wdsso;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.auth.common.DebugLogger;
import org.forgerock.jaspi.logging.LogFactory;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;

public class WDSSO /*extends AMLoginModule*/ {

    private static final DebugLogger LOGGER = LogFactory.getDebug();

    private static final String amAuthWindowsDesktopSSO =
            "amAuthWindowsDesktopSSO";

    private static final String[] configAttributes = {
            "iplanet-am-auth-windowsdesktopsso-principal-name",
            "iplanet-am-auth-windowsdesktopsso-keytab-file",
            "iplanet-am-auth-windowsdesktopsso-kerberos-realm",
            "iplanet-am-auth-windowsdesktopsso-kdc",
            "iplanet-am-auth-windowsdesktopsso-returnRealm",
            "iplanet-am-auth-windowsdesktopsso-lookupUserInRealm",
            "iplanet-am-auth-windowsdesktopsso-auth-level",
            "serviceSubject" };

    private static final int PRINCIPAL = 0;
    private static final int KEYTAB    = 1;
    private static final int REALM     = 2;
    private static final int KDC       = 3;
    private static final int RETURNREALM = 4;
    private static final int LOOKUPUSER = 5;
    private static final int AUTHLEVEL = 6;
    private static final int SUBJECT   = 7;

    private static Hashtable configTable = new Hashtable();
    private Principal userPrincipal = null;
    private Subject serviceSubject = null;
    private String servicePrincipalName = null;
    private String keyTabFile = null;
    private String kdcRealm   = null;
    private String kdcServer  = null;
    private boolean returnRealm = false;
    private String authLevel  = null;
    private Map    options    = null;
    private String confIndex  = null;
    private boolean lookupUserInRealm = false;

//    private Debug debug = Debug.getInstance(amAuthWindowsDesktopSSO);

    /**
     * Constructor
     */
    public WDSSO() {
    }

    /**
     * Initialize parameters. 
     *
     * @param subject
     * @param sharedState
     * @param options
     */
    public void init(Subject subject, Map sharedState, Map options) {
        this.options = options;
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
    public String process(Map<String, String> options, HttpServletRequest request) throws Exception {

//        try {

//        int result = ISAuthConstants.LOGIN_IGNORE;

        // Check to see if the Rest Auth Endpoint has signified that IWA has failed.
        if (hasWDSSOFailed(request)) {                 //TODO this is not required in the context of IB/IDM
//            return ISAuthConstants.LOGIN_IGNORE;
//            return AuthStatus.SEND_CONTINUE;
            return "SEND_CONTINE";
        }

        if (!getConfigParams(options)) {
            initWindowsDesktopSSOAuth(options);
        }

        // retrieve the spnego token
        byte[] spnegoToken = getSPNEGOTokenFromHTTPRequest(request);
//        if (spnegoToken == null) {
//            spnegoToken = getSPNEGOTokenFromCallback(callbacks);
//        }

        if (spnegoToken == null) {
            LOGGER.error("IWA WDSSO: spnego token is not valid.");
            throw new RuntimeException();
        }

//        if (debug.messageEnabled()) {
//            debug.message("SPNEGO token: \n" +
//                    DerValue.printByteArray(spnegoToken, 0, spnegoToken.length));
//        }
        // parse the spnego token and extract the kerberos mech token from it
        final byte[] kerberosToken = parseToken(spnegoToken);
        if (kerberosToken == null) {
            LOGGER.error("IWA WDSSO: kerberos token is not valid.");
            throw new RuntimeException();
//            return AuthStatus.SEND_FAILURE;
        }
//        if (debug.messageEnabled()) {
//            debug.message("Kerberos token retrieved from SPNEGO token: \n" +
//                    DerValue.printByteArray(kerberosToken,0,kerberosToken.length));
//        }

        // authenticate the user with the kerberos token
        try {
            authenticateToken(kerberosToken);
//            if (debug.messageEnabled()){
//                debug.message("WindowsDesktopSSO kerberos authentication passed succesfully.");
//            }
//            result = ISAuthConstants.LOGIN_SUCCEED;
        } catch (PrivilegedActionException pe) {
            Exception e = extractException(pe);
            if( e instanceof GSSException) {
                int major = ((GSSException)e).getMajor();
                if (major == GSSException.CREDENTIALS_EXPIRED) {
                    LOGGER.debug("IWA WDSSO: Credential expired. Re-establish credential... " + e.getMessage());
                    serviceLogin();
                    try {
                        authenticateToken(kerberosToken);
//                        if (debug.messageEnabled()){
                            LOGGER.debug("IWA WDSSO: Authentication succeeded with new cred.");
//                            result = ISAuthConstants.LOGIN_SUCCEED;
//                        }
                    } catch (Exception ee) {
                        LOGGER.error("IWA WDSSO: Authentication failed with new cred. " + e.getMessage(), e);
                        throw ee;
//                        return AuthStatus.SEND_FAILURE;
                    }
                } else {
                    LOGGER.error("IWA WDSSO: Authentication failed with GSSException. " + e.getMessage(), e);
                    throw new RuntimeException();
//                    return AuthStatus.SEND_FAILURE;
                }
            }
        } catch (GSSException e ){
            int major = e.getMajor();
            if (major == GSSException.CREDENTIALS_EXPIRED) {
                LOGGER.debug("IWA WDSSO: Credential expired. Re-establish credential... " + e.getMessage());
                serviceLogin();
                try {
                    authenticateToken(kerberosToken);
//                    if (debug.messageEnabled()){
                        LOGGER.debug("IWA WDSSO: Authentication succeeded with new cred.");
//                        result = ISAuthConstants.LOGIN_SUCCEED;
//                    }
                } catch (Exception ee) {
                    LOGGER.debug("IWA WDSSO: Authentication failed with new cred. " + e.getMessage());
                    throw ee;
//                    return AuthStatus.SEND_FAILURE;
                }
            } else {
                LOGGER.debug("IWA WDSSO: Authentication failed with GSSException. " + e.getMessage());
                throw new RuntimeException();
//                return AuthStatus.SEND_FAILURE;
            }
        } catch (RuntimeException e) {
            LOGGER.error("IWA WDSSO: Authentication failed with generic exception. " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("IWA WDSSO: Authentication failed with generic exception. " + e.getMessage(), e);
            throw e;
//            return AuthStatus.SEND_FAILURE;
        }
//        return AuthStatus.SUCCESS;
            return user;


//        } catch (RuntimeException e) {
//            LOGGER.debug("IWA WDSSO: IWA failure! {}", e.getMessage());
//            throw e;
////            return AuthStatus.SEND_FAILURE;
//        }
    }

    private void authenticateToken(final byte[] kerberosToken)
            throws RuntimeException, GSSException, Exception {

//        debug.message("In authenticationToken ...");
        Subject.doAs(serviceSubject, new PrivilegedExceptionAction(){
            public Object run() throws Exception {
                GSSContext context = GSSManager.getInstance().createContext((GSSCredential)null);
                LOGGER.debug("IWA WDSSO: GSSContext created");
//                if (debug.messageEnabled()){
//                    debug.message("Context created.");
//                }
                byte[] outToken = context.acceptSecContext(
                        kerberosToken, 0,kerberosToken.length);
                if (outToken != null) {
                    LOGGER.debug("IWA WDSSO: Token returned from acceptSecContext: " +
                            DerValue.printByteArray(outToken, 0, outToken.length));
//                    if (debug.messageEnabled()) {
//                        debug.message(
//                                "Token returned from acceptSecContext: \n"
//                                        + DerValue.printByteArray(
//                                        outToken, 0, outToken.length));
//                    }
                }
                if (!context.isEstablished()) {
                    LOGGER.debug("IWA WDSSO: Cannot establish context!");
//                    debug.error("Cannot establish context !");
                    throw new RuntimeException();
                } else {
                    LOGGER.debug("IWA WDSSO: Context established");
//                    if (debug.messageEnabled()) {
//                        debug.message("Context established !");
//                    }
                    GSSName user = context.getSrcName();
                    WDSSO.user = getUserName(user.toString());
                    LOGGER.debug("IWA WDSSO: Found user! " + WDSSO.user);

                    // Check if the user account from the Kerberos ticket exists 
                    // in the realm. The "Alias Search Attribute Names" will be used to
                    // perform the search.
//                    if (lookupUserInRealm) {
//                        String org = getRequestOrg();
//                        String userValue = getUserName(user.toString());
//                        String userName = searchUserAccount(userValue, org);
//                        if (userName != null && !userName.isEmpty()) {
//                            storeUsernamePasswd(userValue, null);
//                        } else {
//                            String data[] = {userValue, org};
//                            debug.error("WindowsDesktopSSO.authenticateToken: "
//                                    + ": Unable to find the user " + userValue);
//                            throw new AuthLoginException(amAuthWindowsDesktopSSO,
//                                    "notfound", data);
//                        }
//                    }

//                    if (debug.messageEnabled()){
//                        debug.message("WindowsDesktopSSO.authenticateToken:"
//                                + "User authenticated: " + user.toString());
//                    }
                    if (user != null) {
//                        setPrincipal(user.toString());
                    }
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
            e = ((PrivilegedActionException)e).getException();
        }
        return e;
    }

    /**
     * TODO-JAVADOC
     */
    public void destroyModuleState() {
        userPrincipal = null;
    }

    /**
     * TODO-JAVADOC
     */
    public void nullifyUsedVars() {
        serviceSubject = null;
        servicePrincipalName = null;
        keyTabFile = null;
        kdcRealm = null;
        kdcServer = null;
        authLevel = null;
        options = null;
        confIndex = null;
    }

//    private void setPrincipal(String user) {
//        userPrincipal = new WindowsDesktopSSOPrincipal(getUserName(user));
//    }

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

    private static byte[] spnegoOID = {
            (byte)0x06, (byte)0x06, (byte)0x2b, (byte)0x06, (byte)0x01,
            (byte)0x05, (byte)0x05, (byte)0x02 };

    // defined but not used.
    private static byte[] MS_KERBEROS_OID =  {
            (byte)0x06, (byte)0x09, (byte)0x2a, (byte)0x86, (byte)0x48,
            (byte)0x82, (byte)0xf7, (byte)0x12, (byte)0x01, (byte)0x02,
            (byte)0x02 };
    private static byte[] KERBEROS_V5_OID = {
            (byte)0x06, (byte)0x09, (byte)0x2a, (byte)0x86, (byte)0x48,
            (byte)0x86, (byte)0xf7, (byte)0x12, (byte)0x01, (byte)0x02,
            (byte)0x02 };

    /**
     * Checks the request for an attribute "iwa-failed".
     *
     * @param request THe HttpServletRequest.
     * @return If the attribute is present and set to true true is returned otherwise false is returned.
     */
    private boolean hasWDSSOFailed(HttpServletRequest request) {
        return Boolean.valueOf((String) request.getAttribute("iwa-failed"));
    }

    private byte[] getSPNEGOTokenFromHTTPRequest(HttpServletRequest req) {
        byte[] spnegoToken = null;
        String header = req.getHeader("Authorization");
        if ((header != null) && header.startsWith("Negotiate")) {
            header = header.substring("Negotiate".length()).trim();
            LOGGER.debug("IWA WDSSO: \"Authorization\" header set, " + header);
            try {
                spnegoToken = Base64.decode(header);
            } catch (Exception e) {
                LOGGER.error("IWA WDSSO: Failed to get SPNEGO Token from request");
//                debug.error("Decoding token error.");
//                if (debug.messageEnabled()) {
//                    debug.message("Stack trace: ", e);
//                }
            }
        } else {
            LOGGER.error("IWA WDSSO: \"Authorization\" header not set in reqest");
        }
        return spnegoToken;
    }

//    private byte[] getSPNEGOTokenFromCallback(Callback[] callbacks) {
//        byte[] spnegoToken = null;
//        if (callbacks != null && callbacks.length != 0) {
//            String spnegoTokenStr =
//                    ((HttpCallback)callbacks[0]).getAuthorization();
//            try {
//                spnegoToken = Base64.decode(spnegoTokenStr);
//            } catch (Exception e) {
//                debug.error("Decoding token error.");
//                if (debug.messageEnabled()) {
//                    debug.message("Stack trace: ", e);
//                }
//            }
//        }
//
//        return spnegoToken;
//    }

    private byte[] parseToken(byte[] rawToken) {
        byte[] token = rawToken;
        DerValue tmpToken = new DerValue(rawToken);
//        if (debug.messageEnabled()) {
//            debug.message("token tag:" + DerValue.printByte(tmpToken.getTag()));
//        }
        if (tmpToken.getTag() != (byte)0x60) {
            return null;
        }

        ByteArrayInputStream tmpInput = new ByteArrayInputStream(
                tmpToken.getData());

        // check for SPNEGO OID
        byte[] oidArray = new byte[spnegoOID.length];
        tmpInput.read(oidArray, 0, oidArray.length);
        if (Arrays.equals(oidArray, spnegoOID)) {
//            if (debug.messageEnabled()) {
//                debug.message("SPNEGO OID found in the Auth Token");
//            }
            tmpToken = new DerValue(tmpInput);

            // 0xa0 indicates an init token(NegTokenInit); 0xa1 indicates an 
            // response arg token(NegTokenTarg). no arg token is needed for us.

            if (tmpToken.getTag() == (byte)0xa0) {
//                if (debug.messageEnabled()) {
//                    debug.message("DerValue: found init token");
//                }
                tmpToken = new DerValue(tmpToken.getData());
                if (tmpToken.getTag() == (byte)0x30) {
//                    if (debug.messageEnabled()) {
//                        debug.message("DerValue: 0x30 constructed token found");
//                    }
                    tmpInput = new ByteArrayInputStream(tmpToken.getData());
                    tmpToken = new DerValue(tmpInput);

                    // In an init token, it can contain 4 optional arguments:
                    // a0: mechTypes
                    // a1: contextFlags
                    // a2: octect string(with leading char 0x04) for the token
                    // a3: message integrity value

                    while (tmpToken.getTag() != (byte)-1 &&
                            tmpToken.getTag() != (byte)0xa2) {
                        // look for next mech token DER
                        tmpToken = new DerValue(tmpInput);
                    }
                    if (tmpToken.getTag() != (byte)-1) {
                        // retrieve octet string
                        tmpToken = new DerValue(tmpToken.getData());
                        token = tmpToken.getData();
                    }
                }
            }
        } else {
            LOGGER.debug("IWA WDSSO: SPENGO OID not found in the Auth Token");
//            if (debug.messageEnabled()) {
//                debug.message("SPNEGO OID not found in the Auth Token");
//            }
            byte[] krb5Oid = new byte[KERBEROS_V5_OID.length];
            int i = 0;
            for (; i < oidArray.length; i++) {
                krb5Oid[i] = oidArray[i];
            }
            tmpInput.read(krb5Oid, i, krb5Oid.length - i);
            if (!Arrays.equals(krb5Oid, KERBEROS_V5_OID)) {
                LOGGER.debug("IWA WDSSO: Kerberos V5 OID not found in the Auth Token");
//                if (debug.messageEnabled()) {
//                    debug.message("Kerberos V5 OID not found in the Auth Token");
//                }
                token = null;
            } else {
                LOGGER.debug("IWA WDSSO: Kerberos V5 OID found in the Auth Token");
//                if (debug.messageEnabled()) {
//                    debug.message("Kerberos V5 OID found in the Auth Token");
//                }
            }
        }
        return token;
    }

    private boolean getConfigParams(Map<String, String> options) {
        // KDC realm in service principal must be uppercase.
        servicePrincipalName = options.get("servicePrincipal");//getMapAttr(options, PRINCIPAL);
        keyTabFile = options.get("keytabFileName");//getMapAttr(options, KEYTAB);
        kdcRealm = options.get("kerberosRealm");//getMapAttr(options, REALM);
        kdcServer = options.get("kerberosServerName");//getMapAttr(options, KDC);
//        authLevel = options.get("authLevel");//getMapAttr(options, AUTHLEVEL);
//        returnRealm = Boolean.valueOf(options.get("RETURNREALM")/*getMapAttr(options,RETURNREALM)*/).booleanValue();
//        lookupUserInRealm = Boolean.valueOf(options.get("LOOKUPUSERINREALM")/*getMapAttr(options,LOOKUPUSER)*/).booleanValue();

//        if (debug.messageEnabled()){
            LOGGER.debug("IWA WDSSO: WindowsDesktopSSO params: principal: " + servicePrincipalName + ", keytab file: " +
                    keyTabFile + ", realm : " + kdcRealm + ", kdc server: " + kdcServer);
//        }

//        confIndex = getRequestOrg() + "/" +
//                options.get(ISAuthConstants.MODULE_INSTANCE_NAME);



        /*Map configMap = (Map)configTable.get(confIndex);             //TODO might need this
        if (configMap == null) {
            return false;
        }


        String principalName = (String)configMap.get(configAttributes[PRINCIPAL]);
        String tabFile = (String)configMap.get(configAttributes[KEYTAB]);
        String realm = (String)configMap.get(configAttributes[REALM]);
        String kdc = (String)configMap.get(configAttributes[KDC]);

        if (principalName == null || tabFile == null ||
                realm == null || kdc == null ||
                ! servicePrincipalName.equalsIgnoreCase(principalName) ||
                ! keyTabFile.equals(tabFile) ||
                ! kdcRealm.equals(realm) ||
                ! kdcServer.equalsIgnoreCase(kdc)) {
            return false;
        }

        serviceSubject = (Subject)configMap.get(configAttributes[SUBJECT]);
        if (serviceSubject == null) {
            return false;
        }
//        if (debug.messageEnabled()){
//            debug.message("Retrieved config params from cache.");
//        }
        return true;*/
        return serviceSubject != null;
    }

    private void initWindowsDesktopSSOAuth(Map options)
            throws Exception {

//        if (debug.messageEnabled()){
//            debug.message("Init WindowsDesktopSSO. This should not happen often.");
//        }
        verifyAttributes();
        serviceLogin();

        // save the service subject and the other configuration data
        // into configTable for other auth requests in the same org
//        Map configMap = (Map)configTable.get(confIndex);              //TODO might need this?...
//        if (configMap == null) {
//            configMap = new HashMap();
//        }
//
//        configMap.put(configAttributes[SUBJECT], serviceSubject);
//        configMap.put(configAttributes[PRINCIPAL], servicePrincipalName);
//        configMap.put(configAttributes[KEYTAB], keyTabFile);
//        configMap.put(configAttributes[REALM], kdcRealm);
//        configMap.put(configAttributes[KDC], kdcServer);
//
//        configTable.put(confIndex, configMap);
    }

    private synchronized void serviceLogin() throws Exception {
//        if (debug.messageEnabled()){
//            debug.message("New Service Login ...");
//        }
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
            LoginContext lc = new LoginContext(WindowsDesktopSSOConfig.defaultAppName);
            LOGGER.debug("IWA WDSSO: Attempting to run login() on the LoginContext");
            lc.login();
            LOGGER.debug("IWA WDSSO: LoginContext.login() successful");

            serviceSubject = lc.getSubject();
//            if (debug.messageEnabled()){
//                debug.message("Service login succeeded.");
//            }
        } catch (Exception e) {
            LOGGER.error("IWA WDSSO: Service Login Error: " + e.getMessage());
//            if (debug.messageEnabled()) {
                LOGGER.error("IWA WDSSO: Stack trace: ", e);
//            }
            throw e;
        }
    }

//    private String getMapAttr(Map options, int index) {
//        return CollectionHelper.getMapAttr(options, configAttributes[index]);
//    }

    private void verifyAttributes() throws RuntimeException {
        if (servicePrincipalName == null || servicePrincipalName.length() == 0) {
            LOGGER.error("IWA WDSSO: Service Principal Name not set");
            throw new RuntimeException();
        }
        if (keyTabFile == null || keyTabFile.length() == 0) {
            LOGGER.error("IWA WDSSO: Key Tab File not set");
            throw new RuntimeException();
        }
        if (kdcRealm == null || kdcRealm.length() == 0) {
            LOGGER.error("IWA WDSSO: Kerberos Realm not set");
            throw new RuntimeException();
        }
        if (kdcServer == null || kdcServer.length() == 0) {
            LOGGER.error("IWA WDSSO: Kerberos Server Name not set");
            throw new RuntimeException();
        }
//        if (authLevel == null || authLevel.length() == 0){
//            throw new RuntimeException();
//        }

        if (!(new File(keyTabFile)).exists()) {
            // ibm jdk needs to skip "file://" part in parameter
            if (!(new File(keyTabFile.substring(7))).exists()) {
                LOGGER.error("IWA WDSSO: Key Tab File does not exist");
                throw new RuntimeException();
            }
        }

//        try {
//            setAuthLevel(Integer.parseInt(authLevel));
//        } catch (Exception e) {
//            throw new AuthLoginException(amAuthWindowsDesktopSSO,
//                    "authlevel", null, e);
//        }
    }


    /**
     * Searches for an account with user Id userID in the organization organization
//     * @param searchAttribute The attribute to be used to search for an identity
     *  in the organization
     * @param attributeValue The attributeValue to compare when searchinf for an 
     *  identity in the organization
     * @param organization organization or the organization name where the identity will be
     *  looked up
     * @return the attribute value for the identity searched. Empty string if not found or
     *  null if an error occurs
     */
//    private String searchUserAccount(String attributeValue, String organization)
//            throws AuthLoginException {
//
//        String classMethod = "WindowsDesktopSSO.searchUserAccount: ";
//
//        if (organization.isEmpty()) {
//            organization = "/";
//        }
//
//        if (debug.messageEnabled()) {
//            debug.message(classMethod + " searching for user " + attributeValue
//                    + " in the organization =" + organization);
//        }
//
//        // And the search criteria
//        IdSearchControl searchControl = new IdSearchControl();
//        searchControl.setMaxResults(1);
//        searchControl.setTimeOut(3000);
//
//        searchControl.setSearchModifiers(IdSearchOpModifier.OR, buildSearchControl(attributeValue));
//        searchControl.setAllReturnAttributes(false);
//
//        try {
//            AMIdentityRepository amirepo = new AMIdentityRepository(getSSOSession(), organization);
//
//            IdSearchResults searchResults = amirepo.searchIdentities(IdType.USER, "*", searchControl);
//            if (searchResults.getErrorCode() == IdSearchResults.SUCCESS && searchResults != null) {
//                Set<AMIdentity> results = searchResults.getSearchResults();
//                if (!results.isEmpty()) {
//                    if (debug.messageEnabled()) {
//                        debug.message(classMethod + results.size() + " result(s) obtained");
//                    }
//                    AMIdentity userDNId = results.iterator().next();
//                    if (userDNId != null) {
//                        if (debug.messageEnabled()) {
//                            debug.message(classMethod + "user = " + userDNId.getUniversalId());
//                            debug.message(classMethod + "attrs =" + userDNId.getAttributes(
//                                    getUserAliasList()));
//                        }
//                        return attributeValue.trim();
//                    }
//                }
//            }
//        } catch (IdRepoException idrepoex) {
//            String data[] = {attributeValue, organization};
//            throw new AuthLoginException(amAuthWindowsDesktopSSO,
//                    "idRepoSearch", data, idrepoex);
//        } catch (SSOException ssoe) {
//            String data[] = {attributeValue, organization};
//            throw new AuthLoginException(amAuthWindowsDesktopSSO,
//                    "ssoSearch", data, ssoe);
//        }
//        if (debug.messageEnabled()) {
//            debug.message(classMethod + " No results were found !");
//        }
//        return null;
//    }

//    private Map<String, Set<String>> buildSearchControl(String value)
//            throws AuthLoginException {
//        Map<String, Set<String>> attr = new HashMap<String, Set<String>>();
//        Set<String> userAttrs = getUserAliasList();
//        for (String userAttr : userAttrs) {
//            attr.put(userAttr, addToSet(new HashSet<String>(), value));
//        }
//        return attr;
//    }

    /**
     * Provides the "Alias Search Attribute Name" list from the Authentication
     * Service for the realm. If these attributes are not configured it falls
     * back to the User Naming Attribute for the realm
     * @return a set containing the attribute names configured 
     */
//    private Set<String> getUserAliasList() throws AuthLoginException {
//        Map orgSvc = getOrgServiceTemplate(
//                getRequestOrg(), ISAuthConstants.AUTH_SERVICE_NAME);
//        Set aliasAttrNames = (Set<String>) orgSvc.get(ISAuthConstants.AUTH_ALIAS_ATTR);
//        if (debug.messageEnabled()) {
//            debug.message("WindowsDesktopSSO.getUserAliasList: aliasAttrNames=" + aliasAttrNames);
//        }
//        if (aliasAttrNames.isEmpty()) {
//            aliasAttrNames = (Set<String>)orgSvc.get(ISAuthConstants.AUTH_NAMING_ATTR);
//            if (debug.messageEnabled()) {
//                debug.message("WindowsDesktopSSO.getUserAliasList trying AUTH_NAMING_ATTR:" +
//                        aliasAttrNames);
//            }
//        }
//        return aliasAttrNames;
//    }

    private static Set<String> addToSet(Set<String> set, String attribute) {
        set.add(attribute);
        return set;
    }
}

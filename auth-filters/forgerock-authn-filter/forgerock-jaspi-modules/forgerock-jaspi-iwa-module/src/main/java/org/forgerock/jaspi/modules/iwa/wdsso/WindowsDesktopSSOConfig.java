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
 * $Id: WindowsDesktopSSOConfig.java,v 1.3 2009/04/07 22:55:13 beomsuk Exp $
 *
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 */


package org.forgerock.jaspi.modules.iwa.wdsso;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for WDSSO.
 */
public class WindowsDesktopSSOConfig extends Configuration {

    static final String DEFAULT_KRB5_LOGINMODULE =
            "com.sun.security.auth.module.Krb5LoginModule";

    /** Default app name. */
    public static final String DEFAULT_APP_NAME = "com.sun.identity.authentication.windowsdesktopsso";
    private static final String KERBEROS_MODULE_NAME = DEFAULT_KRB5_LOGINMODULE;
    private static final String CREDS_TYPE = "acceptor";

    private Configuration config = null;
    private String servicePrincipal = null;
    private String keytab = null;
    private String refreshConf = "false";

    /**
     * Constructor.
     *
     * @param config The configuration.
     */
    public WindowsDesktopSSOConfig(Configuration config) {
        this.config = config;
    }

    /**
     * Sets principal name.
     *
     * @param principalName The name.
     */
    public void setPrincipalName(String principalName) {
        servicePrincipal = principalName;
    }

    /**
     * Sets key tab file.
     *
     * @param keytabFile The file.
     */
    public void setKeyTab(String keytabFile) {
        keytab = keytabFile;
    }

    /**
     * The the refresh config.
     * @param refresh The refresh.
     */
    public void setRefreshConfig(String refresh) {
        refreshConf = refresh;
    }

    /**
     * Returns AppConfigurationEntry array for the application <I>appName</I>
     * using Kerberos module.
     *
     * @param appName The app name.
     * @return Array of AppConfigurationEntry
     */
    public AppConfigurationEntry[] getAppConfigurationEntry(String appName) {
        if (appName.equals(DEFAULT_APP_NAME)) {
            Map<String, String> hashmap = new HashMap<>();
            hashmap.put("principal", servicePrincipal);
            if (KERBEROS_MODULE_NAME.equalsIgnoreCase("com.ibm.security.auth.module.Krb5LoginModule")) {
                hashmap.put("useKeytab", keytab);
                hashmap.put("credsType", CREDS_TYPE);
                hashmap.put("refreshKrb5Config", "false");
            } else {
                hashmap.put("storeKey", "true");
                hashmap.put("useKeyTab", "true");
                hashmap.put("keyTab", keytab);
                hashmap.put("doNotPrompt", "true");
                hashmap.put("refreshKrb5Config", refreshConf);
            }

            AppConfigurationEntry appConfigurationEntry =
                new AppConfigurationEntry(
                        KERBEROS_MODULE_NAME,
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    hashmap);
            return new AppConfigurationEntry[]{ appConfigurationEntry };
        }
        return config.getAppConfigurationEntry(appName);
    }

    /**
     * Do a config refresh.
     */
    public void refresh() {
        config.refresh();
    }
}


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
* Copyright 2015-2016 ForgeRock AS.
*/
package org.forgerock.audit.handlers.csv;

/**
 *
 * Holds the constants shared between the CsvSecure classes.
 */
final class CsvSecureConstants {

    // The key stored with the label "InitialKey" is the first key (salt) used to generate the HEADER_HMAC for the first
    // row. Then on this key, we apply a rotation that gives another key, that will be used to generated the HEADER_HMAC
    // for the second row, ... and so on.
    static final String ENTRY_INITIAL_KEY = "InitialKey";
    // The last signature inserted into the CSV file
    static final String ENTRY_CURRENT_SIGNATURE = "CurrentSignature";
    // This the current key used to calculate the HEADER_HMAC
    static final String ENTRY_CURRENT_KEY = "CurrentKey";
    // The alias to lookup the private key into the keystore
    static final String ENTRY_SIGNATURE = "Signature";
    // The alias to lookup the password into the keystore
    static final String ENTRY_PASSWORD = "Password";

    static final String HEADER_HMAC = "HMAC";
    static final String HEADER_SIGNATURE = "SIGNATURE";

    static final String KEYSTORE_TYPE = "JCEKS";

    static final String HMAC_ALGORITHM = "HmacSHA256";
    static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private CsvSecureConstants() {
        // Prevent from instantiating
    }

}

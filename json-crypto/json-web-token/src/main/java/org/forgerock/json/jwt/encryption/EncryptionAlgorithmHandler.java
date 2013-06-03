///*
// * The contents of this file are subject to the terms of the Common Development and
// * Distribution License (the License). You may not use this file except in compliance with the
// * License.
// *
// * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
// * specific language governing permission and limitations under the License.
// *
// * When distributing Covered Software, include this CDDL Header Notice in each file and include
// * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
// * Header, with the fields enclosed by brackets [] replaced by your own identifying
// * information: "Portions copyright [year] [name of copyright owner]".
// *
// * Copyright 2013 ForgeRock Inc.
// */
//
//package org.forgerock.json.jwt.encryption;
//
//import java.nio.charset.Charset;
//import java.security.Provider;
//import java.security.Security;
//
//public class EncryptionAlgorithmHandler {
//
//    public Object getContentEncryptionKey(String algorithm) {
//
//        if (/* Key Wrapping, Key Encryption or Key Agreement with Key Wrapping */) {
//
//            // Generate a random Content Encryption Key value. See RFC 4086 for considerations on generating randmom values
//
//        } else if (/* Direct Key Agreement or Key Agreement with Key Wrapping */) {
//
//            // Use the key agreement algorithm to compute the value of the agreed upon key
//
//            if (/* Direct Key Agreement */) {
//
//                // CEK is the agreed upon key
//                //?? CEK is the shared symmetric key
//
//            } else {
//                /* Key Agreement with Key Wrapping */
//
//                // ?????
//
//            }
//
//        } else {
//            //TODO error?
//        }
//    }
//
//    public byte[] getEncryptedKey(String algorithm, Object contentEncryptionKey) {
//
//        if (/* Key Wrapping, Key Encryption or Key Agreement with Key Wrapping */) {
//
//            // Encrypt the CEK    with....?
//
//        } else if (/* Direct Key Agreement or Key Agreement with Key Wrapping */) {
//
//            // Empty octet
//            return "".getBytes(Charset.forName("UTF-8"));
//
//        } else {
//            //TODO error?
//        }
//    }
//}

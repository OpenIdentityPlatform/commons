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
* Copyright 2015 ForgeRock AS.
*/

package org.forgerock.audit.events.handlers.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import javax.crypto.KeyGenerator;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.forgerock.util.encode.Base64;

/**
 * This class aims to compute the HMAC for the given data.
 *
 */
class HmacCalculator {
    
    // The key stored with the label "InitialKey" is the first key (salt) used to generate the HMAC for the first row. 
    // Then on this key, we apply a rotation that gives another key, that will be used to generated the HMAC 
    // for the second row, ... and so on.
    private static final String ENTRY_INITIAL_KEY = "InitialKey";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    private SecretKey currentKey;
    private final String keystoreFilename;
    private final String password;
    private MessageDigest messageDigest;
    private KeyGenerator keyGenerator;
    private boolean initialized = false;
    
    public HmacCalculator(String keystoreFilename, String password) {
        this.password = password;
        this.keystoreFilename = keystoreFilename;
    }
    
    public void init() {
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            keyGenerator = KeyGenerator.getInstance(HMAC_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        
        File keystoreFile = new File(keystoreFilename);
        try {
            this.currentKey = readFromKeyStore(keystoreFilename, ENTRY_INITIAL_KEY, password);
            if (this.currentKey == null) {
                // Generate one and store it
                this.currentKey = generateRandomKey();
                writeToKeyStore(currentKey, keystoreFilename, ENTRY_INITIAL_KEY, password);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        initialized = true;
    }
    
    /**
     * Compute the HMAC and returns it as a base64 encoded String.
     *
     * @param data the data used to calculate the HMAC
     * @return the calculated HMAC as a base64 encoded String.
     * @throws SignatureException
     */
    public String calculate(byte[] data) throws SignatureException {
        checkInitialized();
        try {
            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(currentKey);
            
            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data);
            
            // base64-encode the hmac
            String result = Base64.encode(rawHmac);
            
            // Compute the next key's iteration
            computeNextKeyIteration();
            
            return result;
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalStateException e) {
            throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
        }
    }
    
    
    private void computeNextKeyIteration() {
        // k1 = digest(k0)
        messageDigest.update(currentKey.getEncoded());
        currentKey = new SecretKeySpec(messageDigest.digest(), HMAC_ALGORITHM);
    }
    
    /**
     * Writes to the secret Storage. If the data to be written is a key, then writes the older signature also. If it is
     * a signature then writes the older key also
     *
     * @param cryptoMaterial The data to be written to the secret storage
     * @param filename The file for secret storage
     * @param password The password for the file
     * @param dataType The kind of cryptoMaterial, whether it is a signature or a key
     * @throws Exception if it fails to write secret data from secret store
     */
    static void writeToKeyStore(SecretKey cryptoMaterial,
            String filename,
            String dataType,
            String password) throws Exception {
        KeyStore store = KeyStore.getInstance("jceks");
        File file = new File(filename);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                store.load(fis, password.toCharArray());
            }
        } else {
            store.load(null, new char[0]);
        }
        
        if (store.containsAlias(dataType)) {
            store.deleteEntry(dataType);
        }
        
        KeyStore.SecretKeyEntry secKeyEntry = new KeyStore.SecretKeyEntry(cryptoMaterial);
        KeyStore.ProtectionParameter params = new KeyStore.PasswordProtection(password.toCharArray());
        store.setEntry(dataType, secKeyEntry, params);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            store.store(fos, password.toCharArray());
        }
    }
    
    /**
     * Returns matched secret data from from the secret Storage. At a time there are only 3 things in logger's secure
     * store file - initialkey, currentkey and current signature In the verifier secure store file there is just the
     * initial key of the logger and the currentKey
     *
     * @param filename file for secret storage
     * @param dataType The kind of data to be read, whether it is a signature or a key
     * @param password password for the file
     * @return secure data that is matched with dataType
     * @throws Exception if it fails to read secret data from secret store
     */
    private SecretKey readFromKeyStore(String filename,
            String dataType,
            String password) throws Exception {
        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }
        
        KeyStore store;
        try (FileInputStream fis = new FileInputStream(file)) {
            store = KeyStore.getInstance("jceks");
            store.load(fis, password.toCharArray());
        }
        
        KeyStore.ProtectionParameter params = new KeyStore.PasswordProtection(password.toCharArray());
        KeyStore.SecretKeyEntry keyentry = (KeyStore.SecretKeyEntry) store.getEntry(dataType, params);
        if (keyentry != null) {
            return keyentry.getSecretKey();
        } else {
            return null;
        }
    }
    
    SecretKey generateRandomKey() throws NoSuchAlgorithmException {
        // Generate an initial  key
        return keyGenerator.generateKey();
    }
    
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
    }
    
}

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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.jaspi.modules.session.openam;

/**
 * Class to set configuration properties to enable a SSL connection.
 *
 * @since 1.4.0
 */
class SslConfiguration {

    private String keyManagerAlgorithm;
    private String keyStorePath;
    private String keyStoreType;
    private char[] keyStorePassword;
    private char[] keyStoreKeyPassword;
    private String trustManagerAlgorithm;
    private String trustStorePath;
    private String trustStoreType;
    private char[] trustStorePassword;

    /**
     * Gets the Key Manager algorithm.
     *
     * @return The Key Manager algorithm.
     */
    public String getKeyManagerAlgorithm() {
        return keyManagerAlgorithm;
    }

    /**
     * Sets the Key Manager algorithm.
     *
     * @param keyManagerAlgorithm The Key Manager algorithm.
     */
    public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
        this.keyManagerAlgorithm = keyManagerAlgorithm;
    }

    /**
     * Gets the path to the Key Store.
     *
     * @return The Key Store path.
     */
    public String getKeyStorePath() {
        return keyStorePath;
    }

    /**
     * Sets the path to the Key Store.
     *
     * @param keyStorePath The Key Store path.
     */
    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    /**
     * Gets the Key Store type.
     *
     * @return The Key Store type.
     */
    public String getKeyStoreType() {
        return keyStoreType;
    }

    /**
     * Sets the Key Store type.
     *
     * @param keyStoreType The Key Store type.
     */
    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    /**
     * Gets the Key Store password.
     *
     * @return The Key Store password.
     */
    public char[] getKeyStorePassword() {
        return keyStorePassword;
    }

    /**
     * Sets the Key Store password.
     *
     * @param keyStorePassword The Key Store password.
     */
    public void setKeyStorePassword(char[] keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    /**
     * Gets the Key Store key password.
     *
     * @return The Key Store key password.
     */
    public char[] getKeyStoreKeyPassword() {
        return keyStoreKeyPassword;
    }

    /**
     * Sets the Key Store key password.
     *
     * @param keyStoreKeyPassword The Key Store key password.
     */
    public void setKeyStoreKeyPassword(char[] keyStoreKeyPassword) {
        this.keyStoreKeyPassword = keyStoreKeyPassword;
    }

    /**
     * Gets the Trust Manager algorithm.
     *
     * @return The Trust Manager algorithm.
     */
    public String getTrustManagerAlgorithm() {
        return trustManagerAlgorithm;
    }

    /**
     * Sets the Trust Manager algorithm.
     *
     * @param trustManagerAlgorithm The Trust Manager algorithm.
     */
    public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
        this.trustManagerAlgorithm = trustManagerAlgorithm;
    }

    /**
     * Gets the path to the Trust Store.
     *
     * @return The Trust Store path.
     */
    public String getTrustStorePath() {
        return trustStorePath;
    }

    /**
     * Sets the path to the Trust Store.
     *
     * @param trustStorePath The Trust Store path.
     */
    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    /**
     * Gets the Trust Store type.
     *
     * @return The Trust Store type.
     */
    public String getTrustStoreType() {
        return trustStoreType;
    }

    /**
     * Sets the Trust Store type.
     *
     * @param trustStoreType The Trust Store type.
     */
    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }

    /**
     * Gets the Trust Store password.
     *
     * @return The Trust Store password.
     */
    public char[] getTrustStorePassword() {
        return trustStorePassword;
    }

    /**
     * Sets the Trust Store password.
     *
     * @param trustStorePassword The Trust Store password.
     */
    public void setTrustStorePassword(char[] trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }
}

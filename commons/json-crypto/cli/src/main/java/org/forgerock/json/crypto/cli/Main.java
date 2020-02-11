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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2011-2016 ForgeRock AS.
 */

package org.forgerock.json.crypto.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.forgerock.json.JsonPointer;
//import org.forgerock.json.JsonTransformer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.crypto.JsonCrypto;
import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.json.crypto.JsonCryptoTransformer;
import org.forgerock.json.crypto.simple.SimpleDecryptor;
import org.forgerock.json.crypto.simple.SimpleEncryptor;
import org.forgerock.json.crypto.simple.SimpleKeyStoreSelector;
import org.forgerock.security.keystore.KeyStoreBuilder;
import org.forgerock.security.keystore.KeyStoreType;
import org.forgerock.util.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Command-line interface to encrypt/decrypt.
 */
public class Main {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Options OPTIONS; // Command line options

    private static final String PROPERTIES_ALIAS_OPTION = "alias";
    private static final String PROPERTIES_CIPHER_OPTION = "cipher";
    private static final String DEFAULT_CIPHER = "AES/CBC/PKCS5Padding";
    private static final String PROPERTIES_SRCJSON_OPTION = "srcjson";
    private static final String PROPERTIES_DESTJSON_OPTION = "destjson";
    private static final String PROPERTIES_KEYPASS_OPTION = "keypass";
    private static final String PROPERTIES_KEYSTORE_OPTION = "keystore";
    private static final String PROPERTIES_STOREPASS_OPTION = "storepass";
    private static final String PROPERTIES_STORETYPE_OPTION = "storetype";
    private static final String PROPERTIES_PROVIDERNAME_OPTION = "providername";
    private static final String PROPERTIES_PROVIDERCLASS_OPTION = "providerclass";
    private static final String PROPERTIES_PROVIDERARG_OPTION = "providerarg";
    private static final String PROPERTIES_PROVIDERPATH_OPTION = "providerpath";
    private static final String PROPERTIES_ENCRYPT_COMMAND = "encrypt";
    private static final String PROPERTIES_DECRYPT_COMMAND = "decrypt";
    private static final String PROPERTIES_HELP_COMMAND = "help";

    private CommandLine cmd = null; // Command Line arguments

    static {
        OPTIONS = new Options();
        OPTIONS.addOption(PROPERTIES_ENCRYPT_COMMAND, false,
                "Encrypt input file");
        OPTIONS.addOption(PROPERTIES_DECRYPT_COMMAND, false,
                "Decrypt input file");
        OPTIONS.addOption("h", PROPERTIES_HELP_COMMAND, false,
                "Display help");


        //Required encryption options
        OPTIONS.addOption(PROPERTIES_ALIAS_OPTION, true,
                "Cryptography key alias.");
        OPTIONS.addOption(PROPERTIES_CIPHER_OPTION, true,
                "Cipher algorithm. " + DEFAULT_CIPHER + " by default");
        //Required input options
        OPTIONS.addOption(PROPERTIES_SRCJSON_OPTION, true,
                "Input JSON File");
        //Optional output options
        OPTIONS.addOption(PROPERTIES_DESTJSON_OPTION, true,
                "Output JSON File");
        //Required keystore options
        OPTIONS.addOption(PROPERTIES_KEYSTORE_OPTION, true,
                "KeyStore File");
        OPTIONS.addOption(PROPERTIES_STOREPASS_OPTION, true,
                "KeyStore password.");
        OPTIONS.addOption(PROPERTIES_STORETYPE_OPTION, true,
                "KeyStore type. Default: " + KeyStore.getDefaultType());
        OPTIONS.addOption(PROPERTIES_KEYPASS_OPTION, true,
                "Key password");
        OPTIONS.addOption(PROPERTIES_PROVIDERNAME_OPTION, true,
                "KeyStore provider");
        OPTIONS.addOption(PROPERTIES_PROVIDERCLASS_OPTION, true,
                "KeyStore provider class");
        OPTIONS.addOption(PROPERTIES_PROVIDERARG_OPTION, true,
                "KeyStore provider options");
        OPTIONS.addOption(PROPERTIES_PROVIDERPATH_OPTION, true,
                "KeyStore provider path");
    }

    /**
     * Entry point.
     * @param args CLI Args.
     * @throws Exception On error.
     */
    public static void main(String[] args) throws Exception {
        Main cliProg = new Main();
        cliProg.loadArgs(args);
        cliProg.exec();
    }

    /**
     * Execute the CLI on the class instance.
     * @throws Exception On error.
     */
    public void exec() throws Exception {
        if (cmd.hasOption(PROPERTIES_ENCRYPT_COMMAND)) {
            Key key = getSimpleKeySelector(cmd.getOptionValue(PROPERTIES_KEYSTORE_OPTION),
                    cmd.getOptionValue(PROPERTIES_STORETYPE_OPTION, KeyStore.getDefaultType()),
                    cmd.getOptionValue(PROPERTIES_STOREPASS_OPTION),
                    cmd.getOptionValue(PROPERTIES_PROVIDERNAME_OPTION))
                            .select(cmd.getOptionValue(PROPERTIES_ALIAS_OPTION));
            if (key == null) {
                throw new JsonCryptoException("key not found: " + cmd.getOptionValue(PROPERTIES_ALIAS_OPTION));
            }
            SimpleEncryptor encryptor = new SimpleEncryptor(
                    cmd.getOptionValue(PROPERTIES_CIPHER_OPTION, DEFAULT_CIPHER), key,
                    cmd.getOptionValue(PROPERTIES_ALIAS_OPTION));
            JsonValue value = getSourceValue(cmd.getOptionValue(PROPERTIES_SRCJSON_OPTION), true);
            value = new JsonCrypto(encryptor.getType(), encryptor.encrypt(value)).toJsonValue();
            setDestinationValue(cmd.getOptionValue(PROPERTIES_DESTJSON_OPTION), value);
        } else if (cmd.hasOption(PROPERTIES_DECRYPT_COMMAND)) {
            final ArrayList<JsonCryptoTransformer> decryptionTransformers = new ArrayList<>(1);
            decryptionTransformers.add(new JsonCryptoTransformer(new SimpleDecryptor(
                    getSimpleKeySelector(cmd.getOptionValue(PROPERTIES_KEYSTORE_OPTION),
                    cmd.getOptionValue(PROPERTIES_STORETYPE_OPTION, KeyStore.getDefaultType()),
                            cmd.getOptionValue(PROPERTIES_STOREPASS_OPTION),
                            cmd.getOptionValue(PROPERTIES_PROVIDERNAME_OPTION)))));
            JsonValue value = getSourceValue(cmd.getOptionValue(PROPERTIES_SRCJSON_OPTION), true);
            decryptionTransformers.get(0).transform(value);
            setDestinationValue(cmd.getOptionValue(PROPERTIES_DESTJSON_OPTION), value);
        } else {
            usage();
        }
    }

    private SimpleKeyStoreSelector getSimpleKeySelector(String keystore, String type, String password, String provider)
            throws Exception {
        final KeyStore ks = new KeyStoreBuilder()
                .withKeyStoreFile(keystore)
                .withPassword(password)
                .withProvider(provider)
                .withKeyStoreType(Utils.asEnum(type, KeyStoreType.class))
                .build();
        return new SimpleKeyStoreSelector(ks, password);
    }

    private JsonValue getSourceValue(String source, boolean file) throws IOException {
        JsonValue src = null;
        if (file) {
            File srcFile = new File(source);
            if (srcFile.exists()) {
                src = new JsonValue(MAPPER.readValue(srcFile, Object.class));
            } else {
                throw new FileNotFoundException("JsonSource file not found at: " + srcFile.getAbsolutePath());
            }
        } else {
            src = new JsonValue(MAPPER.readValue(source, Object.class));
        }
        return src;
    }

    private void setDestinationValue(String destination, JsonValue value) throws IOException {
        if (null == destination) {
            MAPPER.writeValue(System.out, value.getObject());
        } else {
            File dest = new File(destination);
            dest.getParentFile().mkdirs();
            MAPPER.writeValue(dest, value.getObject());
        }
    }

    /**
     * Validate and set command line arguments. Exit after printing usage if anything is
     * astray.
     *
     * @param args String[] args as featured in public static void main()
     */
    private void loadArgs(String[] args) {
        CommandLineParser parser = new PosixParser();
        try {
            cmd = parser.parse(OPTIONS, args);
        } catch (ParseException e) {
            System.err.println("Error parsing arguments");
            e.printStackTrace();
            System.exit(1);
        }

        if (cmd.hasOption('h')) {
            usage();
            System.exit(0);
        }

        // Check for mandatory args
        if (cmd.hasOption(PROPERTIES_HELP_COMMAND)) {
            usage();
            System.exit(0);
        }
    }

    private static void usage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar json-crypto-1.0.0-command-line.jar", OPTIONS);
    }
}

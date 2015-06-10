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
 * Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.json.schema;

import static org.kohsuke.args4j.ExampleMode.ALL;
import static org.kohsuke.args4j.ExampleMode.REQUIRED;

import java.io.Console;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.schema.validator.Constants;
import org.forgerock.json.schema.validator.ErrorHandler;
import org.forgerock.json.schema.validator.FailFastErrorHandler;
import org.forgerock.json.schema.validator.ObjectValidatorFactory;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.json.schema.validator.exceptions.ValidationException;
import org.forgerock.json.schema.validator.validators.Validator;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class Main {


    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String ROOT_SCHEMA_ID = "http://www.forgerock.org/schema/";

    private final Map<URI, Validator> schemaCache = new HashMap<>();

    @Option(name = "-v", aliases = {"--verbose"}, usage = "display all validation error not just the first")
    private boolean verbose;

    @Option(name = "-s", aliases = {"--schemas"}, required = true, usage = "file or folder contains the schema(s)", metaVar = "./schema")
    private File schemaFile = new File("./schema");

    @Option(name = "-b", aliases = {"--base"}, usage = "base value to resolve relative schema IDs. Default: " + ROOT_SCHEMA_ID, metaVar = ROOT_SCHEMA_ID)
    private String schemeBase = ROOT_SCHEMA_ID;

    @Option(name = "-i", aliases = {"--id"}, usage = "id of the schema. Optional if the object has \"$schema\" property")
    private String schemaURI;

    @Option(name = "-f", aliases = {"--file"}, usage = "input from this file", metaVar = "sample.json")
    private File inputFile;

    // receives other command line parameters than options
    @Argument
    private List<String> arguments = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new Main().doMain(args);
    }

    public void doMain(String[] args) throws Exception {
        CmdLineParser parser = new CmdLineParser(this);

        // if you have a wider console, you could increase the value;
        // here 80 is also the default
        parser.setUsageWidth(80);

        try {
            // parse the arguments.
            parser.parseArgument(args);

            // you can parse additional arguments if you want.
            // parser.parseArgument("more","args");

            // after parsing arguments, you should check
            // if enough arguments are given.
            /*if (arguments.isEmpty())
                throw new CmdLineException(parser, "No argument is given");*/

        } catch (CmdLineException e) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println("java Main [options...] arguments...");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            // print option sample. This is useful some time
            System.err.println("  Example: java Main" + parser.printExample(REQUIRED));
            System.err.println("  Example: java Main" + parser.printExample(ALL));

            return;
        }

        // set the base for all relative schema
        URI base = new URI(schemeBase);
        if (!base.isAbsolute()) {
            throw new IllegalArgumentException("-b (-base) must be an absolute URI");
        }

        // load all schema
        init(base);

        if (null == inputFile) {
            for (; ; ) {
                try {
                    validate(loadFromConsole());
                } catch (Exception e) {
                    printOutException(e);
                }
            }
        } else {
            try {
                validate(loadFromFile());
            } catch (Exception e) {
                printOutException(e);
            }
        }
    }

    //Initialization

    private void init(URI base) throws IOException {
        System.out.append("Loading schemas from: ").append(schemaFile.getAbsolutePath()).append(" with base ").append(base.toString()).println(" URI");
        if (schemaFile.isDirectory()) {
            validateDirectory(schemaFile);
            FileFilter filter = new FileFilter() {

                public boolean accept(File f) {
                    return (f.isDirectory()) || (f.getName().endsWith(".json"));
                }
            };

            for (File f : getFileListingNoSort(schemaFile, filter)) {
                //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6226081
                //org.apache.http.client.utils.URIUtils.resolve(URI,URI)
                URI relative = schemaFile.toURI().relativize(f.toURI());
                loadSchema(base.resolve(relative), f);
            }
        } else if (schemaFile.isFile()) {
            loadSchema(base, schemaFile);
        } else {
            System.exit(1);
        }
    }

    private void loadSchema(URI base, File schemaFile) throws IOException {
        JsonValue schemaMap = new JsonValue(mapper.readValue(new FileInputStream(schemaFile), Map.class));
        URI id = schemaMap.get(Constants.ID).required().asURI();
        Validator v = ObjectValidatorFactory.getTypeValidator(schemaMap.asMap());
        if (!id.isAbsolute()) {
            id = base.resolve(id);
        }
        schemaCache.put(id, v);
        System.out.append("Schema ").append(id.toString()).println(" loaded from file:");
        System.out.append("     location: ").println(schemaFile.getAbsolutePath());
    }

    /**
     * Recursively walk a directory tree and return a List of all
     * Files found; the List is sorted using File.compareTo().
     *
     * @param aStartingDir is a valid directory, which can be read.
     * @param filter
     * @return
     * @throws java.io.FileNotFoundException
     */
    private List<File> getFileListingNoSort(File aStartingDir, FileFilter filter) throws FileNotFoundException {
        List<File> result = new ArrayList<>();
        List<File> filesDirs = Arrays.asList(aStartingDir.listFiles(filter));
        for (File file : filesDirs) {
            if (!file.isFile()) {
                //must be a directory
                //recursive call!
                List<File> deeperList = getFileListingNoSort(file, filter);
                result.addAll(deeperList);
            } else {
                result.add(file);
            }
        }
        return result;
    }

    /**
     * Directory is valid if it exists, does not represent a file, and can be read.
     *
     * @param aDirectory
     * @throws java.io.FileNotFoundException
     */
    private void validateDirectory(File aDirectory) throws FileNotFoundException {
        if (aDirectory == null) {
            throw new IllegalArgumentException("Directory should not be null.");
        }
        if (!aDirectory.exists()) {
            throw new FileNotFoundException("Directory does not exist: " + aDirectory);
        }
        if (!aDirectory.isDirectory()) {
            throw new IllegalArgumentException("Is not a directory: " + aDirectory);
        }
        if (!aDirectory.canRead()) {
            throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
        }
    }

    //Validation

    private void validate(JsonValue value) throws SchemaException, URISyntaxException {
        URI schemaId = value.get(Constants.SCHEMA).asURI();
        if (null == schemaId && isEmptyOrBlank(schemaURI)) {
            System.out.println("-i (--id) must be an URI");
            return;
        } else if (null == schemaId) {
            schemaId = new URI(schemaURI);
        }

        Validator validator = schemaCache.get(schemaId);
        if (null != validator) {
            if (verbose) {
                final boolean[] valid = new boolean[1];
                validator.validate(value.getObject(), null, new ErrorHandler() {
                    @Override
                    public void error(ValidationException exception) throws SchemaException {
                        valid[0] = false;
                        printOutException(exception);
                    }

                    @Override
                    @Deprecated
                    public void assembleException() throws ValidationException {
                    }
                });
                if (valid.length == 0) {
                    System.out.println("OK - Object is valid!");
                }
            } else {
                validator.validate(value.getObject(), null, new FailFastErrorHandler());
                System.out.println("OK - Object is valid!");
            }
        } else {
            System.out.append("Schema ").append(schemaId.toString()).println(" not found!");
        }
    }

    private JsonValue loadFromConsole() throws IOException {
        System.out.println();
        System.out.println("> Enter 'exit' and press enter to exit");
        System.out.println("> Press ctrl-D to finish input");
        System.out.println("Start data input:");
        String input = null;
        StringBuilder stringBuilder = new StringBuilder();
        Console c = System.console();
        if (c == null) {
            System.err.println("No console.");
            System.exit(1);
        }

        Scanner scanner = new Scanner(c.reader());

        while (scanner.hasNext()) {
            input = scanner.next();
            if (null == input) {
                //control-D pressed
                break;
            } else if ("exit".equalsIgnoreCase(input)) {
                System.exit(0);
            } else {
                stringBuilder.append(input);
            }
        }
        return new JsonValue(mapper.readValue(stringBuilder.toString(), Object.class));
    }

    private JsonValue loadFromFile() throws IOException {
        return new JsonValue(mapper.readValue(inputFile, Object.class));
    }


    private static boolean isEmptyOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private void printOutException(Exception ex) {
        String top = "> > > > > >                                                         < < < < < <";
        String exName = ex.getClass().getSimpleName();
        StringBuilder sb = new StringBuilder(top.substring(0, 40 - (exName.length() / 2))).append(exName);
        sb.append(top.substring(sb.length()));

        System.out.println(sb);
        if ((ex instanceof SchemaException) && (null != ((SchemaException) ex).getJsonValue()))
            System.out.append("Path: ").println(((SchemaException) ex).getJsonValue().getPointer().toString());
        System.out.append("Message: ").println(ex.getMessage());
        System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");

    }
}

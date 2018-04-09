/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.script.scope;

import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.Factory;
import org.forgerock.util.LazyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class FunctionFactory {

    private FunctionFactory() {
    }

    public static String getNoSuchMethodMessage(String method, Object[] arguments) {
        StringBuilder sb = new StringBuilder("Method not found ").append(method).append('(');
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            if (arguments[i] == null) {
                sb.append("null");
            } else {
                sb.append(arguments[i].getClass().getSimpleName());
            }
        }
        return sb.append(')').toString();
    }

    public static Map<String, Function> getLogger(final String loggerName) {
        return new LazyMap<String, Function>(new Factory<Map<String, Function>>() {
            @Override
            public Map<String, Function> newInstance() {
                final Logger logger = LoggerFactory.getLogger(loggerName
                /*
                 * "org.forgerock.openidm.script.javascript.JavaScript." + (file
                 * == null ? "embedded-source" : file.getName())
                 */);
                HashMap<String, Function> loggerWrap = new HashMap<String, Function>();
                // error(string id, object... param)
                // Wraps SLF4j error(String format, Object[] argArray)
                // Log a message at the error level according to the specified
                // format and arguments.
                loggerWrap.put("error", new Function<Void>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Void call(Parameter scope, Function<?> callback, Object... arguments)
                            throws ResourceException, NoSuchMethodException {
                        if (arguments.length > 1 || arguments[0] instanceof String) {
                            logger.error((String) arguments[0], Arrays.copyOfRange(arguments, 1,
                                    arguments.length));
                        } else {
                            throw new NoSuchMethodException(getNoSuchMethodMessage("error",
                                    arguments));
                        }
                        return null;
                    }
                });
                // warn(string id, object... param)
                // Wraps SLF4j warn(String format, Object[] argArray)
                // Log a message at the warn level according to the specified
                // format and arguments.
                loggerWrap.put("warn", new Function<Void>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Void call(Parameter scope, Function<?> callback, Object... arguments)
                            throws ResourceException, NoSuchMethodException {
                        if (arguments.length > 1 || arguments[0] instanceof String) {
                            logger.warn((String) arguments[0], Arrays.copyOfRange(arguments, 1,
                                    arguments.length));
                        } else {
                            throw new NoSuchMethodException(getNoSuchMethodMessage("warn",
                                    arguments));
                        }
                        return null;
                    }
                });
                // info(string id, object... param)
                // Wraps SLF4j info(String format, Object[] argArray)
                // Log a message at the info level according to the specified
                // format and arguments.
                loggerWrap.put("info", new Function<Void>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Void call(Parameter scope, Function<?> callback, Object... arguments)
                            throws ResourceException, NoSuchMethodException {
                        if (arguments.length > 1 || arguments[0] instanceof String) {
                            logger.info((String) arguments[0], Arrays.copyOfRange(arguments, 1,
                                    arguments.length));
                        } else {
                            throw new NoSuchMethodException(getNoSuchMethodMessage("info",
                                    arguments));
                        }
                        return null;
                    }
                });
                // debug(string id, object... param)
                // Wraps SLF4j debug(String format, Object[] argArray)
                // Log a message at the debug level according to the specified
                // format and arguments.
                loggerWrap.put("debug", new Function<Void>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Void call(Parameter scope, Function<?> callback, Object... arguments)
                            throws ResourceException, NoSuchMethodException {
                        if (arguments.length > 1 || arguments[0] instanceof String) {
                            logger.debug((String) arguments[0], Arrays.copyOfRange(arguments, 1,
                                    arguments.length));
                        } else {
                            throw new NoSuchMethodException(getNoSuchMethodMessage("debug",
                                    arguments));
                        }
                        return null;
                    }
                });
                // trace(string id, object... param)
                // Wraps SLF4j trace(String format, Object[] argArray)
                // Log a message at the trace level according to the specified
                // format and arguments.
                loggerWrap.put("trace", new Function<Void>() {
                    @Override
                    public Void call(Parameter scope, Function<?> callback, Object... arguments)
                            throws ResourceException, NoSuchMethodException {
                        if (arguments.length > 1 || arguments[0] instanceof String) {
                            logger.trace((String) arguments[0], Arrays.copyOfRange(arguments, 1,
                                    arguments.length));
                        } else {
                            throw new NoSuchMethodException(getNoSuchMethodMessage("trace",
                                    arguments));
                        }
                        return null;
                    }
                });

                return loggerWrap;
            }
        });
    }


}

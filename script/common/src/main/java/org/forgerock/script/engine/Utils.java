/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All rights reserved.
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

package org.forgerock.script.engine;

import org.apache.commons.lang3.tuple.Pair;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.exception.ScriptThrownException;

import javax.script.ScriptException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class Utils {

    private Utils() {
    }

    public static void copyURLToFile(URL in, File out) throws IOException {
        ReadableByteChannel inChannel = Channels.newChannel(in.openStream());
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            outChannel.transferFrom(inChannel, 0, 1 << 24);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            outChannel.close();
        }
    }

    /**
     * Read large > 5Mb text files to String.
     *
     * @param file
     *            source file
     * @return content of the source {@code file}
     * @throws IOException
     *             when the source {@code file} can not be read
     */
    public final static String readLargeFile(File file) throws IOException {
        FileChannel channel = new FileInputStream(file).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
        channel.read(buffer);
        channel.close();
        return new String(buffer.array());
    }

    /**
     * Read small < 5Mb text files to String.
     *
     * @param file
     *            source file
     * @return content of the source {@code file}
     * @throws IOException
     *             when the source {@code file} can not be read
     */
    public static final String readFile(File file) throws IOException {
        return readStream(new FileInputStream(file));
    }

    public static final String readStream(InputStream stream) throws IOException {
        BufferedInputStream in = new BufferedInputStream(stream);
        SimpleByteBuffer buffer = new SimpleByteBuffer();
        /*
         * if you are reading really large files you might want to up the buffer
         * from 1024 to a max of 8192.
         */
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) {
            buffer.put(buf, len);
        }
        in.close();
        return new String(buffer.buffer, 0, buffer.write);
    }

    static class SimpleByteBuffer {

        private byte[] buffer = new byte[256];

        private int write;

        public void put(byte[] buf, int len) {
            ensure(len);
            System.arraycopy(buf, 0, buffer, write, len);
            write += len;
        }

        private void ensure(int amt) {
            int req = write + amt;
            if (buffer.length <= req) {
                byte[] temp = new byte[req * 2];
                System.arraycopy(buffer, 0, temp, 0, write);
                buffer = temp;
            }
        }

    }

    public static ScriptEngineFactory findScriptEngineFactory(String shortName,
            Iterable<ScriptEngineFactory> engineFactories) {
        ScriptEngineFactory engine = null;
        for (ScriptEngineFactory factory : engineFactories) {
            if ((factory.getNames().contains(shortName))
                    || (factory.getMimeTypes().contains(shortName))
                    || (factory.getLanguageName().equals(shortName))) {
                engine = factory;
                break;
            }
        }
        return engine;
    }

    public static Object deepCopy(final Object source) {
        if (source instanceof JsonValue) {
            return new JsonValue(deepCopy(((JsonValue) source).getObject()));
        } else if (source instanceof Collection || source instanceof Map) {
            return deepCopy(source, new Stack<Pair<Object, Object>>());
        } else {
            return source;
        }
    }

    @SuppressWarnings({ "unchecked" })
    private static Object deepCopy(Object source, final Stack<Pair<Object, Object>> valueStack) {
        Iterator<Pair<Object, Object>> i = valueStack.iterator();
        while (i.hasNext()) {
            Pair<Object, Object> next = i.next();
            if (next.getLeft() == source) {
                return next.getRight();
            }
        }

        if (source instanceof JsonValue) {
            return new JsonValue(deepCopy(((JsonValue) source).getObject(), valueStack));
        } else if (source instanceof Collection) {
            List<Object> copy = new ArrayList<Object>(((Collection) source).size());
            valueStack.push(Pair.of(source, (Object) copy));
            for (Object o : (Collection) source) {
                copy.add(deepCopy(o, valueStack));
            }
            // valueStack.pop();
            return copy;
        } else if (source instanceof Map) {
            Map copy = new LinkedHashMap(((Map) source).size());
            valueStack.push(Pair.of(source, (Object) copy));
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) source).entrySet()) {
                copy.put(entry.getKey(), deepCopy(entry.getValue(), valueStack));
            }
            // valueStack.pop();
            return copy;
        } else {
            return source;
        }
    }

    /**
     * Executes the given script with the appropriate context information.
     *
     * @param context
     * @param scriptPair
     *            The script to execute
     * @return
     * @throws ResourceException
     */
    public static Object evaluateScript(final Context context,
            final Pair<JsonPointer, ScriptEntry> scriptPair) throws ResourceException {
        if (scriptPair != null) {
            ScriptEntry scriptEntry = scriptPair.getRight();
            if (scriptEntry.isActive()) {
                throw new ServiceUnavailableException("Failed to execute inactive script: "
                        + scriptPair.getRight().getName());
            }
            Script script = scriptEntry.getScript(context);
            try {
                return script.eval();
            } catch (Throwable t) {
                throw adapt(t);
            }
        }
        return null;
    }

    /**
     * Adapts a {@code Throwable} to a {@code ResourceException}. If the
     * {@code Throwable} is an JSON {@code ScriptException} then an appropriate
     * {@code ResourceException} is returned, otherwise an
     * {@code InternalServerErrorException} is returned.
     *
     * @param t
     *            The {@code Throwable} to be converted.
     * @return The equivalent resource exception.
     */
    public static ResourceException adapt(final Throwable t) {
        int resourceResultCode;
        try {
            throw t;
        } catch (final ResourceException e) {
            return e;
        } catch (final JsonValueException e) {
            resourceResultCode = ResourceException.BAD_REQUEST;
        } catch (final ScriptThrownException e) {
            return e.toResourceException(ResourceException.INTERNAL_ERROR, e.getMessage());
        } catch (final ScriptException e) {
            resourceResultCode = ResourceException.INTERNAL_ERROR;
        } catch (final Throwable tmp) {
            resourceResultCode = ResourceException.INTERNAL_ERROR;
        }
        return ResourceException.getException(resourceResultCode, t.getMessage(), t);
    }
}

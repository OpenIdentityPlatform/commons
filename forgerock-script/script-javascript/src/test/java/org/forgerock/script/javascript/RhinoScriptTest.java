/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 ForgeRock AS. All rights reserved.
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

package org.forgerock.script.javascript;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fest.assertions.core.Condition;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.script.Script;
import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptName;
import org.forgerock.script.ScriptTest;
import org.forgerock.script.source.EmbeddedScriptSource;
import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
@Test
public class RhinoScriptTest extends ScriptTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected Map<String, Object> getConfiguration() {
        Map<String, Object> configuration = new HashMap<String, Object>(1);
        // configuration.put(RhinoScriptEngine.CONFIG_DEBUG_PROPERTY,
        // "transport=socket,suspend=y,address=9888,trace=true");
        return configuration;
    }

    protected String getLanguageName() {
        return RhinoScriptEngineFactory.LANGUAGE_NAME;
    }

    protected URL getScriptContainer(String name) {
        return RhinoScriptTest.class.getResource(name);
    }

    protected EmbeddedScriptSource getScriptSourceWithException() {
        ScriptName scriptName = new ScriptName("exception", "javascript");
        return new EmbeddedScriptSource("throw \"Access denied\";", scriptName);
    }

    // @Test
    /*
     * public void compileTest() throws Exception { Bindings b= new
     * SimpleBindings(); b.put("router", FunctionFactory.getResource());
     *
     * b =
     * getScriptRegistry().getEngineByName(RhinoScriptEngineFactory.LANGUAGE_NAME
     * ).compileBindings(new RootContext(), b);
     *
     * Object o = ((Map)b.get("router")).get("read"); o.getClass(); }
     */


    @Test
    public void testRequire() throws Exception {
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new ScriptName("require", getLanguageName()));
        Script script = scriptEntry.getScript(new RootContext());
        script.put("ketto", 5);
        assertThat(script.eval()).isEqualTo(25);
    }

    @Test
    public void testMapToString() throws Exception {
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new ScriptName("printobject", getLanguageName()));
        Script script = scriptEntry.getScript(new RootContext());
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("a", "1");
        map.put("b", "2");
        script.put("ketto", map);
        assertThat(script.eval()).is(new Condition<Object>() {
            @Override
            public boolean matches(Object value) {
                String s = (String) value;
                return s.matches("\\{\\s*\"a\"\\s*:\\s*\"1\"\\s*,\\s*\"b\"\\s*:\\s*\"2\"\\s*\\}")
                        || s.matches("\\{\\s*\"b\"\\s*:\\s*\"2\"\\s*,\\s*\"a\"\\s*:\\s*\"1\"\\s*\\}");
            }
        });

        // test null map
        map = null;
        script.put("ketto", map);
        assertThat(script.eval()).isEqualTo("null");
    }

    @Test
    public void testListToString() throws Exception {
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new ScriptName("printobject", getLanguageName()));
        Script script = scriptEntry.getScript(new RootContext());
        // test string list
        List<String> slist = new ArrayList<String>();
        slist.add("1");
        slist.add("2");
        script.put("ketto", slist);
        assertThat(script.eval()).is(new Condition<Object>() {
            @Override
            public boolean matches(Object value) {
                String s = (String) value;
                return s.matches("\\[\\s*\"1\"\\s*,\\s*\"2\"\\s*\\]");
            }
        });

        // test integer list
        List<Integer> ilist = new ArrayList<Integer>();
        ilist.add(1);
        ilist.add(2);
        script.put("ketto", ilist);
        assertThat(script.eval()).is(new Condition<Object>() {
            @Override
            public boolean matches(Object value) {
                String s = (String) value;
                return s.matches("\\[\\s*1\\s*,\\s*2\\s*\\]");
            }
        });

        // test null list
        ilist = null;
        script.put("ketto", ilist);
        assertThat(script.eval()).isEqualTo("null");
    }

    @Test
    public void testQueryRequestToString() throws Exception {
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new ScriptName("printobject", getLanguageName()));
        Script script = scriptEntry.getScript(new RootContext());
        QueryRequest request = Requests.newQueryRequest("/some/path")
                .setAdditionalParameter("arg", "value");
        script.put("ketto", request);
        String result = (String) script.eval();
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"method\"\\s*:\\s*\"query\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"resourcePath\"\\s*:\\s*\"some/path\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"additionalParameters\"\\s*:\\s*\\{\\s*\"arg\"\\s*:\\s*\"value\"\\s*\\}.*");
            }
        });
    }

    @Test
    public void testCreateRequestToString() throws Exception {
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new ScriptName("printobject", getLanguageName()));
        Script script = scriptEntry.getScript(new RootContext());
        final JsonValue object = new JsonValue(new HashMap<String, Object>());
        object.put("field1", "value1");
        object.put("field2", "value2");
        object.put("complexfield", new ArrayList<String>() {{ add("1"); add("2"); add("3"); }});
        CreateRequest request = Requests.newCreateRequest("/some/path", "myId", object);
        script.put("ketto", request);
        String result = (String) script.eval();
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"method\"\\s*:\\s*\"create\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"resourcePath\"\\s*:\\s*\"some/path\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"newResourceId\"\\s*:\\s*\"myId\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"content\"\\s*:\\s*.*\"field1\"\\s*:\\s*\"value1\".*")
                        && value.matches(".*\"content\"\\s*:\\s*.*\"field1\"\\s*:\\s*\"value1\".*")
                        && value.matches(".*\"content\"\\s*:\\s*.*\"complexfield\"\\s*:\\s*\\[\\s*\"1\"\\s*,\\s*\"2\"\\s*,\\s*\"3\"\\s*\\].*");
            }
        });
    }

    @Test
    public void testReadRequestToString() throws Exception {
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new ScriptName("printobject", getLanguageName()));
        Script script = scriptEntry.getScript(new RootContext());
        ReadRequest request = Requests.newReadRequest("/some/path", "myId");
        script.put("ketto", request);
        String result = (String) script.eval();
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"method\"\\s*:\\s*\"read\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"resourcePath\"\\s*:\\s*\"some/path/myId\".*");
            }
        });
    }

    @Test
    public void testDeleteRequestToString() throws Exception {
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new ScriptName("printobject", getLanguageName()));
        Script script = scriptEntry.getScript(new RootContext());
        DeleteRequest request = Requests.newDeleteRequest("/some/path", "myId")
                .setRevision("myrev");
        script.put("ketto", request);
        String result = (String) script.eval();
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"method\"\\s*:\\s*\"delete\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"resourcePath\"\\s*:\\s*\"some/path/myId\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"revision\"\\s*:\\s*\"myrev\".*");
            }
        });
    }

    @Test
    public void testActionRequestToString() throws Exception {
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new ScriptName("printobject", getLanguageName()));
        Script script = scriptEntry.getScript(new RootContext());
        final JsonValue object = new JsonValue(new HashMap<String, Object>());
        object.put("field1", "value1");
        object.put("field2", "value2");
        object.put("complexfield", new ArrayList<String>() {{ add("1"); add("2"); add("3"); }});
        ActionRequest request = Requests.newActionRequest("/some/path", "myId", "myAction")
                .setAdditionalParameter("arg", "value")
                .setContent(object);
        script.put("ketto", request);
        String result = (String) script.eval();
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"method\"\\s*:\\s*\"action\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"resourcePath\"\\s*:\\s*\"some/path/myId\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"action\"\\s*:\\s*\"myAction\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"additionalParameters\"\\s*:\\s*\\{\\s*\"arg\"\\s*:\\s*\"value\"\\s*\\}.*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"content\"\\s*:\\s*.*\"field1\"\\s*:\\s*\"value1\".*")
                        && value.matches(".*\"content\"\\s*:\\s*.*\"field1\"\\s*:\\s*\"value1\".*")
                        && value.matches(".*\"content\"\\s*:\\s*.*\"complexfield\"\\s*:\\s*\\[\\s*\"1\"\\s*,\\s*\"2\"\\s*,\\s*\"3\"\\s*\\].*");
            }
        });
    }

    @Test
    public void testUpdateRequestToString() throws Exception {
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new ScriptName("printobject", getLanguageName()));
        Script script = scriptEntry.getScript(new RootContext());
        final JsonValue object = new JsonValue(new HashMap<String, Object>());
        object.put("field1", "value1");
        object.put("field2", "value2");
        object.put("complexfield", new ArrayList<String>() {{ add("1"); add("2"); add("3"); }});
        UpdateRequest request = Requests.newUpdateRequest("/some/path", "myId", object);
        script.put("ketto", request);
        String result = (String) script.eval();
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"method\"\\s*:\\s*\"update\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"resourcePath\"\\s*:\\s*\"some/path/myId\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"content\"\\s*:\\s*.*\"field1\"\\s*:\\s*\"value1\".*")
                        && value.matches(".*\"content\"\\s*:\\s*.*\"field1\"\\s*:\\s*\"value1\".*")
                        && value.matches(".*\"content\"\\s*:\\s*.*\"complexfield\"\\s*:\\s*\\[\\s*\"1\"\\s*,\\s*\"2\"\\s*,\\s*\"3\"\\s*\\].*");
            }
        });
    }

    @Test
    public void testPatchRequestToString() throws Exception {
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new ScriptName("printobject", getLanguageName()));
        Script script = scriptEntry.getScript(new RootContext());
        PatchRequest request = Requests.newPatchRequest("/some/path", "myId", PatchOperation.add("field", "value"))
                .setRevision("myrev");
        script.put("ketto", request);
        String result = (String) script.eval();
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"method\"\\s*:\\s*\"patch\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"resourcePath\"\\s*:\\s*\"some/path/myId\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"revision\"\\s*:\\s*\"myrev\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\\{\\s*\"operation\"\\s*:\\s*\"add\"\\s*,\\s*\"field\"\\s*:\\s*\"/field\"\\s*,\\s*\"value\"\\s*:\\s*\"value\"\\s*\\}.*");
            }
        });
    }

    @Test
    public void testContextToString() throws Exception {
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new ScriptName("printobject", getLanguageName()));
        Script script = scriptEntry.getScript(new RootContext());
        final Context context = new SecurityContext(new RootContext(), "bjensen@example.com", null);
        script.put("ketto", context);
        String result = (String) script.eval();
        /*
            {
              "security": {
                "name": "security",
                "rootContext": false,
                "authorizationId": {},
                "authenticationId": "bjensen@example.com",
                "parent": {
                  "name": "root",
                  "rootContext": true,
                  "parent": null,
                  "id": "d51293e4-46aa-4f4c-b784-6948790c826c1"
                },
                "id": "d51293e4-46aa-4f4c-b784-6948790c826c1"
              },
              "parent": {
                "name": "root",
                "rootContext": true,
                "parent": null,
                "id": "d51293e4-46aa-4f4c-b784-6948790c826c1"
              },
              "current": {
                "name": "security",
                "rootContext": false,
                "authorizationId": {},
                "authenticationId": "bjensen@example.com",
                "parent": {
                  "name": "root",
                  "rootContext": true,
                  "parent": null,
                  "id": "d51293e4-46aa-4f4c-b784-6948790c826c1"
                },
                "id": "d51293e4-46aa-4f4c-b784-6948790c826c1"
              },
              "root": {
                "name": "root",
                "rootContext": true,
                "parent": null,
                "id": "d51293e4-46aa-4f4c-b784-6948790c826c1"
              }
            }
         */
        // result is actually a json map
        JsonValue value = json(OBJECT_MAPPER.readValue(result, Object.class));
        assertThat(value.get("current").get("name").asString()).isEqualTo("security");
        assertThat(value.get("root").get("name").asString()).isEqualTo("root");
        assertThat(value.get("security").get("name").asString()).isEqualTo("security");
        assertThat(value.get("security").get("authenticationId").asString()).isEqualTo("bjensen@example.com");
    }

    @Test
    public void testContextAccess() throws Exception {
        ScriptEntry scriptEntry = getScriptRegistry().takeScript(new ScriptName("context", getLanguageName()));
        Script script = scriptEntry.getScript(new RootContext());
        final Context context = new SecurityContext(new RootContext(), "bjensen@example.com", null);
        script.put("context", context);
        Object result = script.eval();
        System.out.println(result);
        /*
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"method\"\\s*:\\s*\"action\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"resourcePath\"\\s*:\\s*\"some/path/myId\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"action\"\\s*:\\s*\"myAction\".*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"additionalParameters\"\\s*:\\s*\\{\\s*\"arg\"\\s*:\\s*\"value\"\\s*\\}.*");
            }
        });
        assertThat(result).is(new Condition<String>() {
            @Override
            public boolean matches(String value) {
                return value.matches(".*\"content\"\\s*:\\s*.*\"field1\"\\s*:\\s*\"value1\".*")
                        && value.matches(".*\"content\"\\s*:\\s*.*\"field1\"\\s*:\\s*\"value1\".*")
                        && value.matches(".*\"content\"\\s*:\\s*.*\"complexfield\"\\s*:\\s*\\[\\s*\"1\"\\s*,\\s*\"2\"\\s*,\\s*\"3\"\\s*\\].*");
            }
        });
        */
    }
}

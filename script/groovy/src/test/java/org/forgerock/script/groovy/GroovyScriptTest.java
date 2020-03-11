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

package org.forgerock.script.groovy;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.script.ScriptName;
import org.forgerock.script.ScriptTest;
import org.forgerock.script.source.EmbeddedScriptSource;
import org.testng.annotations.Test;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
@Test
public class GroovyScriptTest extends ScriptTest {

    protected Map<String, Object> getConfiguration() {
        return new HashMap<String, Object>();
    }

    protected String getLanguageName() {
        return GroovyScriptEngineFactory.LANGUAGE_NAME;
    }

    protected URL getScriptContainer(String name) {
        return GroovyScriptTest.class.getResource(name);
    }

    protected EmbeddedScriptSource getScriptSourceWithException() {
        ScriptName scriptName =
                new ScriptName("exception", GroovyScriptEngineFactory.LANGUAGE_NAME);
        return new EmbeddedScriptSource("throw new Exception(\"Access denied\");", scriptName);
    }

    // private ScriptRegistry scriptRegistry = null;
    //
    // @BeforeClass
    // public void initScriptRegistry() throws Exception {
    // ScriptEngineManager manager =
    // ScriptEngineManager.getFactory().setCacheDirectory(
    // new
    // File(GroovyScriptTest.class.getResource("/").toURI().resolve("cache/")))
    // .newInstance();
    //
    // RequestHandler resource = mock(RequestHandler.class);
    //
    // doAnswer(new Answer<Void>() {
    // public Void answer(InvocationOnMock invocation) throws Throwable {
    // ReadRequest request = (ReadRequest) invocation.getArguments()[1];
    // ResultHandler<Resource> handler = (ResultHandler<Resource>)
    // invocation.getArguments()[2];
    // handler.handleResult(new Resource(request.getResourceId(),"1",new
    // JsonValue(new HashMap<String,Object>())));
    // return null;
    // }
    // }).when(resource).read(any(Context.class),any(ReadRequest.class),any(ResultHandler.class));
    //
    // Map<String,Object> router = new HashMap<String, Object>(7);
    //
    // for (ConnectionFunction.Operation operation :
    // ConnectionFunction.Operation.values()) {
    // router.put(operation.name().toLowerCase(),operation);
    // }
    // manager.getGlobalScope().put("router", router);
    //
    // URL container = GroovyScriptTest.class.getResource("/container/");
    // org.testng.Assert.assertNotNull(container);
    //
    // scriptRegistry = manager.getScriptLibrary();
    // scriptRegistry.addSourceUnit(new DirectoryContainer("name", container));
    // scriptRegistry.addSourceUnit(new
    // EmbeddedScriptSource(ScriptEntry.Visibility.PUBLIC,
    // "egy = egy + ketto;egy", new ScriptName("test1", "groovy")));
    //
    // }
    //
    // public ScriptRegistry getScriptRegistry() {
    // return scriptRegistry;
    // }
    //
    // //@Test
    // public void testEval() throws Exception {
    // ScriptName scriptName = new ScriptName("test1", "groovy");
    // ScriptEntry scriptEntry = getScriptRegistry().takeScript(scriptName);
    // org.testng.Assert.assertNotNull(scriptEntry);
    // scriptEntry.put("egy", 1);
    // assertThat(scriptEntry.getScript().eval()).isEqualTo(3);
    //
    // // Load Script from Directory
    // scriptEntry = getScriptRegistry().takeScript(new ScriptName("sample",
    // "groovy"));
    // org.testng.Assert.assertNotNull(scriptEntry);
    //
    // // Set ServiceLevel Scope
    // scriptEntry.put("egy", 1);
    // Script script = scriptEntry.getScript();
    //
    // // Set RequestLevel Scope
    // script.put("ketto", 2);
    // assertThat(script.eval()).isEqualTo(3);
    // }
    //
    // @Test
    // public void testResource() throws Exception {
    // ScriptName scriptName = new ScriptName("resource", "groovy");
    // ScriptEntry scriptEntry = getScriptRegistry().takeScript(scriptName);
    // org.testng.Assert.assertNotNull(scriptEntry);
    //
    // Script script = scriptEntry.getScript();
    // // Set RequestLevel Scope
    // script.put("ketto", 2);
    // assertThat(script.eval()).isEqualTo("DDOE");
    // }
    //
    // @DataProvider(name = "Data-Provider-Function")
    // public Object[][] scriptProvider() {
    // ScriptEntry scriptEntry =
    // getScriptRegistry().takeScript(new ScriptName("sample", "javascript"));
    // scriptEntry.put("egy", 1);
    // return new Object[][] { { scriptEntry } };
    // }
    //
    // // @Test(threadPoolSize = 4, invocationCount = 10, timeOut = 1000,
    // // dataProvider = "Data-Provider-Function", dependsOnMethods = {
    // "testEval" })
    // // public void parameterIntTest(ScriptEntry scriptEntry) throws Exception
    // {
    // // Script script = scriptEntry.getScript();
    // // script.put("ketto", 2);
    // // assertThat(script.eval()).isEqualTo(3);
    // // }
}

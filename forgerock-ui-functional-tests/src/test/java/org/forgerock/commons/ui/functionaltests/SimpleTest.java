package org.forgerock.commons.ui.functionaltests;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
public class SimpleTest extends AbstractTest {

	@Test
	public void simpleTest() {		
		forms.setField("content", "login", "openidm-admin");
		forms.setField("content", "password", "openidm-admin");
		forms.submit("content", "loginButton");
		messages.assertInfoMessage("You have been successfully logged in.");
		
		router.routeTo("/#users/");
	}
	
	@Test
	public void simpleTest2() {
		//router.routeTo("/#users/");	
		//router.assertUrl("/#login/");
		
		JsonNode user = jsonUtils.readJsonFromFile("/registration/user.json");
		
		router.routeTo("/#register/");
		selenium.waitForElement("content", "email", ElementType.NAME);		
		forms.fillForm("content", user);
		forms.validateForm("content");
		forms.assertFormValidationPasses("content");
		
		forms.submit("content", "register");
	}
	
}

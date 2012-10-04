package org.forgerock.commons.ui.functionaltests;

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
	
	/*@Test
	public void simpleTest2() {
		//router.routeTo("/#users/");	
		//router.assertUrl("/#login/");
		
		JsonNode user = jsonUtils.readJsonFromFile("/registration/user.json");
		
		router.goToRegistration();
		forms.fillForm("content", user);
		forms.validateForm("content");
		
		forms.assertValidationError("content", "phoneNumber");
		forms.assertValidationPasses("content", "password");
		
		//forms.assertFormValidationPasses("content");
		
		//forms.submit("content", "register");
	}
	
	@Test
	public void simpleTest3() {		
		forms.setField("content", "login", "a@a.pl");
		forms.setField("content", "password", "qweqweqweQ1");
		forms.submit("content", "loginButton");
		messages.assertInfoMessage("You have been successfully logged in.");
		
		router.goToProfile();
		JsonNode e = forms.readForm("content");
		System.out.println(e.toString());
		
		JsonNode user = jsonUtils.readJsonFromFile("/profile/user.json");
		
		Assert.assertEquals(user, e);
	}*/
	
}

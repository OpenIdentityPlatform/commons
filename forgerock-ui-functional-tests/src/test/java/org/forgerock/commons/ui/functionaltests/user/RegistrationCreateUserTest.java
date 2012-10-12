package org.forgerock.commons.ui.functionaltests.user;

import junit.framework.Assert;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.testng.annotations.Test;

public class RegistrationCreateUserTest extends AbstractRegistrationTest {
	
	@Test
	@AssertNoErrors
	public void testRegisterUser() {
		new RegistrationValidationTest(){
			@Override
			protected void checkRegistrationViewBehavior() {
				forms.submit("content", "register");
				messages.assertInfoMessage("User has been registered successfully");
				messages.assertInfoMessage("You have been successfully logged in.");
				router.goToProfile(true);
				router.assertUrl("#profile/");
				
				JsonNode e = forms.readForm("content");
				Assert.assertEquals("Aaaaa", formatStringFromForm(e.get("givenName").toString()));
				Assert.assertEquals("a@a.pl", formatStringFromForm(e.get("email").toString()));
				Assert.assertEquals("Bbbbb", formatStringFromForm(e.get("familyName").toString()));
				Assert.assertEquals("1234", formatStringFromForm(e.get("phoneNumber").toString()));
				Assert.assertEquals("", formatStringFromForm(e.get("address1").toString()));
				Assert.assertEquals("", formatStringFromForm(e.get("country").toString()));
				Assert.assertEquals("", formatStringFromForm(e.get("stateProvince").toString()));
				Assert.assertEquals("", formatStringFromForm(e.get("address2").toString()));
				Assert.assertEquals("", formatStringFromForm(e.get("city").toString()));
				Assert.assertEquals("", formatStringFromForm(e.get("postalCode").toString()));
			}
		}.run();
	}
	
}

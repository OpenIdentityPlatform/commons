package org.forgerock.commons.ui.functionaltests.user;

import javax.inject.Inject;

import junit.framework.Assert;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.AbstractTest;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrorsAspect;

public class AbstractChangeSecurityDataTest extends AbstractTest{

	@Inject
	private AssertNoErrorsAspect assertNoErrorsAspect;

	protected String formatStringFromForm(String value) {
		return value.substring(1, value.length()-1);
	}
	
	protected abstract class ChangeSecurityDataValidationTest {
		
		protected abstract void checkChangeSecurityDataViewBehavior();
		
		public final void run() {
			userHelper.createDefaultUser();
			userHelper.loginAsDefaultUser();
			assertNoErrorsAspect.assertNoErrors();
			
			router.routeTo("#profile/change_security_data/", true);
			router.assertUrl("#profile/change_security_data/");
			
			forms.assertValidationDisabled("securityDataChange", "password");
			forms.assertValidationDisabled("securityDataChange", "passwordConfirm");
			forms.assertValidationDisabled("securityDataChange", "securityAnswer");
			dialogsHelper.assertActionButtonDisabled("Update");
			
			JsonNode profileForm = forms.readForm("securityDataChange");
			Assert.assertEquals("", profileForm.get("password").getTextValue());
			Assert.assertEquals("", profileForm.get("passwordConfirm").getTextValue());
			Assert.assertEquals("1", profileForm.get("securityQuestion").getTextValue());
			Assert.assertEquals("", profileForm.get("securityAnswer").getTextValue());
			
			checkChangeSecurityDataViewBehavior();
		};

	}
	
	protected abstract class ChangeSecurityQuestionAndAnswerTest {
		
		protected abstract void checkChangeSecurityQuestionAdnAnswerBehavior();
		
		public final void run() {
			userHelper.createDefaultUser();
			
			router.routeTo("#profile/forgotten_password/");
			router.assertUrl("#profile/forgotten_password/");
			
			forms.setField("dialogs", "resetUsername", "test@test.test");
			forms.assertValidationError("dialogs", "fgtnSecurityAnswer");
			
			userHelper.loginAsDefaultUser();
			assertNoErrorsAspect.assertNoErrors();
			
			router.routeTo("#profile/change_security_data/", true);
			router.assertUrl("#profile/change_security_data/");
			
			forms.assertValidationDisabled("securityDataChange", "password");
			forms.assertValidationDisabled("securityDataChange", "passwordConfirm");
			forms.assertValidationDisabled("securityDataChange", "securityAnswer");
			dialogsHelper.assertActionButtonDisabled("Update");
			
			JsonNode profileForm = forms.readForm("securityDataChange");
			Assert.assertEquals("", profileForm.get("password").getTextValue());
			Assert.assertEquals("", profileForm.get("passwordConfirm").getTextValue());
			Assert.assertEquals("1", profileForm.get("securityQuestion").getTextValue());
			Assert.assertEquals("", profileForm.get("securityAnswer").getTextValue());
			
			checkChangeSecurityQuestionAdnAnswerBehavior();
			
			userHelper.logout();
			
			router.routeTo("#profile/forgotten_password/", true);
			router.assertUrl("#profile/forgotten_password/");
			
			forms.assertFormFieldHasValue("dialogs", "resetUsername", "");
			forms.setField("dialogs", "resetUsername", "test@test.test");
			forms.assertValidationError("dialogs", "fgtnSecurityAnswer");
			
			forms.assertFormFieldHasValue("fgtnSecurityQuestion", null, getSecurityQuestionAfterChange());
			
			forms.setField("dialogs", "fgtnSecurityAnswer", "someExampleAnswer");
			forms.assertValidationPasses("dialogs", "fgtnSecurityAnswer");
		}

		protected String getSecurityQuestionAfterChange() {
			return "What was your first pet's name?";
		};

	}
	
}

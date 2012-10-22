package org.forgerock.commons.ui.functionaltests.user;

import junit.framework.Assert;

import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.testng.annotations.Test;

@Test(singleThreaded=true)
public class ChangeSecurityDataTest extends AbstractChangeSecurityDataTest {
	
	@Test
	@AssertNoErrors
	public void testDefaultScenario() {
		new ChangeSecurityDataValidationTest() {
			@Override
			protected void checkChangeSecurityDataViewBehavior() {
				//Do nothing - just check if default test scenario passes
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testPasswordWrong() {
		new ChangeSecurityDataValidationTest() {
			@Override
			protected void checkChangeSecurityDataViewBehavior() {
				forms.setField("securityDataChange", "password", "a");
				forms.assertValidationError("securityDataChange", "password");
				forms.assertValidationError("securityDataChange", "passwordConfirm");
				forms.assertFormValidationError("securityDataChange");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testPasswordCleared() {
		new ChangeSecurityDataValidationTest() {
			@Override
			protected void checkChangeSecurityDataViewBehavior() {
				forms.setField("securityDataChange", "password", "a");
				forms.assertValidationError("securityDataChange", "password");
				forms.assertValidationError("securityDataChange", "passwordConfirm");
				forms.assertFormValidationError("securityDataChange");
				
				forms.setField("securityDataChange", "password", "a");
				forms.assertFormFieldHasValue("securityDataChange", "password", "a");
				
				dialogsHelper.assertActionButtonDisabled("Update");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testSetNewPasswordValidation() {
		new ChangeSecurityDataValidationTest() {
			@Override
			protected void checkChangeSecurityDataViewBehavior() {
				forms.setField("securityDataChange", "password", "a");
				forms.assertValidationError("securityDataChange", "password");
				forms.assertValidationError("securityDataChange", "passwordConfirm");
				forms.assertFormValidationError("securityDataChange");
				
				forms.setField("securityDataChange", "password", "Aa12345!");
				forms.assertFormFieldHasValue("securityDataChange", "password", "Aa12345!");
				
				forms.setField("securityDataChange", "passwordConfirm", "Aa12345!");
				forms.assertFormFieldHasValue("securityDataChange", "passwordConfirm", "Aa12345!");
				
				forms.assertFormValidationPasses("securityDataChange");
				
				dialogsHelper.assertActionButtonEnabled("Update");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testSetNewSecurityAnswerValidation() {
		new ChangeSecurityDataValidationTest() {
			@Override
			protected void checkChangeSecurityDataViewBehavior() {
				
				forms.setField("securityDataChange", "securityAnswer", "a");
				
				forms.assertValidationPasses("securityDataChange", "securityAnswer");
				forms.assertFormValidationPasses("securityDataChange");
				dialogsHelper.assertActionButtonEnabled("Update");
				
				
				forms.setField("securityDataChange", "securityAnswer", "");
	
				forms.assertFormFieldHasValue("securityDataChange", "securityAnswer", "");
				forms.assertValidationDisabled("securityDataChange", "securityAnswer");
				dialogsHelper.assertActionButtonDisabled("Update");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testChangeQuestionValidation() {
		new ChangeSecurityDataValidationTest() {
			@Override
			protected void checkChangeSecurityDataViewBehavior() {
				
				forms.setField("securityDataChange", "securityQuestion", "2");
				
				forms.assertValidationError("securityDataChange", "securityAnswer");
				forms.assertFormValidationError("securityDataChange");
				
				
				forms.setField("securityDataChange", "securityQuestion", "1");
				
				forms.assertValidationDisabled("securityDataChange", "securityAnswer");
				dialogsHelper.assertActionButtonDisabled("Update");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testChangeSecurityAnswer() {
		new ChangeSecurityQuestionAndAnswerTest() {
			@Override
			protected void checkChangeSecurityQuestionAdnAnswerBehavior() {
				
				forms.setField("securityDataChange", "securityAnswer", "someExampleAnswer");
				
				forms.assertValidationPasses("securityDataChange", "securityAnswer");
				forms.assertFormValidationPasses("securityDataChange");
				
				dialogsHelper.assertActionButtonEnabled("Update");
				
				forms.submit("content", "Update");
				router.assertUrl("#profile/");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testChangeSecurityQuestionAndSecurityAnswer() {
		new ChangeSecurityQuestionAndAnswerTest() {
			@Override
			protected void checkChangeSecurityQuestionAdnAnswerBehavior() {
				
				forms.setField("securityDataChange", "securityAnswer", "someExampleAnswer");
				forms.setField("securityDataChange", "securityQuestion", "2");
				
				forms.assertValidationPasses("securityDataChange", "securityAnswer");
				forms.assertFormValidationPasses("securityDataChange");
				
				dialogsHelper.assertActionButtonEnabled("Update");
				
				forms.submit("content", "Update");
				router.assertUrl("#profile/");
			}
			
			@Override
			protected String getSecurityQuestionAfterChange() {
				return "What is the name of the street you lived on as a child?";
			}
			
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testChangePassword() {
		new ChangeSecurityDataValidationTest() {
			@Override
			protected void checkChangeSecurityDataViewBehavior() {
				
				forms.setField("securityDataChange", "password", "Aa12345!");
				forms.assertFormFieldHasValue("securityDataChange", "password", "Aa12345!");
				
				forms.setField("securityDataChange", "passwordConfirm", "Aa12345!");
				forms.assertFormFieldHasValue("securityDataChange", "passwordConfirm", "Aa12345!");
				
				forms.assertFormValidationPasses("securityDataChange");
				
				dialogsHelper.assertActionButtonEnabled("Update");
				
				forms.submit("content", "Update");
				router.assertUrl("#profile/");
				
				userHelper.logout();
				
				try {
					userHelper.loginAsDefaultUser();
					Assert.fail("Password have not been changed");
				} catch (IllegalArgumentException e) {
					userHelper.login("test@test.test", "Aa12345!");
				}
				
			}
		}.run();
	}

}

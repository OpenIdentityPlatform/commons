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
				forms.setField("dialogs", "password", "a");
				forms.assertValidationError("dialogs", "password");
				forms.assertValidationError("dialogs", "passwordConfirm");
				forms.assertFormValidationError("dialogs");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testPasswordCleared() {
		new ChangeSecurityDataValidationTest() {
			@Override
			protected void checkChangeSecurityDataViewBehavior() {
				forms.setField("dialogs", "password", "a");
				forms.assertValidationError("dialogs", "password");
				forms.assertValidationError("dialogs", "passwordConfirm");
				forms.assertFormValidationError("dialogs");
				
				forms.setField("dialogs", "password", "a");
				forms.assertFormFieldHasValue("dialogs", "password", "a");
				
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
				forms.setField("dialogs", "password", "a");
				forms.assertValidationError("dialogs", "password");
				forms.assertValidationError("dialogs", "passwordConfirm");
				forms.assertFormValidationError("dialogs");
				
				forms.setField("dialogs", "password", "Aa12345!");
				forms.assertFormFieldHasValue("dialogs", "password", "Aa12345!");
				
				forms.setField("dialogs", "passwordConfirm", "Aa12345!");
				forms.assertFormFieldHasValue("dialogs", "passwordConfirm", "Aa12345!");
				
				//forms.assertFormValidationPasses("dialogs");
				
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
				
				forms.setField("dialogs", "securityAnswer", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
				
				forms.assertValidationPasses("dialogs", "securityAnswer");
				//forms.assertFormValidationPasses("dialogs");
				dialogsHelper.assertActionButtonEnabled("Update");
				
				
				forms.setField("dialogs", "securityAnswer", "");
	
				forms.assertFormFieldHasValue("dialogs", "securityAnswer", "");
				//forms.assertValidationDisabled("dialogs", "securityAnswer");
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
				
				forms.setField("dialogs", "securityQuestion", "2");
				
				forms.assertValidationError("dialogs", "securityAnswer");
				forms.assertFormValidationError("dialogs");
				
				
				forms.setField("dialogs", "securityQuestion", "1");
				
				//forms.assertValidationDisabled("dialogs", "securityAnswer");
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
				
				forms.setField("dialogs", "securityAnswer", "someExampleAnswer");
				
				forms.assertValidationPasses("dialogs", "securityAnswer");
				//forms.assertFormValidationPasses("dialogs");
				
				dialogsHelper.assertActionButtonEnabled("Update");
				
				forms.submit("dialogs", "Update");
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
				
				forms.setField("dialogs", "securityAnswer", "someExampleAnswer");
				forms.setField("dialogs", "securityQuestion", "2");
				
				forms.assertValidationPasses("dialogs", "securityAnswer");
				//forms.assertFormValidationPasses("dialogs");
				
				dialogsHelper.assertActionButtonEnabled("Update");
				
				forms.submit("dialogs", "Update");
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
				
				forms.setField("dialogs", "password", "Aa12345!");
				forms.assertFormFieldHasValue("dialogs", "password", "Aa12345!");
				
				forms.setField("dialogs", "passwordConfirm", "Aa12345!");
				forms.assertFormFieldHasValue("dialogs", "passwordConfirm", "Aa12345!");
				
				//forms.assertFormValidationPasses("dialogs");
				
				dialogsHelper.assertActionButtonEnabled("Update");
				
				forms.submit("dialogs", "Update");
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

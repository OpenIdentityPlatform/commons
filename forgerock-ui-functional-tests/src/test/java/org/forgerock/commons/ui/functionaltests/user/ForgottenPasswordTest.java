package org.forgerock.commons.ui.functionaltests.user;

import junit.framework.Assert;

import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.testng.annotations.Test;

@Test(singleThreaded=true)
public class ForgottenPasswordTest extends AbstractForgottenPasswordTest {

	@Test
	@AssertNoErrors
	public void testNotValidOnOpen() {
		new ForgottenPasswordPhase1ValidationTest() {
			@Override
			protected void checkForgottenPasswordViewBehavior() {
				//Do nothing - just check if default test scenario passes
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testEmailNotExists() {
		new ForgottenPasswordPhase1ValidationTest() {
			@Override
			protected void checkForgottenPasswordViewBehavior() {
				forms.setField("dialogs", "resetUsername", "other@test.test");
				forms.assertValidationError("dialogs", "resetUsername");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testValidUsername() {
		new ForgottenPasswordPhase2ValidationTest() {
			@Override
			protected void checkForgottenPasswordViewBehavior() {
				//Do nothing - just check if default test scenario passes
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testInvalidSecurityAnswer() {
		new ForgottenPasswordPhase2ValidationTest() {
			@Override
			protected void checkForgottenPasswordViewBehavior() {
				forms.setField("dialogs", "fgtnSecurityAnswer", "wrongAnswer");
				forms.assertFormFieldHasValue("dialogs", "fgtnSecurityAnswer", "wrongAnswer");
				forms.assertValidationError("dialogs", "fgtnSecurityAnswer");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testValidSecurityAnswer() {
		new ForgottenPasswordPhase2ValidationTest() {
			@Override
			protected void checkForgottenPasswordViewBehavior() {
				forms.setField("dialogs", "fgtnSecurityAnswer", "yyyy");
				forms.assertFormFieldHasValue("dialogs", "fgtnSecurityAnswer", "yyyy");
				forms.assertValidationPasses("dialogs", "fgtnSecurityAnswer");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testNotValidPasswordCombination() {
		new ForgottenPasswordPhase2ValidationTest() {
			@Override
			protected void checkForgottenPasswordViewBehavior() {
				forms.setField("dialogs", "fgtnSecurityAnswer", "yyyy");
				forms.assertFormFieldHasValue("dialogs", "fgtnSecurityAnswer", "yyyy");
				forms.assertValidationPasses("dialogs", "fgtnSecurityAnswer");
				
				forms.setField("dialogs", "password", "notValidPassword");
				forms.assertFormFieldHasValue("dialogs", "password", "notValidPassword");
				
				forms.setField("dialogs", "passwordConfirm", "notValidPassword");
				forms.assertFormFieldHasValue("dialogs", "passwordConfirm", "notValidPassword");
				
				forms.assertFormValidationError("dialogs");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testValidPasswordCombination() {
		new ForgottenPasswordPhase2ValidationTest() {
			@Override
			protected void checkForgottenPasswordViewBehavior() {
				forms.setField("dialogs", "fgtnSecurityAnswer", "yyyy");
				forms.assertFormFieldHasValue("dialogs", "fgtnSecurityAnswer", "yyyy");
				forms.assertValidationPasses("dialogs", "fgtnSecurityAnswer");
				
				forms.setField("dialogs", "password", "Aa12345!");
				forms.assertFormFieldHasValue("dialogs", "password", "Aa12345!");
				
				forms.setField("dialogs", "passwordConfirm", "Aa12345!");
				forms.assertFormFieldHasValue("dialogs", "passwordConfirm", "Aa12345!");
				
				forms.assertFormValidationPasses("dialogs");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testChangePassword() {
		new ForgottenPasswordPhase2ValidationTest() {
			@Override
			protected void checkForgottenPasswordViewBehavior() {
				forms.setField("dialogs", "fgtnSecurityAnswer", "yyyy");
				forms.assertFormFieldHasValue("dialogs", "fgtnSecurityAnswer", "yyyy");
				forms.assertValidationPasses("dialogs", "fgtnSecurityAnswer");
				
				forms.setField("dialogs", "password", "Aa12345!");
				forms.assertFormFieldHasValue("dialogs", "password", "Aa12345!");
				
				forms.setField("dialogs", "passwordConfirm", "Aa12345!");
				forms.assertFormFieldHasValue("dialogs", "passwordConfirm", "Aa12345!");
				
				forms.assertFormValidationPasses("dialogs");
				
				dialogsHelper.assertActionButtonEnabled("Update");
				
				forms.submit("dialogs", "Update");
				
				router.assertUrl("#");
				messages.assertInfoMessage("Password has been changed");
				
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

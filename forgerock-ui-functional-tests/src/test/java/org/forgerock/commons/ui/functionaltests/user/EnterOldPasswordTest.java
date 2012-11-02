package org.forgerock.commons.ui.functionaltests.user;

import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.testng.annotations.Test;

@Test(singleThreaded=true)
public class EnterOldPasswordTest extends AbstractEnterOldPasswordTest{
	
	@Test
	@AssertNoErrors
	public void testNotValidOnOpen() {
		new EnterOldPasswordValidationTest() {
			@Override
			protected void checkEnterOldPasswordViewBehavior() {
				//Do nothing - just check if default test scenario passes
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testEmptyPassword() {
		new EnterOldPasswordValidationTest() {
			@Override
			protected void checkEnterOldPasswordViewBehavior() {
				forms.setField("enterOldPassword", "oldPassword", "");
				assertFieldHasValue("oldPassword", "");
				
				forms.assertValidationError("enterOldPassword", "oldPassword");
				forms.assertFormValidationError("enterOldPassword");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testCheckInvalidPassword() {
		new EnterOldPasswordValidationTest() {
			@Override
			protected void checkEnterOldPasswordViewBehavior() {
				forms.setField("enterOldPassword", "oldPassword", "someWrongPassword");
				assertFieldHasValue("oldPassword", "someWrongPassword");
				
				forms.assertValidationError("enterOldPassword", "oldPassword");
				forms.assertFormValidationError("enterOldPassword");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testCheckValidPassword() {
		new EnterOldPasswordValidationTest() {
			@Override
			protected void checkEnterOldPasswordViewBehavior() {
				forms.setField("enterOldPassword", "oldPassword", "tesT#1#Test");
				router.assertUrl("#profile/change_security_data/");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testCancelByCloseCross() {
		new EnterOldPasswordValidationTest() {
			@Override
			protected void checkEnterOldPasswordViewBehavior() {
				dialogsHelper.closeDialogByClickOnCloseCross();
				router.assertUrl("#profile/");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testCancelByButton() {
		new EnterOldPasswordValidationTest() {
			@Override
			protected void checkEnterOldPasswordViewBehavior() {
				dialogsHelper.closeDialogByButtonClick();
				router.assertUrl("#profile/");
			}
		}.run();
	}
	
}

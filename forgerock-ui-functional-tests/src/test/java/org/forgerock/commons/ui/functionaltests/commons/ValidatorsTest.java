package org.forgerock.commons.ui.functionaltests.commons;

import org.forgerock.commons.ui.functionaltests.AbstractTest;
import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ValidatorsTest extends AbstractTest {
	
	@Test
	@AssertNoErrors
	public void shouldPutTickWhenValidationPassed() {
		router.goToRegistration();
		
		forms.setField("content", "email", "test@test.test");
		WebElement element = selenium.getElement("content", "ok", ElementType.CLASS);
		Assert.assertEquals("✔", element.getText());
	}
	
	@Test
	@AssertNoErrors
	public void shouldPutXWhenValidationError() {
		router.goToRegistration();
		
		forms.setField("content", "email", "test@test.test");
		WebElement element = selenium.getElement("content", "ok", ElementType.CLASS);
		
		forms.setField("content", "email", "test");
		Assert.assertEquals("x", element.getText());
	}
	
	@Test
	@AssertNoErrors
	public void shouldDisplayValidationMessage() {
		router.goToRegistration();
		
		forms.setField("content", "userName", "");
		WebElement element = selenium.getElement("content", "validationMessage", ElementType.CLASS);
		Assert.assertEquals("Required", element.getText());
	}
	
}

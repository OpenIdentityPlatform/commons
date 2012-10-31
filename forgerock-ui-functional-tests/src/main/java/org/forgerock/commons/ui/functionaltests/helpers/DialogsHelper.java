package org.forgerock.commons.ui.functionaltests.helpers;

import javax.inject.Inject;

import junit.framework.Assert;

import org.openqa.selenium.*;
import org.springframework.stereotype.Component;

@Component
public class DialogsHelper {

	@Inject
	private WebDriver driver;
	
	public void closeDialog() {
		closeDialogByButtonClick();
	}
	
	public void closeDialogByButtonClick() {
		WebElement element = driver.findElement(By.cssSelector(".dialogActions [name='close']"));
		element.click();
	}
	
	public void closeDialogByClickOnCloseCross() {
		WebElement element = driver.findElement(By.cssSelector(".dialogCloseCross > a > img"));
		element.click();
	}
	
	public void assertActionButtonDisabled(String buttonName) {
		try {
			boolean disabled = driver.findElement(By.cssSelector(".dialogActions input[name='" + buttonName + "']")).getAttribute("class").contains("inactive");
			Assert.assertTrue(disabled);
		} catch (NoSuchElementException e) {
			Assert.fail("Expected disabled button with name " + buttonName);
		}
	}
	
	public void assertActionButtonEnabled(String buttonName) {
		try {
			boolean enabled = driver.findElement(By.cssSelector(".dialogActions input[name='" + buttonName + "']")).getAttribute("class").contains("active");
			Assert.assertTrue(enabled);
		} catch (NoSuchElementException e) {
			Assert.fail("Expected enabled button with name " + buttonName);
		}
	}
	
}

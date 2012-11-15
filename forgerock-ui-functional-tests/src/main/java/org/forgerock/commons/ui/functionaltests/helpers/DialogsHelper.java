package org.forgerock.commons.ui.functionaltests.helpers;

import javax.inject.Inject;

import org.forgerock.commons.ui.functionaltests.webdriver.WebDriverFactory;
import org.openqa.selenium.*;
import org.springframework.stereotype.Component;

@Component
public class DialogsHelper {

	private WebDriver driver = WebDriverFactory.getWebDriver();
	
	@Inject
	private SeleniumHelper selenium;
	
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
	
	public void assertActionButtonDisabled(final String buttonName) {
		selenium.new AssertionWithTimeout() {
			@Override
			protected String getAssertionFailedMessage() {
				return "Button " + buttonName + " was expecte to be inactive";
			}
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return driver.findElement(By.cssSelector(".dialogActions input[name='" + buttonName + "']")).getAttribute("class").contains("inactive");
			}
		}.checkAssertion();
	}
	
	public void assertActionButtonEnabled(final String buttonName) {
		selenium.new AssertionWithTimeout() {
			@Override
			protected String getAssertionFailedMessage() {
				return "Button " + buttonName + " was expecte to be active";
			}
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return driver.findElement(By.cssSelector(".dialogActions input[name='" + buttonName + "']")).getAttribute("class").contains("active");
			}
		}.checkAssertion();
	}
	
}

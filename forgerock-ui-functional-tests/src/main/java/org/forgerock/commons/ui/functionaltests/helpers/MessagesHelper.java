package org.forgerock.commons.ui.functionaltests.helpers;

import javax.inject.Inject;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

@Component
public class MessagesHelper {
	
	@Inject
	private WebDriver driver;
	
	@Inject
	private WebDriverWait wait;
	
	public void assertInfoMessage(final String text) {
		wait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver d) {
				WebElement element = driver.findElement(By.className("confirmMessage"));
				return element.isDisplayed() && element.getText().equals(text);
			}
		});
	}
	
	public void assertErrorMessage(final String text) {
		wait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(WebDriver d) {
				WebElement element = driver.findElement(By.className("errorMessage"));
				return element.getText()!=null && element.getText().equals(text);
			}
		});
	}
	
}

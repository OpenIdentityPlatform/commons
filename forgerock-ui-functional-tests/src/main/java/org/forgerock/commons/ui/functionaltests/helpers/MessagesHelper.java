package org.forgerock.commons.ui.functionaltests.helpers;

import javax.inject.Inject;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import org.testng.Assert;

@Component
public class MessagesHelper {
	
	@Inject
	private WebDriver driver;
	
	@Inject
	private WebDriverWait wait;
	
	public void assertInfoMessage(String text) {
		WebElement message = driver.findElement(By.className("confirmMessage"));
		WebElement element = wait.until(ExpectedConditions.visibilityOf(message));
		
		if(text != null) {
			Assert.assertEquals(element.getText(), text);
		}
	}
	
	public void assertErrorMessage(String text) {
		WebElement message = driver.findElement(By.className("errorMessage"));
		WebElement element = wait.until(ExpectedConditions.visibilityOf(message));
		
		if(text != null) {
			Assert.assertEquals(element.getText(), text);
		}
	}
	
}

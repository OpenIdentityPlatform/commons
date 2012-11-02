package org.forgerock.commons.ui.functionaltests.utils;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.annotation.*;
import org.forgerock.commons.ui.functionaltests.webdriver.WebDriverFactory;
import org.openqa.selenium.*;
import org.springframework.stereotype.Component;
import org.testng.Assert;

@Aspect
@Component
public class AssertNoErrorsAspect {
	
	@Before("execution(* *.*(..)) && @annotation(AssertNoErrors) ")
	public void overwriteErrorHandlers() {		
		String script = "window.jsErrors = [];"
				+ "window.onerror = function(error) { "
				+ "window.jsErrors.push(error.namespace);"
				+ "}";
		((JavascriptExecutor)  WebDriverFactory.getWebDriver()).executeScript(script);
	}
	
	@SuppressWarnings("unchecked")
	@After("execution(* *.*(..)) && @annotation(AssertNoErrors) ")
	public void assertNoErrors() {
		Object errors = ((JavascriptExecutor) WebDriverFactory.getWebDriver()).executeScript("return JSON.stringify(window.jsErrors);");
		
		/*if(errors != null && errors.size() > 0) {
			System.out.println(errors);
			Assert.assertEquals(errors.size(), 0);
		}*/
		
		try{
			List<WebElement> messages = WebDriverFactory.getWebDriver().findElements(By.xpath("//div[contains(@class, 'errorMessage')]"));
			
			if(!messages.isEmpty()) {
				List<String> messagesText = new ArrayList<String>();
				for(WebElement message : messages) {
					System.out.println("Error: " + message.getText());
					messagesText.add(message.getText());					
				}
				
				Assert.fail("There are error messages: " + messagesText.toString());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

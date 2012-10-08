package org.forgerock.commons.ui.functionaltests.utils;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.forgerock.commons.ui.functionaltests.webdriver.WebDriverFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;
import org.testng.Assert;

@Aspect
@Component
public class AssertNoErrorsAspect {
	
	//@Inject
	//private WebDriver driver;
	
	@Before("execution(* *.*(..)) && @annotation(AssertNoErrors) ")
	public void overwriteErrorHandlers() {		
		String script = "window.jsErrors = [];"
				+ "window.onerror = function(error) { "
				+ "window.jsErrors.push(error.namespace);"
				+ "}";
		((JavascriptExecutor) WebDriverFactory.driver).executeScript(script);
	}
	
	@SuppressWarnings("unchecked")
	@After("execution(* *.*(..)) && @annotation(AssertNoErrors) ")
	public void assertNoErrors() {
		Object errors = ((JavascriptExecutor) WebDriverFactory.driver).executeScript("return JSON.stringify(window.jsErrors);");
		
		/*if(errors != null && errors.size() > 0) {
			System.out.println(errors);
			Assert.assertEquals(errors.size(), 0);
		}*/
		
		try{
			List<WebElement> messages = WebDriverFactory.driver.findElements(By.xpath("//div[contains(@class, 'errorMessage')]"));
			
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

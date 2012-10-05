package org.forgerock.commons.ui.functionaltests.utils;

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
				+ "window.jsErrors[window.jsErrors.length] = error;"
				+ "}";
		((JavascriptExecutor) WebDriverFactory.driver).executeScript(script);
	}
	
	@SuppressWarnings("unchecked")
	@After("execution(* *.*(..)) && @annotation(AssertNoErrors) ")
	public void assertNoErrors() {
		List<String> errors = (List<String>) ((JavascriptExecutor) WebDriverFactory.driver).executeScript("return window.jsErrors;");
		
		if(errors != null && errors.size() > 0) {
			System.out.println(errors);
			Assert.assertEquals(errors.size(), 0);
		}
		
		try{
			WebElement msg = WebDriverFactory.driver.findElement(By.className("errorMessage"));
			Assert.fail("Error message " + msg.getText());
		} catch(Exception e) {
			//ok
		}
	}
	
}

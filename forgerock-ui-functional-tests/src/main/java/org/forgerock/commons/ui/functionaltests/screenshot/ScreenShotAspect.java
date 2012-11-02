package org.forgerock.commons.ui.functionaltests.screenshot;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ScreenShotAspect {
	
	private ScreenShot screenShot = new ScreenShot();

	@AfterThrowing(
			pointcut = "execution(* *.*(..))", 
			throwing = "assertionError")
	public void makeScreenShot(AssertionError assertionError) {
		System.err.println("Test failed, preparing screenshot...");
		screenShot.makeScreenShot(assertionError.getLocalizedMessage(), assertionError);
	}

}

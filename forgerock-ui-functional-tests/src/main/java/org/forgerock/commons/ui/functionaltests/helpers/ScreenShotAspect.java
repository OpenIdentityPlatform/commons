package org.forgerock.commons.ui.functionaltests.helpers;

import javax.inject.Inject;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ScreenShotAspect {
	
	@Inject
	private ScreenShot screenShot;

	@AfterThrowing(
			pointcut = "execution(* org.forgerock.commons.ui.functionaltests.helpers.*.assert*(..))", 
			throwing = "assertionError")
	public void makeScreenShot(AssertionError assertionError) {
		System.err.println("Test failed, preparing screenshot...");
		screenShot.makeScreenShot(assertionError.getLocalizedMessage());
	}

}

package org.forgerock.commons.ui.functionaltests.helpers;

import org.springframework.stereotype.Component;

@Component
public class Constants {

	private Object get(Consts constant){
		if (System.getProperty(constant.getKey()) != null) {
			return System.getProperty(constant.getKey());
		} else {
			return Consts.valueOf(constant.name()).getValue();
		}
	}
	
	public String getBasePage() {
		return get(Consts.BASE_PAGE).toString();
	}

	public String getWebBrowser() {
		return get(Consts.WEB_BROWSER).toString();
	}
	
}

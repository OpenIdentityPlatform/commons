package org.forgerock.commons.ui.functionaltests.openidmclient.transfer;

import java.util.Arrays;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityBaseInfoArray extends BaseResult{
	
	private EntityBaseInfo[] result;

	public EntityBaseInfo[] getResult() {
		return result;
	}

	public void setResult(EntityBaseInfo[] result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "Result [result=" + Arrays.toString(result) + "]";
	}

}

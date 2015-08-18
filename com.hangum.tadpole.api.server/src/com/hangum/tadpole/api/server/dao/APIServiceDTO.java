/*******************************************************************************
 * Copyright (c) 2015 hangum.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     hangum - initial API and implementation
 ******************************************************************************/
package com.hangum.tadpole.api.server.dao;

/**
 * API Server DTO
 * 
 * @author hangum
 *
 */
public class APIServiceDTO {
	/** user return type. default type is JSON */
	String userReturnType = "";
	
	/** request ip */
	String requestIP = ""; 
	
	String accessKey 		= "";
	String secretKey 		= "";
	int userSEQ = 0;

	/** http request url */
	String requestURL 			= "";
	String apiServiceKey		= "";
	
	/** http request parameter */
	String requestParameter 	= "";

	String requestUserDomainURL = "";
	
	public APIServiceDTO() {
	}

	public String getUserReturnType() {
		return userReturnType;
	}

	public void setUserReturnType(String userReturnType) {
		this.userReturnType = userReturnType;
	}

	public String getRequestIP() {
		return requestIP;
	}

	public void setRequestIP(String requestIP) {
		this.requestIP = requestIP;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public int getUserSEQ() {
		return userSEQ;
	}

	public void setUserSEQ(int userSEQ) {
		this.userSEQ = userSEQ;
	}

	public String getRequestURL() {
		return requestURL;
	}

	public void setRequestURL(String requestURL) {
		this.requestURL = requestURL;
	}

	public String getApiServiceKey() {
		return apiServiceKey;
	}

	public void setApiServiceKey(String apiServiceKey) {
		this.apiServiceKey = apiServiceKey;
	}

	public String getRequestParameter() {
		return requestParameter;
	}

	public void setRequestParameter(String requestParameter) {
		this.requestParameter = requestParameter;
	}

	public String getRequestUserDomainURL() {
		return requestUserDomainURL;
	}

	public void setRequestUserDomainURL(String requestUserDomainURL) {
		this.requestUserDomainURL = requestUserDomainURL;
	}

	@Override
	public String toString() {
		return "APIServiceDTO [userReturnType=" + userReturnType + ", requestIP=" + requestIP + ", accessKey="
				+ accessKey + ", secretKey=" + secretKey + ", userSEQ=" + userSEQ + ", requestURL=" + requestURL
				+ ", apiServiceKey=" + apiServiceKey + ", requestParameter=" + requestParameter
				+ ", requestUserDomainURL=" + requestUserDomainURL + "]";
	}

	
}

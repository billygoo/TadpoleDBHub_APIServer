package com.hangum.tadpole.api.server.internal.manager;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.hangum.tadpole.api.server.dao.ErrorMessageResponseDTO;

public class ErrorMessageManager extends ErrorMessageResponseDTO {
	
	public ErrorMessageManager(int status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
	
	public ErrorMessageManager(int status, String code, String message, String devMessage) {
		this.status = status;
		this.code = code;
		this.message = message;
		this.devMessage = devMessage;
	}

	public String msgToJsonString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
	
	public Response getResponse() {
		return Response.status(getStatus())
			.entity(msgToJsonString())
			.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN + "; charset=UTF-8")
			.build();
	}

}

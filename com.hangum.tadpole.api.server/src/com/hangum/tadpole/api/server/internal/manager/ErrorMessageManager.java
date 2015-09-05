package com.hangum.tadpole.api.server.internal.manager;

import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.hangum.tadpole.api.server.dao.ErrorMessageResponseDTO;

public class ErrorMessageManager extends ErrorMessageResponseDTO {
	
	public ErrorMessageManager(String status, int code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
	
	public ErrorMessageManager(String status, int code, String message, String devMessage) {
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
		return Response.status(getCode())
			.entity(msgToJsonString())
			.build();
	}

}

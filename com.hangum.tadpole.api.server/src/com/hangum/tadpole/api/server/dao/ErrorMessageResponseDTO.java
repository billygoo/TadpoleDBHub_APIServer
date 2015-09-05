package com.hangum.tadpole.api.server.dao;

/**
 * Response error message
 * 
 * @author hangum
 *
 */
public class ErrorMessageResponseDTO {

	protected String status = "";
	protected int code;
	protected String message = "";
	protected String devMessage = "";
	
	public ErrorMessageResponseDTO() {
	}

	public ErrorMessageResponseDTO(String status, int code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDevMessage() {
		return devMessage;
	}

	public void setDevMessage(String devMessage) {
		this.devMessage = devMessage;
	}

	@Override
	public String toString() {
		return "ErrorMessageResponseDAO [status=" + status + ", code=" + code + ", message=" + message + ", devMessage="
				+ devMessage + "]";
	}
	
}

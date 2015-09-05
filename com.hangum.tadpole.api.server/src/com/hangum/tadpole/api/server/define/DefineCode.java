package com.hangum.tadpole.api.server.define;

/**
 * RESTFul API code define
 * 
 * refer to : http://apigee.com/about/blog/technology/restful-api-design-what-about-errors
 * 
 * @author hangum
 *
 */
public class DefineCode {

	public static int OK = 200;
	
	public static int Unauthorized = 401;
	public static String STR_Unauthorized = "401";
	
	public static int NotFound = 404;
	public static String STR_NotFound = "404";
	
	public static int InternalServerError = 500;
	public static String STR_InternalServerError = "400";
	
	/**
	 * <pre>
	 * 	engine 연결시 오류 
	 * </pre>
	 * */
	public static String STR_ENGINDB_EXCEPTION = "0000";
	/**
	 * <pre>
	 * 	엔진디비에서 쿼리 실행시 오류
	 * </pre>
	 */
	public static String STR_ENGINDB_SQL_EXCEPTIONO = "0001";
	
	/**
	 * restful sql exception
	 * 사용자가 정의한 쿼리를 수행하는 중에 나타나는 오류.
	 */
	public static String STR_RESTFUL_SQL_EXCEPTION = "1000";

	
	/**
	 * url parameter exception
	 */
	public static String STR_URL_PARSE_EXCEPTION = "4000";
	
	/**
	 * SQL parameter과 url의 파라미터가 올바르지 않는경우.
	 * sql parameter 변수가 없을 경우에 나타난다.
	 */
	public static String STR_ARGUMENT_NOT_MATCH_EXCEPTION = "4100";
	
	/**
	 * velocity template exception
	 */
	public static String STR_TEMPLATE_EXCEPTION = "4200";
	
	
}

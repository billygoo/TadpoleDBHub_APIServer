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
package com.hangum.tadpole.api.server.request;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.hangum.tadpole.api.server.dao.APIServiceDTO;
import com.hangum.tadpole.api.server.define.DefineCode;
import com.hangum.tadpole.api.server.internal.manager.ErrorMessageManager;
import com.hangum.tadpole.api.server.internal.manager.UserCredentialManager;
import com.hangum.tadpole.commons.dialogs.message.dao.SQLHistoryDAO;
import com.hangum.tadpole.commons.exception.TadpoleSQLManagerException;
import com.hangum.tadpole.commons.libs.core.define.PublicTadpoleDefine;
import com.hangum.tadpole.commons.util.JSONUtil;
import com.hangum.tadpole.engine.query.dao.system.UserDBDAO;
import com.hangum.tadpole.engine.query.dao.system.UserDBResourceDAO;
import com.hangum.tadpole.engine.query.sql.TadpoleSystem_ExecutedSQL;
import com.hangum.tadpole.engine.query.sql.TadpoleSystem_UserDBQuery;
import com.hangum.tadpole.engine.query.sql.TadpoleSystem_UserDBResource;
import com.hangum.tadpole.engine.restful.RESTFULUnsupportedEncodingException;
import com.hangum.tadpole.engine.restful.RESTFulArgumentNotMatchException;
import com.hangum.tadpole.engine.restful.RESTFulNotFoundURLException;
import com.hangum.tadpole.engine.restful.RESTFulSQLExecuteException;
import com.hangum.tadpole.engine.restful.RESTfulAPIUtils;
import com.hangum.tadpole.engine.restful.SQLTemplateException;
import com.hangum.tadpole.engine.sql.paremeter.NamedParameterDAO;
import com.hangum.tadpole.engine.sql.paremeter.NamedParameterUtil;
import com.hangum.tadpole.engine.sql.util.QueryUtils;
import com.hangum.tadpole.engine.sql.util.SQLUtil;

/**
 *  Tadpole API Server BASE
 * 
 * <PRE>
 * Client test tool : 
 * 		chrome-extension://hgmloofddffdnphfgcellkdfbfbjeloo/RestClient.html#RequestPlace:default
 * 
 * example url : 
 * 		http://localhost:8080/tadpoleapi/rest/base/dblist/mysql?user_seq=1&resultType=JSON
 * </PRE>
 * 
 * @author hangum
 *
 */
public class BaseQuery {
	private static final Logger logger = Logger.getLogger(BaseQuery.class);
	
	/**
	 * service gate 
	 *  
	 * @param req 
	 * @param uriInfo 
	 * @param strResultType
	 * @return
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response service(
					@Context HttpServletRequest req,
					@Context UriInfo uriInfo,
					@DefaultValue("JSON") @QueryParam("resultType") String strResultType
	) {
		// setting api dto
		APIServiceDTO apiDto = initAPIDAO(strResultType, req, uriInfo);
		
		// check your credential
		try {
			long sTimeM = System.currentTimeMillis();
			apiDto.setUserSEQ(UserCredentialManager.getCredential(apiDto));
			if(logger.isDebugEnabled()) logger.debug("Login time is " + (System.currentTimeMillis() - sTimeM));
		} catch (Exception e) {
			logger.error("Check your credential." + apiDto, e);
			
			ErrorMessageManager retDao = new ErrorMessageManager(DefineCode.Unauthorized, DefineCode.STR_Unauthorized, e.getMessage());
			return retDao.getResponse();
		}
		
		// find url
		UserDBResourceDAO userDBResourceDao = null;
		try {
			userDBResourceDao = TadpoleSystem_UserDBResource.findRESTURL(apiDto.getUserSEQ(), apiDto.getRequestUserDomainURL());
		} catch(RESTFulNotFoundURLException notFoundException) {
			ErrorMessageManager retDao = new ErrorMessageManager(DefineCode.NotFound, DefineCode.STR_NotFound 
						,String.format("Not found your request url. Check your Request URL. URL is %s", apiDto.getRequestUserDomainURL())
					);
			return retDao.getResponse();
		} catch (Exception e) {
			logger.error("Check your API Server.\n\t", e);
			
			ErrorMessageManager retDao = new ErrorMessageManager(
						DefineCode.InternalServerError, DefineCode.STR_ENGINDB_EXCEPTION 
						,String.format("Check your API Server. %s", e.getMessage())
					);
			return retDao.getResponse();
		}

		// execute sql
		try {
			String strMediaType = MediaType.TEXT_PLAIN;
			if(QueryUtils.RESULT_TYPE.HTML_TABLE.name().equalsIgnoreCase(apiDto.getUserReturnType())) {
				strMediaType = MediaType.TEXT_HTML;
			} else if(QueryUtils.RESULT_TYPE.XML.name().equalsIgnoreCase(apiDto.getUserReturnType())) {
				strMediaType = MediaType.TEXT_XML;
			}
			
			return Response.status(DefineCode.OK)
					.entity(requestQuery(userDBResourceDao, apiDto))
					.header(HttpHeaders.CONTENT_TYPE, strMediaType + "; charset=UTF-8")
					.build();
		} catch (RESTFULUnsupportedEncodingException e) {
			logger.error("URL parse exception.", e);
			ErrorMessageManager retDao 
				= new ErrorMessageManager(DefineCode.InternalServerError 
						,DefineCode.STR_URL_PARSE_EXCEPTION 
						,String.format("URL parse exception. Please URL pasrse exception. %s", e.getMessage())
				);
			return retDao.getResponse();
		} catch (TadpoleSQLManagerException e) {
			logger.error("Connection problem engine db.", e);
			ErrorMessageManager retDao = new ErrorMessageManager(DefineCode.InternalServerError 
					,DefineCode.STR_ENGINDB_EXCEPTION  
					,String.format("Connection problem engine db. %s", e.getMessage())
					,String.format("Connection problem engine db. %s", e.getMessage())
				);
			return retDao.getResponse();
		} catch (SQLException e) {
			logger.error("Connection problem engine db.", e);
			ErrorMessageManager retDao = new ErrorMessageManager(DefineCode.InternalServerError 
					,DefineCode.STR_ENGINDB_SQL_EXCEPTIONO   
					,String.format("Connection problem engine db. %s", e.getMessage())
					,String.format("Connection problem engine db. %s", e.getMessage())
				);
			return retDao.getResponse();
		} catch (SQLTemplateException e) {
			logger.error("Check user sql template.", e);
			ErrorMessageManager retDao = new ErrorMessageManager(DefineCode.InternalServerError 
					,DefineCode.STR_TEMPLATE_EXCEPTION    
					,String.format("Check user sql template. %s", e.getMessage())
					,String.format("Check user sql template. %s", e.getMessage())
				);
			return retDao.getResponse();
		} catch (RESTFulArgumentNotMatchException e) {
			logger.error("Check user sql argument.", e);
			ErrorMessageManager retDao = new ErrorMessageManager(DefineCode.InternalServerError 
					,DefineCode.STR_ARGUMENT_NOT_MATCH_EXCEPTION  
					,String.format("Check user sql argument. %s", e.getMessage())
					,String.format("Check user sql argument. %s", e.getMessage())
				);
			return retDao.getResponse();
		} catch (RESTFulSQLExecuteException e) {
			logger.error("Check user sql.", e);
			ErrorMessageManager retDao = new ErrorMessageManager(DefineCode.InternalServerError 
					,DefineCode.STR_RESTFUL_SQL_EXCEPTION 
					,String.format("Check user sql. %s", e.getMessage())
					,String.format("Check user sql. %s", e.getMessage())
				);
			return retDao.getResponse();
		}
	}
	
	/**
	 * initialize API DAO
	 * @param strResultType
	 * @param req
	 * @param uriInfo
	 * @return
	 */
	private APIServiceDTO initAPIDAO(String strResultType, HttpServletRequest req, UriInfo uriInfo) {
		APIServiceDTO apiDto = new APIServiceDTO();
		apiDto.setUserReturnType(strResultType);
		apiDto.setRequestIP(req.getRemoteAddr());
		
		apiDto.setRequestURL(req.getRequestURL().toString());
		apiDto.setRequestParameter(uriInfo.getRequestUri().getQuery());
		apiDto.setAccessKey(req.getHeader("TDB_ACCESS_KEY"));
		apiDto.setSecretKey(req.getHeader("TDB_SECRET_KEY"));
		apiDto.setRequestUserDomainURL(StringUtils.substringAfter(apiDto.getRequestURL(), "/base"));
		if(logger.isDebugEnabled()) logger.debug("[ARI Service DTO]" + apiDto);
		
		return apiDto;
	}
	
	/**
	 * call service
	 * 
	 * @param userDBResourceDao
	 * @param apiServiceDto
	 * 
	 * @throws RESTFulNotFoundURLException 
	 * @throws SQLException 
	 * @throws TadpoleSQLManagerException 
	 * @throws SQLTemplateException 
	 * @throws RESTFulSQLExecuteException 
	 * @throws RESTFulArgumentNotMatchException 
	 * @throws UnsupportedEncodingException 
	 */
	protected String requestQuery(UserDBResourceDAO userDBResourceDao, APIServiceDTO apiServiceDto) throws TadpoleSQLManagerException, SQLException, SQLTemplateException, RESTFULUnsupportedEncodingException, RESTFulArgumentNotMatchException, RESTFulSQLExecuteException {
		final Timestamp timstampStart = new Timestamp(System.currentTimeMillis());
		UserDBDAO userDB = new UserDBDAO();
	
		String strReturnResult = "";
		
		// setting dto to service key
		apiServiceDto.setApiServiceKey(userDBResourceDao.getRestapi_key());
		
		// find db
		userDB = TadpoleSystem_UserDBQuery.getUserDBInstance(userDBResourceDao.getDb_seq());

		// find sql
		String strSQLs = TadpoleSystem_UserDBResource.getResourceData(userDBResourceDao);
		
		// velocity 로 if else 가 있는지 검사합니다. 
		strSQLs = RESTfulAPIUtils.makeTemplateTOSQL(apiServiceDto.getApiServiceKey(), strSQLs, apiServiceDto.getRequestParameter());
		
		// 모든 SQL을 실행 가능한 상태로 만든다.
		strSQLs = SQLUtil.sqlExecutable(strSQLs);
		
		// 분리자 만큼 실행한다.
		for (String strSQL : strSQLs.split(PublicTadpoleDefine.SQL_DELIMITER)) {
			// execute sql
			long sTimeM = System.currentTimeMillis();
			if(QueryUtils.RESULT_TYPE.JSON.name().equalsIgnoreCase(apiServiceDto.getUserReturnType())) {
				strReturnResult += executeSQL(strSQL, apiServiceDto, userDB) + ",";
			} else {
				strReturnResult += executeSQL(strSQL, apiServiceDto, userDB);
			}
			if(logger.isDebugEnabled()) logger.debug("Execute time is " + (System.currentTimeMillis() - sTimeM));
		}
		
		if(QueryUtils.RESULT_TYPE.JSON.name().equalsIgnoreCase(apiServiceDto.getUserReturnType())) {
			strReturnResult = "[" + StringUtils.removeEnd(strReturnResult, ",") + "]"; 
		}
		
		// save called history
		saveHistoryData(userDB, timstampStart, apiServiceDto, PublicTadpoleDefine.SUCCESS_FAIL.S.name(), "");
		
		return strReturnResult;
	}
	
	/**
	 * execute sql
	 * 
	 * @param strSQL
	 * @param apiServiceDto
	 * @param userDB
	 * @return
	 * @throws RESTFulArgumentNotMatchException 
	 * @throws UnsupportedEncodingException 
	 * @throws RESTFulSQLExecuteException 
	 * @throws Exception
	 */
	private String executeSQL(String strSQL, APIServiceDTO apiServiceDto, UserDBDAO userDB) throws RESTFULUnsupportedEncodingException, RESTFulArgumentNotMatchException, RESTFulSQLExecuteException  {
		NamedParameterDAO dao = NamedParameterUtil.parseParameterUtils(strSQL, apiServiceDto.getRequestParameter());
		return getSelect(apiServiceDto, userDB, dao);
	}
	
	/**
	 * save api history
	 * 
	 * @param userDB
	 * @param timstampStart
	 * @param apiServiceDto
	 * @param strResult
	 * @param strErrorMsg
	 */
	private void saveHistoryData(final UserDBDAO userDB, Timestamp timstampStart, APIServiceDTO apiServiceDto, String strResult, String strErrorMsg) {
		SQLHistoryDAO sqlHistoryDAO = new SQLHistoryDAO();
		sqlHistoryDAO.setDbSeq(userDB.getSeq());
		sqlHistoryDAO.setStartDateExecute(timstampStart);
		sqlHistoryDAO.setEndDateExecute(new Timestamp(System.currentTimeMillis()));
		sqlHistoryDAO.setResult(strResult);
		sqlHistoryDAO.setMesssage(strErrorMsg);
		sqlHistoryDAO.setStrSQLText(apiServiceDto.getApiServiceKey() + "&" + apiServiceDto.getRequestParameter());
		
		sqlHistoryDAO.setIpAddress(apiServiceDto.getRequestIP());
		
		try {
			TadpoleSystem_ExecutedSQL.saveExecuteSQUeryResource(-1, 
					userDB, 
					PublicTadpoleDefine.EXECUTE_SQL_TYPE.API, 
					sqlHistoryDAO);
		} catch(Exception e) {
			logger.error("save history", e);
		}
	}
	
	/**
	 * get select
	 * 
	 * @param apiServiceDto 
	 * @param userDB
	 * @param dao
	 * @return
	 * @throws RESTFulSQLExecuteException
	 */
	private String getSelect(APIServiceDTO apiServiceDto, final UserDBDAO userDB, NamedParameterDAO dao) throws RESTFulSQLExecuteException {
		String strResult = "";
		
		try {
			if(SQLUtil.isStatement(dao.getStrSQL())) {
				if(QueryUtils.RESULT_TYPE.JSON.name().equalsIgnoreCase(apiServiceDto.getUserReturnType())) {
					JsonArray jsonArry = QueryUtils.selectToJson(userDB, dao.getStrSQL(), dao.getListParam());
					strResult = JSONUtil.getPretty(jsonArry.toString());
				} else if(QueryUtils.RESULT_TYPE.CSV.name().equalsIgnoreCase(apiServiceDto.getUserReturnType())) {
					strResult = QueryUtils.selectToCSV(userDB, dao.getStrSQL(), dao.getListParam(), true, "\t");
				} else if(QueryUtils.RESULT_TYPE.XML.name().equalsIgnoreCase(apiServiceDto.getUserReturnType())) {
					strResult = QueryUtils.selectToXML(userDB, dao.getStrSQL(), dao.getListParam());
				} else {
					strResult = QueryUtils.selectToHTML_TABLE(userDB, dao.getStrSQL(), dao.getListParam());
				}
				
			} else {
				strResult = QueryUtils.executeDML(userDB, dao.getStrSQL(), dao.getListParam(), apiServiceDto.getUserReturnType());
			}
		} catch(Exception e) {
			throw new RESTFulSQLExecuteException(String.format("Rise System exception.\n [ERROR]%s\n [SQL] %s\n [Parameter]%s", e.getMessage(), dao.getStrSQL(), apiServiceDto.getRequestParameter()), e);
		}
		
		return strResult;
	}

}

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
package com.hangum.tadpole.api.server;

import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
import com.hangum.tadpold.commons.libs.core.define.PublicTadpoleDefine;
import com.hangum.tadpole.api.server.dao.APIServiceDTO;
import com.hangum.tadpole.api.server.internal.manager.UserCredentialManager;
import com.hangum.tadpole.commons.dialogs.message.dao.SQLHistoryDAO;
import com.hangum.tadpole.commons.util.JSONUtil;
import com.hangum.tadpole.engine.query.dao.system.UserDBDAO;
import com.hangum.tadpole.engine.query.dao.system.UserDBResourceDAO;
import com.hangum.tadpole.engine.query.sql.TadpoleSystem_ExecutedSQL;
import com.hangum.tadpole.engine.query.sql.TadpoleSystem_UserDBQuery;
import com.hangum.tadpole.engine.query.sql.TadpoleSystem_UserDBResource;
import com.hangum.tadpole.engine.sql.util.QueryUtils;
import com.hangum.tadpole.engine.sql.util.SQLNamedParameterUtil;
import com.hangum.tadpole.engine.sql.util.SQLUtil;

/**
 * Tadpole API Server BASE
 * 
 * <PRE>
 * chrome-extension://hgmloofddffdnphfgcellkdfbfbjeloo/RestClient.html#RequestPlace:default
 * 
 * example url : 
 * 		http://localhost:8080/com.hangum.tadpole.api.server/rest/base/dblist/mysql?user_seq=1&resultType=JSON
 * </PRE>
 * 
 * @author hangum
 *
 */
@Path("/base/{root}/{child}")
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
		APIServiceDTO apiServiceDto = new APIServiceDTO();
		apiServiceDto.setUserReturnType(strResultType);
		apiServiceDto.setRequestIP(req.getRemoteAddr());
		
		apiServiceDto.setRequestURL(req.getRequestURL().toString());
		apiServiceDto.setRequestParameter(uriInfo.getRequestUri().getQuery());
		apiServiceDto.setAccessKey(req.getHeader("TDB_ACCESS_KEY"));
		apiServiceDto.setSecretKey(req.getHeader("TDB_SECRET_KEY"));
		apiServiceDto.setRequestUserDomainURL(StringUtils.substringAfter(apiServiceDto.getRequestURL(), "/base"));
		if(logger.isDebugEnabled()) logger.debug("[ARI Service DTO]" + apiServiceDto);
		
		// check your credential
		try {
			long sTimeM = System.currentTimeMillis();
			apiServiceDto.setUserSEQ(UserCredentialManager.getCredential(apiServiceDto));
			if(logger.isDebugEnabled()) logger.debug("Login time is " + (System.currentTimeMillis() - sTimeM));
		} catch (Exception e) {
			logger.error("Check your credential." + apiServiceDto, e);
			return Response.status(401)
					.entity(e.getMessage())
					.build();
		}
			
		try {		
			// return to result
			String strMediaType = MediaType.TEXT_PLAIN;
			if(QueryUtils.RESULT_TYPE.HTML_TABLE.name().equalsIgnoreCase(apiServiceDto.getUserReturnType())) {
				strMediaType = MediaType.TEXT_HTML;
			} else if(QueryUtils.RESULT_TYPE.XML.name().equalsIgnoreCase(apiServiceDto.getUserReturnType())) {
				strMediaType = MediaType.TEXT_XML;
			}
			
			return Response.status(200)
					.entity(requestQuery(apiServiceDto))
					.header(HttpHeaders.CONTENT_TYPE, strMediaType + "; charset=UTF-8")
					.build();
		} catch (Exception e) {
			logger.error("Requset service error.", e);
			return returnSystemError("Requset service error.\n" + e.getMessage());
		}
	}
	
	/**
	 * call service
	 * 
	 * @param apiServiceDto
	 */
	private String requestQuery(APIServiceDTO apiServiceDto) throws Exception {
		final Timestamp timstampStart = new Timestamp(System.currentTimeMillis());
		UserDBDAO userDB = new UserDBDAO();
		
		try {
			UserDBResourceDAO userDBResourceDao = TadpoleSystem_UserDBResource.findRESTURL(apiServiceDto.getUserSEQ(), apiServiceDto.getRequestUserDomainURL());

			if(userDBResourceDao == null) {
				throw new Exception("Not found your request url. Check your Request URL.");
			} else {
				String strReturnResult = "";
				
				// setting dto to service key
				apiServiceDto.setApiServiceKey(userDBResourceDao.getRestapi_key());
				
				// find db
				userDB = TadpoleSystem_UserDBQuery.getUserDBInstance(userDBResourceDao.getDb_seq());

				// find sql
				String strSQLs = TadpoleSystem_UserDBResource.getResourceData(userDBResourceDao);
				for (String strSQL : strSQLs.split(PublicTadpoleDefine.SQL_DELIMITER)) {
					// execute sql
					long sTimeM = System.currentTimeMillis();
					if(QueryUtils.RESULT_TYPE.JSON.name().equalsIgnoreCase(apiServiceDto.getUserReturnType())) {
						strReturnResult += executeSQL(SQLUtil.sqlExecutable(strSQL), apiServiceDto, userDB) + ",";
					} else {
						strReturnResult += executeSQL(SQLUtil.sqlExecutable(strSQL), apiServiceDto, userDB);
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
			
		} catch (Exception e) {
			logger.error("requestQuery", e);
			saveHistoryData(userDB, timstampStart, apiServiceDto, PublicTadpoleDefine.SUCCESS_FAIL.F.name(), e.getMessage());
			throw e;
		}
	}
	
	/**
	 * execute sql
	 * 
	 * @param strSQL
	 * @param apiServiceDto
	 * @param userDB
	 * @return
	 * @throws Exception
	 */
	private String executeSQL(String strSQL, APIServiceDTO apiServiceDto, UserDBDAO userDB) throws Exception {
		try {
			String strLastSQL = strSQL;
			
			// parse request parameter
			SQLNamedParameterUtil oracleNamedParamUtil = SQLNamedParameterUtil.getInstance();
			String strJavaSQL = oracleNamedParamUtil.parse(strSQL);
			
			Map<Integer, String> mapIndex = oracleNamedParamUtil.getMapIndexToName();
			List<Object> listParam = new ArrayList<>();
			if(!mapIndex.isEmpty()) {
				strLastSQL = strJavaSQL;
				listParam = makeOracleListParameter(mapIndex, apiServiceDto.getRequestParameter());
			} else {
				listParam = makeJavaListParameter(apiServiceDto.getRequestParameter());
			}
			
			return getSelect(apiServiceDto.getUserReturnType(), userDB, strLastSQL, listParam);
		} catch(Exception e) {
			throw new Exception(String.format("Check your query.\n [SQL] %s\n[Parameter]%s", strSQL, apiServiceDto.getRequestParameter()));
		}
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
	 * @param userDB
	 * @param strSQL
	 * @param listParam
	 * @return
	 * @throws Exception
	 */
	private String getSelect(String strResultType, final UserDBDAO userDB, String strSQL, List<Object> listParam) throws Exception {
		String strResult = "";
		
		if(SQLUtil.isStatement(strSQL)) {
			if(QueryUtils.RESULT_TYPE.JSON.name().equalsIgnoreCase(strResultType)) {
				JsonArray jsonArry = QueryUtils.selectToJson(userDB, strSQL, listParam);
				strResult = JSONUtil.getPretty(jsonArry.toString());
			} else if(QueryUtils.RESULT_TYPE.CSV.name().equalsIgnoreCase(strResultType)) {
				strResult = QueryUtils.selectToCSV(userDB, strSQL, listParam, true, "\t");
			} else if(QueryUtils.RESULT_TYPE.XML.name().equalsIgnoreCase(strResultType)) {
				strResult = QueryUtils.selectToXML(userDB, strSQL, listParam);
			} else {
				strResult = QueryUtils.selectToHTML_TABLE(userDB, strSQL, listParam);
			}
			
		} else {
			strResult = QueryUtils.executeDML(userDB, strSQL, listParam, strResultType);
		}
		
		return strResult;
	}
	
	/**
	 * make oracle type sql parameter
	 * 
	 * @param mapIndex
	 * @param strArgument
	 * @return
	 */
	private List<Object> makeOracleListParameter(Map<Integer, String> mapIndex, String strArgument) throws Exception {
		List<Object> listParam = new ArrayList<Object>();
		
		if(logger.isDebugEnabled()) logger.debug("original URL is ===> " + strArgument);
		Map<String, String> params = new HashMap<String, String>();
		for (String param : StringUtils.split(strArgument, "&")) {
			String pair[] = StringUtils.split(param, "=");
			String key = URLDecoder.decode(pair[0], "UTF-8");
			String value = "";
			if (pair.length > 1) {
				try {
					value = URLDecoder.decode(pair[1], "UTF-8");
				} catch(Exception e) {
					value = pair[1];
				}
			}

			params.put(key, value);
		}
		
		for(int i=1; i<=mapIndex.size(); i++ ) {
			String strKey = mapIndex.get(i);
			
			if(!params.containsKey(strKey)) {
				throw new Exception("SQL Parameter not found. key name is " + strKey);
			} else {
				listParam.add( params.get(strKey) );
			}
		}
		return listParam;
	}

	
	/**
	 * make parameter list
	 * 
	 * @param strArgument 
	 * @return
	 * @throws Exception
	 */
	private List<Object> makeJavaListParameter(String strArgument) throws Exception {
		List<Object> listParam = new ArrayList<Object>();
		
		if(logger.isDebugEnabled()) logger.debug("original URL is ===> " + strArgument);
		Map<String, String> params = new HashMap<String, String>();
		for (String param : StringUtils.split(strArgument, "&")) {
			String pair[] = StringUtils.split(param, "=");
			String key = URLDecoder.decode(pair[0], "UTF-8");
			String value = "";
			if (pair.length > 1) {
				try {
					value = URLDecoder.decode(pair[1], "UTF-8");
				} catch(Exception e) {
					value = pair[1];
				}
			}

			params.put(key, value);
		}

		// assume this count... no way i'll argument is over 100..... --;;
		for(int i=1; i<100; i++) {
			if(params.containsKey(String.valueOf(i))) {
				listParam.add(params.get(""+i));
			} else {
				break;
			}
		}

		return listParam;
	}


	/**
	 * Return Error Service.
	 * 
	 * Case
	 * 	1. Do not connect Engine DB.
	 *  2. Do not connect System DB.
	 * 
	 * @param strMsg
	 * @return
	 */
	private Response returnSystemError(String strMsg) {
		return Response.status(500)
				.entity(strMsg)
				.build();
	}
	
}
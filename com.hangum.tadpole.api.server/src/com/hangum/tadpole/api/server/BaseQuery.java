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
import com.hangum.tadpole.commons.dialogs.message.dao.SQLHistoryDAO;
import com.hangum.tadpole.commons.util.JSONUtil;
import com.hangum.tadpole.engine.query.dao.system.UserDBDAO;
import com.hangum.tadpole.engine.query.dao.system.UserDBResourceDAO;
import com.hangum.tadpole.engine.query.sql.TadpoleSystem_ExecutedSQL;
import com.hangum.tadpole.engine.query.sql.TadpoleSystem_UserDBQuery;
import com.hangum.tadpole.engine.query.sql.TadpoleSystem_UserDBResource;
import com.hangum.tadpole.engine.sql.util.QueryUtils;
import com.hangum.tadpole.engine.sql.util.SQLUtil;

/**
 * Tadpole API Server BASE
 * 
 * example url : http://localhost:8080/com.hangum.tadpole.api.server/rest/base?serviceID=ac464340-704b-4123-9f95-b2c285094250&1=2&resultType=JSON
 * 
 * @author hangum
 *
 */
@Path("/base")
public class BaseQuery {
	private static final Logger logger = Logger.getLogger(BaseQuery.class);

	/**
	 * service gate 
	 *  
	 * @param req 
	 * @param uriInfo 
	 * @param strServiceID
	 * @param strResultType
	 * @return
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response service(
					@Context HttpServletRequest req,
					@Context UriInfo uriInfo,
					@QueryParam("serviceID") String strServiceID,
					@DefaultValue("JSON") @QueryParam("resultType") String strResultType
	) {
		
		String strUriQuery = uriInfo.getRequestUri().getQuery();
		if(logger.isDebugEnabled()) logger.debug("[serviceID]\t" + strServiceID);
		
		if("".equals(strServiceID)) {
			return Response.status(404).entity("service id not found. check your request.").build();
		} else {
			try {		
				String strResult =  requestQuery(req.getRemoteAddr(), strServiceID, strUriQuery, strResultType);
				
				if(QueryUtils.RESULT_TYPE.HTML_TABLE.name().equalsIgnoreCase(strResultType)) {
					return Response.status(200)
							.entity(strResult)
							.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML + "; charset=UTF-8")
							.build();
				} else {
					return Response.status(200)
							.entity(strResult)
							.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN + "; charset=UTF-8")
							.build();
				}
			} catch (Exception e) {
				logger.error("service call exception : service id : " + strServiceID, e);
				return Response.status(400).entity("Service is error. " + e.getMessage()).build();
			}
		}
	}
	
	/**
	 * call service
	 * 
	 * @param strRemoteAddrr
	 * @param strServiceID
	 * @param strUriQuery
	 * @param strResultType
	 */
	private String requestQuery(String strRemoteAddrr, String strServiceID, String strUriQuery, String strResultType) throws Exception {
		
		final Timestamp timstampStart = new Timestamp(System.currentTimeMillis());
		UserDBDAO userDB = null;
		
		try {
			UserDBResourceDAO userDBResourceDao = TadpoleSystem_UserDBResource.findAPIKey(strServiceID);
			if(userDBResourceDao == null) {
				throw new Exception("Service ID is " + strServiceID + " not found. Check service ID is ask your serverice provider.");
			} else {
				
				String strSQL = TadpoleSystem_UserDBResource.getResourceData(userDBResourceDao);
				if(logger.isDebugEnabled()) logger.debug("===> resource info: " + userDBResourceDao.getName() + ", " + strSQL);
				
				// find db
				userDB = TadpoleSystem_UserDBQuery.getUserDBInstance(userDBResourceDao.getDb_seq());
				List<Object> listParam = makeListParameter(strUriQuery);
				
				String strResult = getSelect(strResultType, userDB, strSQL, listParam);
				if(logger.isDebugEnabled()) logger.debug("\t===> result is " + strResult);
				
				// save called history
				saveHistoryData(strRemoteAddrr, userDB, timstampStart, strServiceID, strUriQuery, PublicTadpoleDefine.SUCCESS_FAIL.S.name(), "");
				
				return strResult;
			}
			
		} catch (Exception e) {
			logger.error("find api", e);
			saveHistoryData(strRemoteAddrr, userDB, timstampStart, strServiceID, strUriQuery, PublicTadpoleDefine.SUCCESS_FAIL.F.name(), e.getMessage());
			throw e;
		}
		
	}
	
	/**
	 * save api history
	 * 
	 * @param userDB
	 * @param timstampStart
	 * @param strApiname
	 * @param strApiArgument
	 * @param strResult
	 * @param strErrorMsg
	 */
	private void saveHistoryData(String strRemoteAddrr, final UserDBDAO userDB, Timestamp timstampStart, String strApiname, String strApiArgument, String strResult, String strErrorMsg) {
		SQLHistoryDAO sqlHistoryDAO = new SQLHistoryDAO();
		sqlHistoryDAO.setDbSeq(userDB.getSeq());
		sqlHistoryDAO.setStartDateExecute(timstampStart);
		sqlHistoryDAO.setEndDateExecute(new Timestamp(System.currentTimeMillis()));
		sqlHistoryDAO.setResult(strResult);
		sqlHistoryDAO.setMesssage(strErrorMsg);
		sqlHistoryDAO.setStrSQLText(strApiname + "&" + strApiArgument);
		
		sqlHistoryDAO.setIpAddress(strRemoteAddrr);
		
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
	 * make parameter list
	 * 
	 * @param strURLQuery
	 * @return
	 * @throws Exception
	 */
	private List<Object> makeListParameter(String strURLQuery) throws Exception {
		List<Object> listParam = new ArrayList<Object>();

		if(logger.isDebugEnabled()) logger.debug("original URL is ===> " + strURLQuery);
		Map<String, String> params = new HashMap<String, String>();
		for (String param : StringUtils.split(strURLQuery, "&")) {
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

}
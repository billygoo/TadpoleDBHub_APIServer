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
package com.hangum.tadpole.api.server.internal.manager;

import java.util.List;

import org.apache.log4j.Logger;

import com.hangum.tadpole.api.server.dao.APIServiceDTO;
import com.hangum.tadpole.engine.query.dao.system.UserInfoDataDAO;
import com.hangum.tadpole.engine.query.sql.TadpoleSystem_UserInfoData;

/**
 * User credential manager
 * 
 * @author hangum
 *
 */
public class UserCredentialManager {
	private static final Logger logger = Logger.getLogger(UserCredentialManager.class);
	
	/**
	 * get credential 
	 * 
	 * @param apiServiceDto
	 * @return
	 * @throws Exception
	 */
	public static int getCredential(APIServiceDTO apiServiceDto) throws Exception {
		int intUserSEQ = 0;
		try {
			List<UserInfoDataDAO> listCredential = TadpoleSystem_UserInfoData.getUserCredential(apiServiceDto.getAccessKey(), apiServiceDto.getSecretKey());
			if(!listCredential.isEmpty()) {
				if(listCredential.size() == 1) throw new Exception("Authorization exception. Check your access key and secret key.");
				
				// user_seq가 모두 맞아야 정상이다.
				for (UserInfoDataDAO userInfoDataDAO : listCredential) {
					if(intUserSEQ == 0) {
						intUserSEQ = userInfoDataDAO.getUser_seq();
					} else {
						if(intUserSEQ != userInfoDataDAO.getUser_seq()) {
							throw new Exception("Authorization exception. Check your access key and secret key.");
						}
					}
				}
			} else {
				throw new Exception("Not found user credential. Check your access key and secret key.");
			}
			
		} catch (Exception e) {
			logger.error("Check your credential.", e);
			
			throw e;
		}
		
		return intUserSEQ;

	}
}

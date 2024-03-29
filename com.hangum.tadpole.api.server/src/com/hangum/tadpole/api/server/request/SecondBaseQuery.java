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

import javax.ws.rs.Path;

/**
 * second base uri
 * 
 * @author hangum
 *
 */
@Path("/base/{first}/{second}")
public class SecondBaseQuery extends BaseQuery {
}

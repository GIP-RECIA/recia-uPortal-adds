/**
 * Copyright © 2018 GIP-RECIA (https://www.recia.fr/)
 * Copyright © 2018 Esup Portail https://www.esup-portail.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.recia.portal.events.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apereo.portal.security.IPerson;
import org.apereo.portal.security.IPersonManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author GIP RECIA 2013
 *
 */
public abstract class Helper {

	/** Logger. */
	private static final Logger LOG = LoggerFactory.getLogger(Helper.class);

	/**
	 * Retrieve the IPerson object from the session.
	 *
	 * @param session The user session
	 * @return the IPerson object
	 */
	public static IPerson retrievePerson(final HttpSession session){
		IPerson result = null;

		if (session != null) {
			result = (IPerson)session.getAttribute(IPersonManager.PERSON_SESSION_KEY);
		}

		return result;
	}

	/**
	 * Retrieve the HttpRequest in the current request context.
	 *
	 * @return the HttpRequest.
	 */
	public static HttpServletRequest retrieveHttpRequest() {
		final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes == null ||
				!ServletRequestAttributes.class.isAssignableFrom(requestAttributes.getClass())) {
			Helper.LOG.error("Problem encountered while retrieving ServletRequestAttributes, your certainly not in a HTTP Request context !");
			return null;
		}

		final ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
		final HttpServletRequest httpRequest = servletRequestAttributes.getRequest();

		return httpRequest;
	}


}

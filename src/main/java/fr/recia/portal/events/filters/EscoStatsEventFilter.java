/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fr.recia.portal.events.filters;

import org.apereo.portal.events.LoginEvent;
import org.apereo.portal.events.LogoutEvent;
import org.apereo.portal.events.PortalRenderEvent;
import org.apereo.portal.events.PortletRenderExecutionEvent;
import org.apereo.portal.spring.context.ApplicationEventFilter;
import org.springframework.context.ApplicationEvent;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;

/**
 * Filter which supports only the stats relative events.
 *
 * @author GIP RECIA 2013
 *
 */
public class EscoStatsEventFilter implements ApplicationEventFilter<ApplicationEvent> {

	@Override
	public boolean supports(ApplicationEvent event) {
		boolean test = LoginEvent.class.isAssignableFrom(event.getClass())
				|| LogoutEvent.class.isAssignableFrom(event.getClass())
				|| PortalRenderEvent.class.isAssignableFrom(event.getClass())
				|| PortletRenderExecutionEvent.class.isAssignableFrom(event.getClass())
				|| HttpSessionCreatedEvent.class.isAssignableFrom(event.getClass())
				|| HttpSessionDestroyedEvent.class.isAssignableFrom(event.getClass());

		return test;
	}

}

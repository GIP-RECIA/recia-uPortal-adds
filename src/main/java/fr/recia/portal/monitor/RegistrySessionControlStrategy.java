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
package fr.recia.portal.monitor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.util.Assert;

public class RegistrySessionControlStrategy implements SessionAuthenticationStrategy {

	private final SessionRegistry sessionRegistry;

    public RegistrySessionControlStrategy(SessionRegistry sessionRegistry) {
        Assert.notNull(sessionRegistry, "The sessionRegistry cannot be null");
        this.sessionRegistry = sessionRegistry;
    }

	@Override
	public void onAuthentication(Authentication authentication,
                                 HttpServletRequest request, HttpServletResponse response)
			throws SessionAuthenticationException {
        sessionRegistry.registerNewSession(request.getSession().getId(), authentication.getPrincipal());
	}

}

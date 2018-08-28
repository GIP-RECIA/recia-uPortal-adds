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
package fr.recia.portal.security.guest;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apereo.portal.security.provider.IGuestUsernameSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class guestUsernameSelector implements IGuestUsernameSelector {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String defaultGuest = "guest";

    private Map<String, String> serverNameMapping;

    public guestUsernameSelector(final Map<String, String> serverNamemapping) {
        serverNameMapping = serverNamemapping;
    }

    @Override
    public String selectGuestUsername(HttpServletRequest httpServletRequest) {
        final String mappedGuest = serverNameMapping.get(httpServletRequest.getServerName());
        logger.debug("Loaded mapping {} and for request serverName {} found {}", serverNameMapping,
                httpServletRequest.getServerName(), mappedGuest);
        if (mappedGuest != null) {
            logger.debug("Selected guest username {}", mappedGuest);
            return mappedGuest;
        }
        logger.debug("Returning default guest username {}", defaultGuest);
        return defaultGuest;
    }

    @Override
    public int compareTo(IGuestUsernameSelector o) {
        return 0;
    }

    public void setDefaultGuest(String defaultGuest) {
        this.defaultGuest = defaultGuest;
    }

    public void setServerNameMapping(Map<String, String> serverNameMapping) {
        this.serverNameMapping = serverNameMapping;
    }
}

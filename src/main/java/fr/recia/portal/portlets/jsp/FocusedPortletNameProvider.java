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
package fr.recia.portal.portlets.jsp;

import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;

import org.apereo.portal.portlet.om.IPortletDefinition;
import org.apereo.portal.portlet.om.IPortletEntity;
import org.apereo.portal.portlet.om.IPortletWindow;
import org.apereo.portal.portlet.registry.IPortletWindowRegistry;
import org.apereo.portal.url.IPortalRequestInfo;
import org.apereo.portal.url.IPortletRequestInfo;
import org.apereo.portal.url.IUrlSyntaxProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public final class FocusedPortletNameProvider {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired private IUrlSyntaxProvider urlSyntaxProvider;

    @Autowired private IPortletWindowRegistry portletWindowRegistry;

    public String getInfo(final HttpServletRequest req) {
        String focusedPortletName = "";
        logger.debug("Request {}", req);
        try {
            final IPortalRequestInfo portalRequestInfo =
                    this.urlSyntaxProvider.getPortalRequestInfo(req);
            final IPortletRequestInfo portletRequestInfo =
                    portalRequestInfo.getTargetedPortletRequestInfo();
            if (portletRequestInfo != null) {
                final IPortletWindow portletWindow =
                        portletWindowRegistry.getPortletWindow(req,
                                portletRequestInfo.getPortletWindowId());
                if (portletWindow != null) {
                    final IPortletEntity portletEntity = portletWindow.getPortletEntity();
                    final IPortletDefinition portletDefinition =
                            portletEntity.getPortletDefinition();
                    if (WindowState.MAXIMIZED.equals(portletRequestInfo.getWindowState())) {
                        focusedPortletName = portletDefinition.getTitle();
                        logger.debug(
                                "Maximized Window State on targeted Portlet  {}",
                                portletDefinition.getTitle());
                    }
                }
            }
        } catch (NullPointerException npe) {
            logger.error("JspInvokerPortletController - NPE :", npe);
        }
        return focusedPortletName;
    }
}

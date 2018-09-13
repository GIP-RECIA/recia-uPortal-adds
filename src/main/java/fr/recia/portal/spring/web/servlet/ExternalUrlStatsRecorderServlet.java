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
package fr.recia.portal.spring.web.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.recia.portal.stats.ExternalStatsLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * This servlet record access to external link.
 */
@Controller
public class ExternalUrlStatsRecorderServlet implements InitializingBean {

	/** Logger .*/
	private static final Logger LOG = LoggerFactory.getLogger(ExternalUrlStatsRecorderServlet.class);

	public static final String FNAME_HTTP_PARAM = "fname";
	public static final String SERVICE_HTTP_PARAM = "service";

	@Autowired(required = true)
	private ExternalStatsLogger externalStatsLogger;

	@RequestMapping(value="/ExternalURLStats", method = RequestMethod.GET)
	public void handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		LOG.debug("External URL Stat called...");

		final String fname = request.getParameter(FNAME_HTTP_PARAM);
		final String service = request.getParameter(SERVICE_HTTP_PARAM);

		if(LOG.isDebugEnabled()){
			LOG.debug("fname: [{}]", fname);
			LOG.debug("service: [{}]", service);
		}

		// scan parameters
		if(!StringUtils.hasText(fname) || !StringUtils.hasText(service)) {
			LOG.error("Bad parameters ! fname: [{}], service: [{}]", fname, service);
			return;
		}

		this.externalStatsLogger.processExternalURLCall(request, fname, service);

		response.sendRedirect(service);

		LOG.debug("External URL Stat processing ended.");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.externalStatsLogger, "ExternalStatsLogger not configured !");
		LOG.info("ExternalUrlStatsRecorder servlet initialized.");
	}

	public ExternalStatsLogger getStatsLogger() {
		return externalStatsLogger;
	}

	public void setStatsLogger(ExternalStatsLogger statsLogger) {
		this.externalStatsLogger = statsLogger;
	}

}

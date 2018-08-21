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
/**
 *
 */
package fr.recia.portal.stats;

import javax.servlet.http.HttpServletRequest;

/**
 * @author GIP RECIA 2013
 *
 */
public interface ExternalStatsLogger {

	/** Process external stats.
	 * @param fname The fname
	 * @param request The request
	 * @param service The service
	 */
	void processExternalURLCall(HttpServletRequest request, String fname, String service);

}

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

	/** Process external stats. */
	void processExternalURLCall(HttpServletRequest request, String fname, String service);

}

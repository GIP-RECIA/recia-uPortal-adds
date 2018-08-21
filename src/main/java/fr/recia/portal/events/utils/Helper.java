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
	 * @param session
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
	 * @return
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

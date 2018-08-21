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
package fr.recia.portal.events.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import fr.recia.portal.events.filters.EscoStatsEventFilter;
import fr.recia.portal.events.utils.Helper;
import fr.recia.portal.events.utils.NodeStatsRecorder;
import fr.recia.portal.events.utils.StatsRecorder;
import fr.recia.portal.stats.ExternalStatsLogger;
import org.apereo.portal.events.LoginEvent;
import org.apereo.portal.events.LogoutEvent;
import org.apereo.portal.events.PortalEvent;
import org.apereo.portal.events.PortalRenderEvent;
import org.apereo.portal.events.PortletRenderExecutionEvent;
import org.apereo.portal.layout.node.IUserLayoutChannelDescription;
import org.apereo.portal.layout.node.IUserLayoutNodeDescription;
import org.apereo.portal.security.IPerson;
import org.apereo.portal.security.IPersonManager;
import org.apereo.portal.security.PortalSecurityException;
import org.apereo.portal.spring.context.ApplicationEventFilter;
import org.apereo.portal.spring.context.FilteringApplicationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Hands off queued portal events for logging in a stats log file.
 *
 * @author GIP RECIA 2013
 */
public class EscoStatsLogger extends FilteringApplicationListener<ApplicationEvent>
		implements ExternalStatsLogger, InitializingBean {

	/** Logger. */
	private static final Logger LOG = LoggerFactory.getLogger(EscoStatsLogger.class);

	public static final String TAG_LOGIN = "LI";
	public static final String TAG_LOGOUT = "LO";
	public static final String TAG_SESSION_CREATED = "SSTART";
	public static final String TAG_SESSION_DESTROYED = "SSTOP";
	public static final String TAG_CHANNEL_RENDERED = "CTARG";
	public static final String TAG_EXTERNAL_URL_CALL = "CCALL_EXT";

	public static final String USERAGENT_HTTP_HEADER = "User-Agent";

	public final static String PERSON_ATTR_KEY_SESSIONID = "esco.stats.sessionId";
	public final static String PERSON_ATTR_KEY_NODESTATS = "esco.stats.nodesStats";

	public static final String UNKNOWN_TYPE = "UNKNOWN";

	public static final String STATS_SEPARATOR = "\t";

	/** Serial used to generate stats incremented session id. */
	protected int serial = 0;

	/** Logger wich log esco stats. */
	private Logger statsLogger = LoggerFactory.getLogger("esco-stats");

	/** IPersonn attribute containing the ESCOUAICourant value. */
	private String personAttrCurrentUai = "ESCOUAICourant";

	/** IPersonn attribute containing the user type value. */
	private String personAttrUserType = "objectClass";

	/** Do we print Rendered stats immedatly or on logout ? */
	private boolean printOnLogout = true;

	/** Do we eliminat guest from logging ? */
	private boolean eliminateGuest = true;

	/** Node names which must be filtered (not processed). */
	private Set<String> filteredFnames = Collections.emptySet();

	/** Period during which an event is "recent" in ms. By default 1 min. */
	private long recentPeriod = 60000L;

	private Pattern fnamePattern = Pattern.compile(".+\\/p\\/([^.\\/]+).*");

	@Autowired
	private IPersonManager personManager;

	@Override
	protected void onFilteredApplicationEvent(ApplicationEvent event) {
		this.processEscoStatEvent(event);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		List<ApplicationEventFilter<ApplicationEvent>> applicationEventFilters = new ArrayList<ApplicationEventFilter<ApplicationEvent>>(
				1);
		applicationEventFilters.add(new EscoStatsEventFilter());

		this.setApplicationEventFilters(applicationEventFilters);

		Assert.hasText(this.personAttrCurrentUai, "Property personAttrCurrentUai not configured !");
		Assert.hasText(this.personAttrUserType, "Property personAttrUserType not configured !");
		Assert.notNull(this.statsLogger, "Stats Logger not configured !");
	}

	/* Pierre: utilise par la servlet ExternalUrlStatsRecorderServlet */
	public void processExternalURLCall(final HttpServletRequest request, final String fname, final String service) {
		try {
			final IPerson person = this.personManager.getPerson(request);
			final String userAgent = request.getHeader(USERAGENT_HTTP_HEADER);

			this.logStatsDatas(EscoStatsLogger.TAG_EXTERNAL_URL_CALL, this.getTypePerson(person),
					String.valueOf(person.getID()), String.valueOf(person.getAttribute(IPerson.USERNAME)), fname,
					String.valueOf(person.getAttribute(this.personAttrCurrentUai)),
					String.valueOf(person.getAttribute(EscoStatsLogger.PERSON_ATTR_KEY_SESSIONID)), userAgent);
		} catch (PortalSecurityException e) {
			LOG.error("Unable to find IPerson object");
		}
	}

	/**
	 * Process an event which was already filtered as an esco stats event.
	 *
	 * @param event The event to process
	 * @return true if the event was processed.
	 */
	protected boolean processEscoStatEvent(final ApplicationEvent event) {
		if (event != null) {
			if (PortalEvent.class.isAssignableFrom(event.getClass())) {
				final PortalEvent portalEvent = (PortalEvent) event;
				final IPerson person = portalEvent.getPerson();
				if (this.isPersonToIgnored(person)) {
					// If no object person found => skip the event
					return false;
				}
			}

			// Process the event depending on its type.
			if (LoginEvent.class.isAssignableFrom(event.getClass())) {
				return this.processLoginEvent((LoginEvent) event);
			} else if (LogoutEvent.class.isAssignableFrom(event.getClass())) {
				return this.processLogoutEvent((LogoutEvent) event);
			} else if (PortalRenderEvent.class.isAssignableFrom(event.getClass())) {
				return this.processPortalRenderEvent((PortalRenderEvent) event);
			} else if (PortletRenderExecutionEvent.class.isAssignableFrom(event.getClass())) {
				return this.processPortletRenderExecutionEvent((PortletRenderExecutionEvent) event);
			} else if (HttpSessionCreatedEvent.class.isAssignableFrom(event.getClass())) {
				// This event is not exploitable cause it is launched too early in the portal process
				// We fake it when we receive a LoginEvent
				//return this.processHttpSessionCreatedEvent((HttpSessionCreatedEvent) event);
			} else if (HttpSessionDestroyedEvent.class.isAssignableFrom(event.getClass())) {
				return this.processHttpSessionDestroyedEvent((HttpSessionDestroyedEvent) event);
			} else {
				LOG.error("Event of type {} cannot be processed !", event.getClass().getName());
			}
		}

		return false;
	}

	/**
	 * Process a LoginEvent : log a stats line.
	 *
	 * @param event The event to process
	 * @return true if the event was logged
	 */
	protected boolean processLoginEvent(final LoginEvent event) {
		// On LoginEvent we fake a HttpSessionCreatedEvent.
		this.processHttpSessionCreatedEvent(event);

		EscoStatsLogger.LOG.debug("Processing LoginEvent...");

		final IPerson person = event.getPerson();
		final int serial = this.retrieveSerial(person);

		// Immediatily log the event
		this.logStatsDatas(EscoStatsLogger.TAG_LOGIN, this.getTypePerson(person), String.valueOf(person.getID()),
				(String) person.getAttribute(IPerson.USERNAME),
				(String) person.getAttribute(this.personAttrCurrentUai), String.valueOf(serial));

		EscoStatsLogger.LOG.info(String.format("Login of user uid='%s' with sessionId='%s' and IP='%s'",
				(String) person.getAttribute(IPerson.USERNAME), event.getPortalRequest().getSession().getId(), event
						.getPortalRequest().getRemoteAddr()));
		return true;
	}

	/**
	 * Process a LogoutEvent : log a stats line.
	 *
	 * @param event The event to process
	 * @return true if the event was logged
	 */
	protected boolean processLogoutEvent(final LogoutEvent event) {
		EscoStatsLogger.LOG.debug("Processing LogoutEvent...");

		final IPerson person = event.getPerson();
		final int serial = this.retrieveSerial(person);

		// Immediatily log the event
		this.logStatsDatas(EscoStatsLogger.TAG_LOGOUT, this.getTypePerson(person), String.valueOf(person.getID()),
				(String) person.getAttribute(IPerson.USERNAME),
				(String) person.getAttribute(this.personAttrCurrentUai), String.valueOf(serial));

		return true;
	}

	/**
	 * Process a PortalRenderEvent : log a stats line OR
	 * record a stat line in a NodeStatsRecorder if this.printOnLogout is true.
	 *
	 * @param event The event to process
	 * @return true if the event was logged or recorded
	 */
	protected boolean processPortalRenderEvent(final PortalRenderEvent event) {
		EscoStatsLogger.LOG.debug("Processing PortalRenderEvent...");

		//this.statsLogger.debug("Portal parameters: [{}].", event.getParameters());

		final IPerson person = event.getPerson();

		String pathInfo = event.getRequestPathInfo();

		if (pathInfo != null) {
			Matcher m = fnamePattern.matcher(pathInfo);

			if (m.matches()) {
				final String fname = m.group(1);
				if (fname != null && !"".equals(fname)) {
					return this.processRenderEvent(person, fname, event);
				}
			}
		}
		if (EscoStatsLogger.LOG.isDebugEnabled()) {
			if (!"/accueil/normal/render.uP".equals(pathInfo) && !"/v4-3/dlm/layout.json".equals(pathInfo)) {
				EscoStatsLogger.LOG.debug("PortalRenderEvent: fnamePattern not match  :" + pathInfo);
			}
		}
		return false;
	}

	/**
	 * Process a PortletRenderExecutionEvent : log a stats line OR
	 * record a stat line in a NodeStatsRecorder if this.printOnLogout is true.
	 *
	 * @param event The event to process
	 * @return true if the event was correctly processed
	 */
	protected boolean processPortletRenderExecutionEvent(final PortletRenderExecutionEvent event) {
		EscoStatsLogger.LOG.debug("Processing PortletRenderExecutionEvent...");

		this.statsLogger.debug("Portal parameters: [{}].", event.getParameters());

		final IPerson person = event.getPerson();

		final String fname = event.getFname();

		return this.processRenderEvent(person, fname, event);
	}

	/**
	 * Process any render event : may filter the fname and the update the corresponding
	 * NodeStatsRecorder or log the event immediatly.
	 *
	 * @param person The user details
	 * @param fname The fname to log.
	 * @param event The event processed
	 * @return true if the event was correctly processed
	 */
	protected boolean processRenderEvent(final IPerson person, final String fname, final ApplicationEvent event) {
		final int serial = this.retrieveSerial(person);

		this.statsLogger.debug("Resolved fname: [{}].", fname);

		// On vérifie que le fname doit etre trace
		if (StringUtils.hasText(fname) && this.filteredFnames.isEmpty() || !this.filteredFnames.contains(fname)) {
			if (this.printOnLogout) {
				// Update stats recorder to log at logout
				this.updateNodeStatsRecorder(person, fname, event);
			} else {
				// Log immediatly
				this.logStatsDatas(EscoStatsLogger.TAG_CHANNEL_RENDERED, this.getTypePerson(person),
						String.valueOf(person.getID()), (String) person.getAttribute(IPerson.USERNAME), fname,
						(String) person.getAttribute(this.personAttrCurrentUai), String.valueOf(serial));
			}
			return true;
		}
		return false;
	}

	/**
	 * Process a HttpSessionCreatedEvent : log a stats line
	 * and init some stats recording feature for the session life.
	 *
	 * @param event The event to process
	 * @return true if the event was logged
	 */
	protected boolean processHttpSessionCreatedEvent(final LoginEvent event) {
		EscoStatsLogger.LOG.debug("Processing HttpSessionCreatedEvent...");

		final IPerson person = event.getPerson();
		final HttpServletRequest request = event.getPortalRequest();
		final String userAgent = request.getHeader(EscoStatsLogger.USERAGENT_HTTP_HEADER);
		final int serial = genSerial(person);
		if (person != null) {
			this.logStatsDatas(EscoStatsLogger.TAG_SESSION_CREATED, this.getTypePerson(person),
					String.valueOf(person.getID()), (String) person.getAttribute(IPerson.USERNAME),
					(String) person.getAttribute(this.personAttrCurrentUai), String.valueOf(serial), userAgent);
			return true;
		}

		return false;
	}

	/**
	 * Process a HttpSessionDestroyedEvent : log a stats line
	 * and may log the node rendered stats if this.printOnLogout is true.
	 *
	 * @param event The event to process
	 * @return true if the event was logged
	 */
	protected boolean processHttpSessionDestroyedEvent(final HttpSessionDestroyedEvent event) {
		EscoStatsLogger.LOG.debug("Processing HttpSessionDestroyedEvent...");

		final HttpSession session = event.getSession();
		final IPerson person = Helper.retrievePerson(session);
		if (!this.isPersonToIgnored(person)) {
			final int serial = this.retrieveSerial(person);

			this.logStatsDatas(EscoStatsLogger.TAG_SESSION_DESTROYED, this.getTypePerson(person),
					String.valueOf(person.getID()), (String) person.getAttribute(IPerson.USERNAME),
					(String) person.getAttribute(this.personAttrCurrentUai), String.valueOf(serial));

			if (this.printOnLogout) {
				this.logRenderEventsRecorded(person);
			}

			// Empty IPerson object
			person.setAttribute(EscoStatsLogger.PERSON_ATTR_KEY_NODESTATS, null);
			person.setAttribute(EscoStatsLogger.PERSON_ATTR_KEY_SESSIONID, null);

			return true;
		}

		return false;
	}

	/**
	 * Log all NSR recorded in the IPerson object.
	 *
	 * @param person The user details
	 */
	protected void logRenderEventsRecorded(final IPerson person) {
		final int serial = this.retrieveSerial(person);
		final StatsRecorder sr = retrieveStatsRecorder(person);
		if (sr != null) {
			for (NodeStatsRecorder nsr : sr.getRecords()) {
				// Log all NSR
				this.logStatsDatas(EscoStatsLogger.TAG_CHANNEL_RENDERED, String.valueOf(this.getTypePerson(person)),
						String.valueOf(person.getID()), (String) person.getAttribute(IPerson.USERNAME), nsr.getFname(),
						String.valueOf(nsr.getCountSmoothedUse()), String.valueOf(nsr.getCountRendered()),
						(String) person.getAttribute(this.personAttrCurrentUai), String.valueOf(serial));
			}
		}
	}

	/**
	 * Update the NodeStatsRecorder stored in the IPerson object.
	 *
	 * @param person The user details
	 * @param fname
	 * @param event The event to process
	 */
	protected void updateNodeStatsRecorder(final IPerson person, final String fname, final ApplicationEvent event) {
		// The rendered node is the same than the last rendered node
		final StatsRecorder sr = retrieveStatsRecorder(person);
		sr.recordRenderedEvent(fname, event.getTimestamp());
	}

	/**
	 * Retrieve the Stats recorder from IPerson object.
	 *
	 * @param person The user details
	 * @return The StatsRecorder depending on the User.
	 */
	protected StatsRecorder retrieveStatsRecorder(final IPerson person) {
		StatsRecorder sr = (StatsRecorder) person.getAttribute(EscoStatsLogger.PERSON_ATTR_KEY_NODESTATS);

		if (sr == null) {
			sr = new StatsRecorder(this.recentPeriod);
			person.setAttribute(EscoStatsLogger.PERSON_ATTR_KEY_NODESTATS, sr);
		}

		return sr;
	}

	/**
	 * Retrieve the Layout Node fname if one can be found.
	 *
	 * @param nodeDescription
	 * @return The fname from the node Description.
	 */
	protected String retrieveLayoutNodeFname(final IUserLayoutNodeDescription nodeDescription) {
		final String fname;

		if (nodeDescription != null) {
			if (IUserLayoutChannelDescription.class.isAssignableFrom(nodeDescription.getClass())) {
				final IUserLayoutChannelDescription ulcd = (IUserLayoutChannelDescription) nodeDescription;
				fname = ulcd.getFunctionalName();
			} else {
				fname = "-folder-" + nodeDescription.getName();
			}
		} else {
			fname = null;
		}

		return fname;
	}

	/**
	 * Retourne le type d'utilisateur.
	 *
	 * @param person The user details
	 * @return type Le type d'utilisateur
	 */
	protected String getTypePerson(IPerson person) {
		String type = null;
		if (person == null) {
			return EscoStatsLogger.UNKNOWN_TYPE;
		}
		type = (String) person.getAttribute(this.personAttrUserType);
		if ((type == null) || (type.length() == 0)) {
			return EscoStatsLogger.UNKNOWN_TYPE;
		}
		return type;
	}

	/**
	 * Log some stats datas.
	 *
	 * @param datas can be null or empty.
	 */
	protected void logStatsDatas(final String... datas) {
		if (datas != null && datas.length > 0) {
			final StringBuilder sb = new StringBuilder(128);
			sb.append(datas[0]);
			for (int k = 1; k < datas.length; k++) {
				final String data = datas[k];
				if (StringUtils.hasText(data)) {
					sb.append(EscoStatsLogger.STATS_SEPARATOR);
					sb.append(datas[k]);
				}
			}

			this.statsLogger.info(sb.toString());
		}
	}

	/**
	 * Gen a serial number and place it in the IPerson attributes.
	 *
	 * @param person The user details
	 * @return the generated serial
	 */
	protected int retrieveSerial(IPerson person) {
		return (Integer) person.getAttribute(EscoStatsLogger.PERSON_ATTR_KEY_SESSIONID);
	}

	/**
	 * Gen a serial number and place it in the IPerson attributes.
	 *
	 * @param person The user details
	 * @return the generated serial
	 */
	protected int genSerial(IPerson person) {
		final int serial;
		if (person != null) {
			serial = genSerial();
			person.setAttribute(EscoStatsLogger.PERSON_ATTR_KEY_SESSIONID, new Integer(serial));
		} else {
			serial = -1;
		}
		return serial;
	}

	/**
	 * Test if the person must be ignored.
	 *
	 * @param person The user details
	 * @return true if it must be ignored
	 */
	protected boolean isPersonToIgnored(final IPerson person) {
		return person == null || this.eliminateGuest && person.isGuest();
	}

	/**
	 * Calcule le prochain identifiant d'utilisateur
	 * @return serial L'identifiant
	 */
	protected synchronized int genSerial() {
		return (++this.serial) % Integer.MAX_VALUE;
	}

	public String getPersonAttrCurrentUai() {
		return personAttrCurrentUai;
	}

	public void setPersonAttrCurrentUai(String personAttrCurrentUai) {
		this.personAttrCurrentUai = personAttrCurrentUai;
	}

	public String getPersonAttrUserType() {
		return personAttrUserType;
	}

	public void setPersonAttrUserType(String personAttrUserType) {
		this.personAttrUserType = personAttrUserType;
	}

	public boolean isPrintOnLogout() {
		return printOnLogout;
	}

	public void setPrintOnLogout(boolean printOnLogout) {
		this.printOnLogout = printOnLogout;
	}

	public boolean isEliminateGuest() {
		return eliminateGuest;
	}

	public void setEliminateGuest(boolean eliminateGuest) {
		this.eliminateGuest = eliminateGuest;
	}

	public Set<String> getFilteredFnames() {
		return filteredFnames;
	}

	public void setFilteredFnames(Set<String> filteredFnames) {
		this.filteredFnames = filteredFnames;
	}

	public Logger getStatsLogger() {
		return statsLogger;
	}

	public void setStatsLogger(Logger statsLogger) {
		this.statsLogger = statsLogger;
	}

	public long getRecentPeriod() {
		return recentPeriod;
	}

	public void setRecentPeriod(long recentPeriod) {
		this.recentPeriod = recentPeriod;
	}

	public IPersonManager getPersonManager() {
		return personManager;
	}

	public void setPersonManager(IPersonManager personManager) {
		this.personManager = personManager;
	}

}
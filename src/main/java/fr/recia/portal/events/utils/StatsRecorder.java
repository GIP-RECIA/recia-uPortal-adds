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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.joda.time.Instant;

/**
 * @author GIP RECIA 2013
 *
 */
public class StatsRecorder implements Serializable {

	/** Stats recorded during the session. */
	private Map<String, NodeStatsRecorder> stats = new TreeMap<String, NodeStatsRecorder>();

	/** Nodes rendered for the last request. */
	private Map<String, Instant> recentlyRendered = new HashMap<String, Instant>(32);

	/** Period during which an event is "recent". By default 1 min. */
	private long recentPeriod = 60000L;

	/** Last action recorded. */
	private long lastRenderingTimestamp = System.currentTimeMillis();

	public StatsRecorder() {
		super();
	}

	/**
	 * Allow to configure the "recent period" param.
	 *
	 * @param recentPeriod period during which an event is considered recent
	 */
	public StatsRecorder(final long recentPeriod) {
		super();
		this.recentPeriod = recentPeriod;
	}

	/**
	 * Record stats for a fname.
	 *
	 * @param fname The fname
	 * @param eventTimestamp The event timestamp
	 */
	public synchronized void recordRenderedEvent(final String fname, final long eventTimestamp) {
		this.lastRenderingTimestamp = eventTimestamp;

		NodeStatsRecorder nsr = this.stats.get(fname);
		if (nsr == null) {
			nsr = new NodeStatsRecorder(fname);
			this.stats.put(fname, nsr);
		}

		nsr.incRendered();
		if (!this.wasRecentlyRendered(fname, eventTimestamp)) {
			nsr.incSmoothedUse();
		}
	}

	public Collection<NodeStatsRecorder> getRecords() {
		return this.stats.values();
	}

	public NodeStatsRecorder get(final String fname) {
		return this.stats.get(fname);
	}

	/**
	 * Test if the fname was already recently rendered by a previous event.
	 *
	 * @param fname The fname
	 * @param eventTimestamp The event timestamp
	 * @return true if recently rendered
	 */
	protected boolean wasRecentlyRendered(final String fname, final long eventTimestamp) {
		boolean test = true;

		final Instant recentEventInstant = this.recentlyRendered.get(fname);
		if (recentEventInstant == null || recentEventInstant.isBefore(eventTimestamp - this.recentPeriod)) {
			// If no previous event was recorded or latter than the recent period
			this.recentlyRendered.put(fname, new Instant(eventTimestamp));
			test = false;
		}

		return test;
	}

	public long getRecentPeriod() {
		return recentPeriod;
	}

	public void setRecentPeriod(long recentPeriod) {
		this.recentPeriod = recentPeriod;
	}

	public long getLastRenderingTimestamp() {
		return lastRenderingTimestamp;
	}

}
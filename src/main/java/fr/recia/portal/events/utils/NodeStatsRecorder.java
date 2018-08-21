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

package fr.recia.portal.events.utils;

import java.io.Serializable;

/**
 * Object which record stats about rendered nodes in the portal.
 *
 * @author GIP RECIA 2013
 *
 */
public class NodeStatsRecorder implements Serializable {

    private String fname = null;
    private int countSmoothedUse = 0;
    private int countRendered = 0;

	protected NodeStatsRecorder(final String fname) {
		super();
		this.fname = fname;
	}

	public String getFname() {
		return this.fname;
	}

    public int getCountRendered() {
        return this.countRendered;
    }

    public void incRendered() {
	this.countRendered ++;
    }

    public int getCountSmoothedUse() {
        return this.countSmoothedUse;
    }

    public void incSmoothedUse() {
	this.countSmoothedUse ++;
    }
}

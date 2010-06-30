/**
 * $RCSfile$
 * $Revision: 18654 $
 * $Date: 2005-03-24 09:55:04 -0800 (Thu, 24 Mar 2005) $
 *
 * Copyright (C) 1999-2003 CoolServlets, Inc. All rights reserved.
 *
 * This software is the proprietary information of CoolServlets, Inc.
 * Use is subject to license terms.
 */

package org.jivesoftware.xmpp.workgroup;


import org.xmpp.packet.JID;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <p>Simple stand-alone implementation of an in-memory agent session list.</p>
 *
 * @author Derek DeMoro
 */
public class AgentSessionList {

    private Map<AgentSession, String> sessionList = new ConcurrentHashMap<AgentSession, String>();
    private Queue<AgentSessionListener> listenerList = new ConcurrentLinkedQueue<AgentSessionListener>();

    public AgentSessionList() {
    }

    public Collection<AgentSession> getAgentSessions() {
        return Collections.unmodifiableCollection(sessionList.keySet());
    }

    public void addAgentSessionListener(AgentSessionListener listener) {
        listenerList.add(listener);
    }

    public void removeAgentSessionListener(AgentSessionListener listener) {
        listenerList.remove(listener);
    }

    /**
     * Sends two presence packets to each connected agent to the specified queue with the detailed
     * and summary status of the specified queue.
     *
     * @param queue the queue whose status is going to be broadcasted
     */
    public void broadcastQueueStatus(RequestQueue queue) {
        for (AgentSession session : sessionList.keySet()) {
            queue.sendStatus(session.getJID());
            queue.sendDetailedStatus(session.getJID());
        }
    }

    /**
     * Returns true if there are any available agents to chat.
     *
     * @return true if there are any available agents to chat, otherwise false.
     */
    public boolean containsAvailableAgents() {
        boolean dispatchable = false;
        for (AgentSession agentSession : sessionList.keySet()) {
            if (!dispatchable) {
                dispatchable = agentSession.isAvailableToChat();
            }
        }
        return dispatchable;
    }

    /**
     * <p>Obtain the agent session by address.</p>
     * <p>The current implementation is SLOW, but we
     * expect it to only be used by the admin interface so
     * overhead is not a concern. If this is used for the
     * runtime behavior of the workgroup itself, the
     * implementation MUST be changed.</p>
     *
     * @param address The address to be located
     * @return The agent session found
     * @throws AgentNotFoundException If the agent is not in the list
     */
    public AgentSession getAgentSession(JID address) throws AgentNotFoundException {
        AgentSession session = null;
        for (AgentSession agentSession : sessionList.keySet()) {
            if (agentSession.getJID().equals(address)) {
                session = agentSession;
                break;
            }
        }

        // If there is no session found, throw an AgentNotFoundException
        if (session == null) {
            throw new AgentNotFoundException(address.toString());
        }
        return session;
    }

    public void addAgentSession(AgentSession agentSession) {
        boolean added = sessionList.put(agentSession, "") == null;

        if (added) {
            for (AgentSessionListener listener : listenerList) {
                listener.notifySessionAdded(agentSession);
            }
        }
    }

    public void removeAgentSession(AgentSession agentSession) {
        boolean removed = sessionList.remove(agentSession) != null;
        if (removed) {
            for (AgentSessionListener listener : listenerList) {
                listener.notifySessionRemoved(agentSession);
            }
        }
    }

    public int getAgentSessionCount() {
        return sessionList.size();
    }

    /**
     * Returns all agents available to chat in this list.
     *
     * @return agents available to chat.
     */
    public int getAvailableAgentCount() {
        int count = 0;
        for (AgentSession agentSession : sessionList.keySet()) {
            if (agentSession.isAvailableToChat()) {
                count++;
            }
        }

        return count;
    }

    /**
     * Returns true if no agent session is found in this list.
     *
     * @return true if no agent session is found in this list.
     */
    public boolean isEmpty() {
        return sessionList.isEmpty();
    }
}
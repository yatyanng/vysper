/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.vysper.xmpp.server.components;

import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.protocol.NamespaceHandlerDictionary;
import org.apache.vysper.xmpp.protocol.ProtocolException;
import org.apache.vysper.xmpp.protocol.ResponseStanzaContainer;
import org.apache.vysper.xmpp.protocol.SessionStateHolder;
import org.apache.vysper.xmpp.protocol.StanzaHandler;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ComponentStanzaProcessor implements StanzaProcessor {

	private static final Logger logger = LoggerFactory.getLogger(ComponentStanzaProcessor.class);
	
    protected ServerRuntimeContext serverRuntimeContext;

    protected ComponentStanzaHandlerLookup componentStanzaHandlerLookup = new ComponentStanzaHandlerLookup();

    public ComponentStanzaProcessor(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    public void addHandler(StanzaHandler stanzaHandler) {
        componentStanzaHandlerLookup.addDefaultHandler(stanzaHandler);
    }

    public void addDictionary(NamespaceHandlerDictionary namespaceHandlerDictionary) {
        componentStanzaHandlerLookup.addDictionary(namespaceHandlerDictionary);
    }

    public void processStanza(ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, Stanza stanza,
            SessionStateHolder sessionStateHolder) {
        if (stanza == null)
            throw new RuntimeException("cannot process NULL stanzas");

        XMPPCoreStanza xmppStanza = XMPPCoreStanza.getWrapper(stanza);
        if (xmppStanza == null)
            throw new RuntimeException("cannot process only: IQ, message or presence");

        logger.debug("Getting Component Handler for stanza {}", xmppStanza);
        StanzaHandler stanzaHandler = componentStanzaHandlerLookup.getHandler(xmppStanza);

        if (stanzaHandler == null) {
            unhandledStanza(stanza);
            return;
        }

        ResponseStanzaContainer responseStanzaContainer = null;
        try {
            responseStanzaContainer = stanzaHandler.execute(stanza, serverRuntimeContext, false, sessionContext,
                    sessionStateHolder);
        } catch (ProtocolException e) {
        	//To change body of catch statement use File | Settings | File Templates.
        	logger.error("[processStanza] error while processing {}", stanza, e); 
        }

        if (responseStanzaContainer != null && responseStanzaContainer.getResponseStanza() != null) {
            Stanza responseStanza = responseStanzaContainer.getResponseStanza();
            try {
                IgnoreFailureStrategy failureStrategy = new IgnoreFailureStrategy(); // TODO call back module
                serverRuntimeContext.getStanzaRelay().relay(responseStanza.getTo(), responseStanza, failureStrategy);
            } catch (DeliveryException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void unhandledStanza(Stanza stanza) {
        throw new RuntimeException("no handler for stanza");
    }

    public void processTLSEstablished(SessionContext sessionContext, SessionStateHolder sessionStateHolder) {
        throw new RuntimeException("should not be called for components, which only acts as an established session");
    }
}

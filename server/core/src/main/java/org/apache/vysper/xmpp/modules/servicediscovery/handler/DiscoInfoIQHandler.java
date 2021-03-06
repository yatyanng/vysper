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
package org.apache.vysper.xmpp.modules.servicediscovery.handler;

import java.util.List;

import org.apache.vysper.compliance.SpecCompliance;
import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.delivery.StanzaRelay;
import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.ReturnErrorToSenderFailureStrategy;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceCollector;
import org.apache.vysper.xmpp.modules.servicediscovery.collection.ServiceDiscoveryRequestListenerRegistry;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoElement;
import org.apache.vysper.xmpp.modules.servicediscovery.management.InfoRequest;
import org.apache.vysper.xmpp.modules.servicediscovery.management.ServiceDiscoveryRequestException;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.components.ExternalComponentStanzaProcessor;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * handles IQ info queries
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliance(compliant = {
		@SpecCompliant(spec = "xep-0030", status = SpecCompliant.ComplianceStatus.IN_PROGRESS, coverage = SpecCompliant.ComplianceCoverage.PARTIAL, comment = "handles disco info queries"),
		@SpecCompliant(spec = "xep-0128", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE, comment = "allows InfoDataForm elements") })
public class DiscoInfoIQHandler extends DefaultIQHandler {

	static final Logger logger = LoggerFactory.getLogger(DiscoInfoIQHandler.class);

	@Override
	protected boolean verifyNamespace(Stanza stanza) {
		return verifyInnerNamespace(stanza, NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO);
	}

	@Override
	protected boolean verifyInnerElement(Stanza stanza) {
		return verifyInnerElementWorker(stanza, "query");
	}

	protected Stanza handleResult(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext) {
		Entity to = stanza.getTo();
		try {
			if (to != null) {
				// See handleSet of BindIQHandler
				SessionContext userSessionContext = serverRuntimeContext.getResourceRegistry()
						.getSessionContext(to.getResource());
				logger.debug("In stanza {}, target resource is {} and user session context is {}", stanza.getID(),
						to.getResource(), userSessionContext);
				if (userSessionContext != null) {
					userSessionContext.getResponseWriter().write(stanza);
				}
			} else {
				logger.debug("In stanza {}, target is unknown {}", stanza.getID(), to);
			}
			return null;
		} catch (Exception e) {
			logger.error("discover-info response error", e);
			StanzaErrorCondition stanzaErrorCondition = StanzaErrorCondition.INTERNAL_SERVER_ERROR;
			return ServerErrorResponses.getStanzaError(stanzaErrorCondition, stanza, StanzaErrorType.CANCEL,
					"discover-info response error", getErrorLanguage(serverRuntimeContext, sessionContext), null);
		}
    }
	
	@Override
	protected Stanza handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
			SessionContext sessionContext) {
		ServiceCollector serviceCollector = null;

		// TODO if the target entity does not exist, return error/cancel/item-not-found
		// TODO more strictly, server can also return error/cancel/service-unavailable

		// retrieve the service collector
		try {
			serviceCollector = (ServiceCollector) serverRuntimeContext.getServerRuntimeContextService(
					ServiceDiscoveryRequestListenerRegistry.SERVICE_DISCOVERY_REQUEST_LISTENER_REGISTRY);
		} catch (Exception e) {
			logger.error("error retrieving ServiceCollector service {}", e);
			serviceCollector = null;
		}

		if (serviceCollector == null) {
			return ServerErrorResponses.getStanzaError(StanzaErrorCondition.INTERNAL_SERVER_ERROR, stanza,
					StanzaErrorType.CANCEL, "cannot retrieve IQ-get-info result from internal components",
					getErrorLanguage(serverRuntimeContext, sessionContext), null);
		}

		// if "vysper.org" is the server entity, 'to' can either be "vysper.org",
		// "node@vysper.org", "service.vysper.org".
		Entity to = stanza.getTo();
		boolean isServerInfoRequest = false;
		boolean isComponentInfoRequest = false;
		Entity serviceEntity = serverRuntimeContext.getServerEntity();
		if (to == null || to.equals(serviceEntity)) {
			isServerInfoRequest = true; // this can only be meant to query the server
		} else if (serverRuntimeContext.getComponentStanzaProcessor(to) != null) {
			isComponentInfoRequest = true; // this is a query to a component
		} else if (!to.isNodeSet()) {
			isServerInfoRequest = serviceEntity.equals(to);
			if (!isServerInfoRequest) {
				return ServerErrorResponses.getStanzaError(StanzaErrorCondition.ITEM_NOT_FOUND, stanza,
						StanzaErrorType.CANCEL,
						"server does not handle info query requests for " + to.getFullQualifiedName(),
						getErrorLanguage(serverRuntimeContext, sessionContext), null);
			}
		}

		XMLElement queryElement = stanza.getFirstInnerElement();
		String node = queryElement != null ? queryElement.getAttributeValue("node") : null;

		logger.debug("server info request: {}, component info request: {}", isServerInfoRequest,
				isComponentInfoRequest);
		// collect all the info response elements
		List<InfoElement> elements = null;
		try {
			Entity from = stanza.getFrom();
			if (from == null)
				from = sessionContext.getInitiatingEntity();
			if (isServerInfoRequest) {
				elements = serviceCollector.processServerInfoRequest(new InfoRequest(from, to, node, stanza.getID()));
			} else if (isComponentInfoRequest) {
				boolean externalComponent = serverRuntimeContext
						.getComponentStanzaProcessor(to) instanceof ExternalComponentStanzaProcessor;
				logger.debug("stanza {} will be relayed to an external component: {}", stanza, externalComponent);
				if (externalComponent) {
					if (from == null || !from.isResourceSet()) {
						from = new EntityImpl(sessionContext.getInitiatingEntity(),
								serverRuntimeContext.getResourceRegistry().getUniqueResourceForSession(sessionContext));
					}
					Stanza forwardedStanza = StanzaBuilder.createForward(stanza, from, to).build();
					StanzaRelay stanzaRelay = serverRuntimeContext.getStanzaRelay();
					stanzaRelay.relay(stanza.getTo(), forwardedStanza, new ReturnErrorToSenderFailureStrategy(stanzaRelay));
					return null;
				} else {
					InfoRequest infoRequest = new InfoRequest(from, to, node, stanza.getID());
					elements = serviceCollector.processComponentInfoRequest(infoRequest);
				}
			} else {
				elements = serviceCollector.processInfoRequest(new InfoRequest(from, to, node, stanza.getID()));
			}
		} catch (ServiceDiscoveryRequestException e) {
			logger.error("discover-info error", e);
			// the request yields an error
			StanzaErrorCondition stanzaErrorCondition = e.getErrorCondition();
			if (stanzaErrorCondition == null)
				stanzaErrorCondition = StanzaErrorCondition.INTERNAL_SERVER_ERROR;
			return ServerErrorResponses.getStanzaError(stanzaErrorCondition, stanza, StanzaErrorType.CANCEL,
					"disco info request failed.", getErrorLanguage(serverRuntimeContext, sessionContext), null);
		} catch (DeliveryException e) {
			logger.error("discover-info error", e);
			StanzaErrorCondition stanzaErrorCondition = StanzaErrorCondition.INTERNAL_SERVER_ERROR;
			return ServerErrorResponses.getStanzaError(stanzaErrorCondition, stanza, StanzaErrorType.CANCEL,
					"disco info request failed.", getErrorLanguage(serverRuntimeContext, sessionContext), null);
		}

		// TODO check that elementSet contains at least one identity element and on
		// feature element!

		// render the stanza with information collected
		StanzaBuilder stanzaBuilder = StanzaBuilder
				.createIQStanza(to, stanza.getFrom(), IQStanzaType.RESULT, stanza.getID())
				.startInnerElement("query", NamespaceURIs.XEP0030_SERVICE_DISCOVERY_INFO);
		if (node != null) {
			stanzaBuilder.addAttribute("node", node);
		}
		for (InfoElement infoElement : elements) {
			infoElement.insertElement(stanzaBuilder);
		}

		stanzaBuilder.endInnerElement();

		return stanzaBuilder.build();
	}
}

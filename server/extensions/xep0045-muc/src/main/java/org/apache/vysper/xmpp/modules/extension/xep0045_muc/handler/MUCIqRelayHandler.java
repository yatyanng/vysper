package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Occupant;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MUCIqRelayHandler extends DefaultIQHandler {

	private static final Logger logger = LoggerFactory.getLogger(MUCIqRelayHandler.class);

	protected Conference conference;

	public MUCIqRelayHandler(Conference conference) {
		this.conference = conference;
	}

	@Override
	protected boolean verifyNamespace(Stanza stanza) {
		return verifyInnerNamespace(stanza, NamespaceURIs.XEP0166_JINGLE);
	}

	@Override
	protected Stanza handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
			SessionContext sessionContext) {
		try {
			Entity to = stanza.getTo();
			// might be an items request on a room
            Room room = conference.findRoom(to.getBareJID());
            if (room == null) {
            	return createBadRequestError(stanza, serverRuntimeContext, sessionContext, "Room not found");
            }
            // request for an occupant
            Occupant occupant = room.findOccupantByNick(to.getResource());
            // request for occupant, relay
            if (occupant != null) {
            	// See handleSet of BindIQHandler
            	Stanza forwardedStanza = StanzaBuilder.createForward(stanza, stanza.getFrom(), occupant.getJid()).build();
    			SessionContext userSessionContext = serverRuntimeContext.getResourceRegistry()
    					.getSessionContext(to.getResource());
    			logger.debug("In stanza {}, target is {} and user session context is {}", stanza.getID(),
    					occupant.getJid(), userSessionContext);
    			if (userSessionContext != null) {
    				userSessionContext.getResponseWriter().write(forwardedStanza);
    			}
    			return null;
            }
            return createBadRequestError(stanza, serverRuntimeContext, sessionContext, "Occupant not found");
		} catch (Exception e) {
			logger.debug("An error has occurred when getting room info!", e);
			return createBadRequestError(stanza, serverRuntimeContext, sessionContext, "Room info error");
		}
	}

	@Override
	protected Stanza handleResult(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext) {
		Entity to = stanza.getTo();
		try {
			// See handleSet of BindIQHandler
			SessionContext userSessionContext = serverRuntimeContext.getResourceRegistry()
					.getSessionContext(to.getResource());
			logger.debug("In stanza {}, target resource is {} and user session context is {}", stanza.getID(),
					to.getResource(), userSessionContext);
			if (userSessionContext != null) {
				userSessionContext.getResponseWriter().write(stanza);
			}
			return null;
		} catch (Exception e) {
			logger.error("jingle response error", e);
			StanzaErrorCondition stanzaErrorCondition = StanzaErrorCondition.INTERNAL_SERVER_ERROR;
			return ServerErrorResponses.getStanzaError(stanzaErrorCondition, stanza, StanzaErrorType.CANCEL,
					"jingle response error", getErrorLanguage(serverRuntimeContext, sessionContext), null);
		}
    }
	
	private Stanza createBadRequestError(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
			SessionContext sessionContext, String message) {
		return ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza, StanzaErrorType.MODIFY,
				message, getErrorLanguage(serverRuntimeContext, sessionContext), null);
	}
}

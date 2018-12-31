package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MUCIqOwnerHandler  extends DefaultIQHandler {

    private static final Logger logger = LoggerFactory.getLogger(MUCIqOwnerHandler.class);

    protected Conference conference;

    public MUCIqOwnerHandler(Conference conference) {
        this.conference = conference;
    }

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        return verifyInnerNamespace(stanza, NamespaceURIs.XEP0045_MUC_OWNER);
    }

    @Override
    protected Stanza handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        logger.debug("Received MUC owner stanza");

        Room room = conference.findRoom(stanza.getTo());
    	try {
            XMLElement query = stanza.getSingleInnerElementsNamed("query", NamespaceURIs.XEP0045_MUC_OWNER);
            XMLElement submitElement = query.getSingleInnerElementsNamed("x", NamespaceURIs.JABBER_X_DATA);
            if (submitElement != null && submitElement.getAttribute("type").equals(new Attribute("type","submit"))) {
            	logger.debug("Instant Room: {}", room.getJID());
            	synchronized (room) {
            		room.removeRoomType(RoomType.MembersOnly);
                	room.removeRoomType(RoomType.PasswordProtected);
                	room.removeRoomType(RoomType.Persistent);
                	room.removeRoomType(RoomType.Moderated);
                	room.removeRoomType(RoomType.Hidden);
                	room.addRoomType(RoomType.Public);
                	room.addRoomType(RoomType.Unmoderated);
                	room.addRoomType(RoomType.Temporary);
                	room.addRoomType(RoomType.Unsecured);
                	room.addRoomType(RoomType.Open);
            	}
            	return StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT, stanza.getID())
                        .build();
            }
            XMLElement destroyElement = query.getSingleInnerElementsNamed("destroy");
            if (destroyElement != null) {
            	XMLElement reasonElement = destroyElement.getSingleInnerElementsNamed("reason");
            	if (reasonElement != null) {
            		logger.debug("Reason for destroying room {}: {}", room.getJID(), reasonElement.getFirstInnerText());
            	}
            	conference.deleteRoom(room.getJID());
            	logger.debug("Room destroyed: {}", room.getJID());
            	return StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT, stanza.getID())
                        .build();
            }
            return createBadRequestError(stanza, serverRuntimeContext, sessionContext,
                    "Invalid IQ stanza");
    	} catch (XMLSemanticError e) {
            logger.debug("Invalid MUC Owner stanza", e);
            return createBadRequestError(stanza, serverRuntimeContext, sessionContext,
                    "Invalid IQ stanza");
        }

    }
    
    
    
    private Stanza createBadRequestError(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
            SessionContext sessionContext, String message) {
        return ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza,
                StanzaErrorType.MODIFY, message,
                getErrorLanguage(serverRuntimeContext, sessionContext), null);
    }
}

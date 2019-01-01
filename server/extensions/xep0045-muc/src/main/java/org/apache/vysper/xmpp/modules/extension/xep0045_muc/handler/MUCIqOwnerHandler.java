package org.apache.vysper.xmpp.modules.extension.xep0045_muc.handler;

import java.util.Arrays;
import java.util.List;

import org.apache.vysper.xml.fragment.Attribute;
import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLFragment;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xml.fragment.XMLText;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCStanzaBuilder;
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
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MUCIqOwnerHandler extends DefaultIQHandler {

	private static final Logger logger = LoggerFactory.getLogger(MUCIqOwnerHandler.class);

	protected Conference conference;

	public MUCIqOwnerHandler(Conference conference) {
		this.conference = conference;
	}

	@Override
	protected boolean verifyNamespace(Stanza stanza) {
		return verifyInnerNamespace(stanza, NamespaceURIs.XEP0045_MUC_OWNER);
	}

	private XMLFragment createWhois(Room room, String var, String type, String label) {
		XMLFragment value = new XMLElement(null, "value", null, new Attribute[] {},
				new XMLFragment[] { new XMLText(room.getRoomTypes().contains(RoomType.NonAnonymous) ? "anyone" : "moderators") });

		XMLFragment value1 = new XMLElement(null, "value", null, new Attribute[] {},
				new XMLFragment[] { new XMLText("anyone") });
		
		XMLFragment option1 = new XMLElement(null, "option", null,
				new Attribute[] { new Attribute("label", "Anyone") }, new XMLFragment[] { value1 });

		XMLFragment value2 = new XMLElement(null, "value", null, new Attribute[] {},
				new XMLFragment[] { new XMLText("moderators") });
		
		XMLFragment option2 = new XMLElement(null, "option", null,
				new Attribute[] { new Attribute("label", "Moderators only") }, new XMLFragment[] { value2 });
		
		XMLFragment xField = new XMLElement(null, "field", null, new Attribute[] { new Attribute("var", var),
				new Attribute("type", type), new Attribute("label", label) }, new XMLFragment[] { value, option1, option2 });
		return xField;
	}

	private XMLFragment createText(String tag, String value) {
		XMLFragment tagElement = new XMLElement(null, tag, null, new Attribute[] {},
				new XMLFragment[] { new XMLText(value) });
		return tagElement;
	}

	protected Stanza handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
			SessionContext sessionContext) {
		logger.debug("Received MUC owner stanza");
		try {
			Room room = conference.findRoom(stanza.getTo());
			
			XMLFragment title = createText("title", "Configuration for " + room.getJID());
			
			XMLFragment instructions = createText("instructions", "Complete and submit this form to configure the room.");
			
			XMLFragment whois = createWhois(room, "muc#roomconfig_whois", "list-single", "Who May Discover Real JIDs?");
			
			XMLFragment xElement = new XMLElement(NamespaceURIs.JABBER_X_DATA, "x", null,
					new Attribute[] { new Attribute("type", "form") }, new XMLFragment[] { title, instructions, whois });
			
			XMLFragment queryElement = new XMLElement(NamespaceURIs.XEP0045_MUC_OWNER, "query", null, new Attribute[0],
					new XMLFragment[] { xElement });
			
			List<XMLFragment> queryElements = Arrays.asList(queryElement);
			Stanza iqStanza = MUCStanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT,
					stanza.getID(), queryElements).build();

			return iqStanza;
		} catch (Exception e) {
			logger.debug("An error has occurred when getting room info!", e);
			return createBadRequestError(stanza, serverRuntimeContext, sessionContext, "Room info error");
		}
	}

	@Override
	protected Stanza handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
			SessionContext sessionContext) {
		logger.debug("Received MUC owner stanza");
		try {
			Room room = conference.findRoom(stanza.getTo());
			XMLElement queryElement = stanza.getSingleInnerElementsNamed("query", NamespaceURIs.XEP0045_MUC_OWNER);
			XMLElement submitElement = queryElement.getSingleInnerElementsNamed("x", NamespaceURIs.JABBER_X_DATA);
			if (submitElement != null && submitElement.getAttribute("type").equals(new Attribute("type", "submit"))) {
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
				return MUCStanzaBuilder
						.createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT, stanza.getID(), null)
						.build();
			}
			XMLElement destroyElement = queryElement.getSingleInnerElementsNamed("destroy");
			if (destroyElement != null) {
				XMLElement reasonElement = destroyElement.getSingleInnerElementsNamed("reason");
				if (reasonElement != null) {
					logger.debug("Reason for destroying room {}: {}", room.getJID(), reasonElement.getFirstInnerText());
				}
				conference.deleteRoom(room.getJID());
				logger.debug("Room destroyed: {}", room.getJID());
				return MUCStanzaBuilder
						.createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT, stanza.getID(), null)
						.build();
			}
			return createBadRequestError(stanza, serverRuntimeContext, sessionContext, "Invalid IQ stanza");
		} catch (XMLSemanticError e) {
			logger.debug("Invalid MUC Owner stanza", e);
			return createBadRequestError(stanza, serverRuntimeContext, sessionContext, "Invalid IQ stanza");
		}
	}

	private Stanza createBadRequestError(IQStanza stanza, ServerRuntimeContext serverRuntimeContext,
			SessionContext sessionContext, String message) {
		return ServerErrorResponses.getStanzaError(StanzaErrorCondition.BAD_REQUEST, stanza, StanzaErrorType.MODIFY,
				message, getErrorLanguage(serverRuntimeContext, sessionContext), null);
	}
}

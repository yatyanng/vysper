package org.apache.vysper.xmpp.stanza;


public class StreamEndStanza extends Stanza {

	public StreamEndStanza() {
		super("stream", "stream", false, true);
	}
}

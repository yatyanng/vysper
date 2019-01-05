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
package org.apache.vysper.xmpp.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * basic facility to collect and query a set of namespace-based handlers
 */
public abstract class AbstractStanzaHandlerLookup {

	private static final Logger logger = LoggerFactory.getLogger(AbstractStanzaHandlerLookup.class);

	protected List<HandlerDictionary> namespaceDictionaries = new ArrayList<HandlerDictionary>();

	public void addDictionary(HandlerDictionary namespaceHandlerDictionary) {
		namespaceDictionaries.add(namespaceHandlerDictionary);
	}

	public abstract StanzaHandler getHandler(Stanza stanza);

	/**
	 * tries to find the handler by trying 1. value of xmlElement's XMLNS attribute,
	 * if unique 2. xmlElements namespace, if the element name has a namespace
	 * prefix
	 */
	protected StanzaHandler getHandlerForElement(Stanza stanza, XMLElement xmlElement) {
		List<StanzaHandler> matchedHandlers = namespaceDictionaries.stream()
				.map(dictionary -> dictionary.get(stanza)).filter(Objects::nonNull).collect(Collectors.toList());
		logger.debug("For stanza {}, the matched handler list is {}", stanza, matchedHandlers);
		return matchedHandlers.stream().findFirst().orElse(null);
	}
}

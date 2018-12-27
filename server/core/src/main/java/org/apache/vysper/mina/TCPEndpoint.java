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
package org.apache.vysper.mina;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.vysper.mina.codec.XMPPProtocolCodecFactory;
import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;

/**
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class TCPEndpoint implements Endpoint {

    private ServerRuntimeContext serverRuntimeContext;

    private int port = 5222;

    private SocketAcceptor acceptor;

    private DefaultIoFilterChainBuilder filterChainBuilder;

    public DefaultIoFilterChainBuilder getFilterChainBuilder() {
        return filterChainBuilder;
    }

    public void setServerRuntimeContext(ServerRuntimeContext serverRuntimeContext) {
        this.serverRuntimeContext = serverRuntimeContext;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();

        DefaultIoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();
        //filterChainBuilder.addLast("executorFilter", new OrderedThreadPoolExecutor());
        filterChainBuilder.addLast("xmppCodec", new ProtocolCodecFilter(new XMPPProtocolCodecFactory()));
        filterChainBuilder.addLast("loggingFilter", new StanzaLoggingFilter());
        acceptor.setFilterChainBuilder(filterChainBuilder);

        XmppIoHandlerAdapter adapter = new XmppIoHandlerAdapter();
        adapter.setServerRuntimeContext(serverRuntimeContext);
        acceptor.setHandler(adapter);

        acceptor.setReuseAddress(true);
        acceptor.bind(new InetSocketAddress(port));

        this.acceptor = acceptor;
    }

    public void stop() {
        acceptor.unbind();
        acceptor.dispose();
    }
}

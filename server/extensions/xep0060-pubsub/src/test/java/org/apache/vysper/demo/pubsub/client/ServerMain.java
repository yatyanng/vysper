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
package org.apache.vysper.demo.pubsub.client;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.vysper.mina.C2SEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.modules.extension.xep0049_privatedata.PrivateDataModule;
import org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp.VcardTempModule;
import org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PublishSubscribeModule;
import org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule;
import org.apache.vysper.xmpp.modules.extension.xep0119_xmppping.XmppPingModule;
import org.apache.vysper.xmpp.modules.extension.xep0202_entity_time.EntityTimeModule;
import org.apache.vysper.xmpp.server.XMPPServer;

/**
 * starts the server as a standalone application
 *
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class ServerMain {

    /**
     * boots the server as a standalone application
     * 
     * adding a module from the command line:
     * using a runtime property, one or more modules can be specified, like this:
     * -Dvysper.add.module=org.apache.vysper.xmpp.modules.extension.xep0060_pubsub.PublishSubscribeModule,... more ...
     * 
     * @param args
     */
    public static void main(String[] args) throws AccountCreationException, EntityFormatException, FileNotFoundException {

        // choose the storage you want to use
        //StorageProviderRegistry providerRegistry = new JcrStorageProviderRegistry();
        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();

        final AccountManagement accountManagement = (AccountManagement) providerRegistry
                .retrieve(AccountManagement.class);

        Entity user1 = EntityImpl.parse("user1@vysper.org");
        if (!accountManagement.verifyAccountExists(user1)) {
            accountManagement.addUser(user1, "password1");
        }
        Entity user2 = EntityImpl.parse("user2@vysper.org");
        if (!accountManagement.verifyAccountExists(user2)) {
            accountManagement.addUser(user2, "password1");
        }
        Entity user3 = EntityImpl.parse("user3@vysper.org");
        if (!accountManagement.verifyAccountExists(user3)) {
            accountManagement.addUser(user3, "password1");
        }

        XMPPServer server = new XMPPServer("vysper.org");
        server.addEndpoint(new C2SEndpoint());
        //server.addEndpoint(new StanzaSessionFactory());
        server.setStorageProviderRegistry(providerRegistry);

        server.setTLSCertificateInfo(new File("../../core/src/main/config/bogus_mina_tls.cert"), "boguspw");

        try {
            server.start();
            System.out.println("vysper server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }

        server.addModule(new SoftwareVersionModule());
        server.addModule(new EntityTimeModule());
        server.addModule(new VcardTempModule());
        server.addModule(new XmppPingModule());
        server.addModule(new PrivateDataModule());
        server.addModule(new PublishSubscribeModule());
    }
}

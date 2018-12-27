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
package org.apache.vysper.xmpp.modules.extension.xep0133_service_administration;

import org.apache.vysper.xmpp.addressing.Entity;

import java.util.Collection;

/**
 */
public interface ServerAdministrationService {

    public static final String SERVICE_ID = "ServerAdministrationService";
    
    void setAddAdminJIDs(Collection<Entity> admins);

    void setAddAdmins(Collection<String> admins);

    boolean isAdmin(Entity adminCandidate);
}

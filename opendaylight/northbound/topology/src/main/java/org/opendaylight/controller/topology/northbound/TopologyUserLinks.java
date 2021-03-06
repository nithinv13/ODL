/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topology.northbound;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.topologymanager.TopologyUserLinkConfig;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class TopologyUserLinks {
        @XmlElement
        List<TopologyUserLinkConfig> userLinks;

        //To satisfy JAXB
        private TopologyUserLinks() {

        }

        public List<TopologyUserLinkConfig> getUserLinks() {
                return userLinks;
        }

        public void setUserLinks(List<TopologyUserLinkConfig> userLinks) {
                this.userLinks = userLinks;
        }

        public TopologyUserLinks(List<TopologyUserLinkConfig> userLinks) {
                this.userLinks = new ArrayList<TopologyUserLinkConfig>(userLinks);
        }
}

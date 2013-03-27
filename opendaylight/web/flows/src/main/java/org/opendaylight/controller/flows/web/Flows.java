
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.flows.web;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.core.Name;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.controller.switchmanager.SwitchConfig;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.web.IOneWeb;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;

@Controller
@RequestMapping("/")
public class Flows implements IOneWeb {
	private static final UserLevel AUTH_LEVEL = UserLevel.CONTAINERUSER;
    private final String WEB_NAME = "Flows";
    private final String WEB_ID = "flows";
    private final short WEB_ORDER = 2;

    public Flows() {
        ServiceHelper.registerGlobalService(IOneWeb.class, this, null);
    }

    @Override
    public String getWebName() {
        return WEB_NAME;
    }

    @Override
    public String getWebId() {
        return WEB_ID;
    }

    @Override
    public short getWebOrder() {
        return WEB_ORDER;
    }

	@Override
	public boolean isAuthorized(UserLevel userLevel) {
		return userLevel.ordinal() <= AUTH_LEVEL.ordinal();
	}
	
    @RequestMapping(value = "/main")
    @ResponseBody
    public Set<Map<String, Object>> getFlows() {
        // fetch frm
        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper
                .getInstance(IForwardingRulesManager.class, "default", this);
        if (frm == null)
            return null;

        // fetch sm
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, "default", this);
        if (switchManager == null)
            return null;
        
        // get static flow list
        List<FlowConfig> staticFlowList = frm.getStaticFlows();
        Set<Map<String, Object>> output = new HashSet<Map<String, Object>>();
        for (FlowConfig flowConfig : staticFlowList) {
        	Map<String, Object> entry = new HashMap<String, Object>();
        	entry.put("flow", flowConfig);
        	entry.put("name", flowConfig.getName());
        	
        	Node node = flowConfig.getNode(); 
        	SwitchConfig switchConfig = switchManager.getSwitchConfig(node.getNodeIDString());
        	String nodeName = node.toString();
        	if (switchConfig != null) { nodeName = switchConfig.getNodeName(); }
        	entry.put("node", nodeName);
        	entry.put("nodeId", node.toString());
        	output.add(entry);
        }
        
        return output;
    }

    @RequestMapping(value = "/node-ports")
    @ResponseBody
    public Map<String, Object> getNodePorts() {
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, "default", this);
        if (switchManager == null)
            return null;

        Map<String, Object> nodes = new HashMap<String, Object>();
        Map<Short, String> port;

        for (Switch node : switchManager.getNetworkDevices()) {
            port = new HashMap<Short, String>(); // new port
            Set<NodeConnector> nodeConnectorSet = node.getNodeConnectors();

            if (nodeConnectorSet != null)
                for (NodeConnector nodeConnector : nodeConnectorSet) {
                    String nodeConnectorName = ((Name) switchManager
                            .getNodeConnectorProp(nodeConnector,
                                    Name.NamePropName)).getValue();
                    port.put((Short) nodeConnector.getID(),
                             nodeConnectorName + "("
                             + nodeConnector.getNodeConnectorIDString() + ")");
                }
            
            // add ports
            Map<String, Object> entry = new HashMap<String, Object>();
            entry.put("ports", port);
            
            // add name
            String nodeName = node.getNode().toString();
            SwitchConfig config = switchManager.getSwitchConfig(node.getNode().getNodeIDString());
            if (config != null) {
            	nodeName = config.getNodeName();
            }
            entry.put("name", nodeName);
            
            // add to the node
            nodes.put(node.getNode().toString(), entry);
        }

        return nodes;
    }
    
    @RequestMapping(value = "/node-flows")
    @ResponseBody
    public Map<String, Object> getNodeFlows() {
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, "default", this);
        if (switchManager == null) { return null; }
        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper
                .getInstance(IForwardingRulesManager.class, "default", this);
        if (frm == null) { return null; }

        Map<String, Object> nodes = new HashMap<String, Object>();

        for (Switch sw : switchManager.getNetworkDevices()) {
            Node node = sw.getNode();
            
            List<FlowConfig> flows = frm.getStaticFlows(node);
            
            String nodeName = node.toString();
            SwitchConfig config = switchManager.getSwitchConfig(node.getNodeIDString());
            if (config != null) {
            	nodeName = config.getNodeName();
            }
            
            nodes.put(nodeName, flows.size());
        }

        return nodes;
    }

    @RequestMapping(value = "/flow", method = RequestMethod.POST)
    @ResponseBody
    public String actionFlow(@RequestParam(required = true) String action,
            @RequestParam(required = false) String body, @RequestParam(required = true) String nodeId) {
    	if (!authorize(UserLevel.NETWORKADMIN)) {
    		return "Operation not authorized";
    	}
    	
        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper
                .getInstance(IForwardingRulesManager.class, "default", this);
        if (frm == null) { return null; }

        Gson gson = new Gson();
        FlowConfig flow = gson.fromJson(body, FlowConfig.class);
        Node node = Node.fromString(nodeId);
        flow.setNode(node);
        Status result = null;
        if (action.equals("add")) {
            result = frm.addStaticFlow(flow, false);
        }

        return result.getDescription();
    }
    
    @RequestMapping(value = "/flow/{nodeId}/{name}", method = RequestMethod.POST)
    @ResponseBody
    public String removeFlow(@PathVariable("nodeId") String nodeId, @PathVariable("name") String name,
    		@RequestParam(required = true) String action) {
    	if (!authorize(UserLevel.NETWORKADMIN)) { return "Operation not authorized"; }
    	
    	IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper
                .getInstance(IForwardingRulesManager.class, "default", this);
        if (frm == null) { return null; }
        
        Status result = null;
        Node node = Node.fromString(nodeId);
        if (node == null) {
            return null;
        }
        if (action.equals("remove")) {
        	result = frm.removeStaticFlow(name, node);
        } else if (action.equals("toggle")) {
        	FlowConfig config = frm.getStaticFlow(name, node);
        	result = frm.toggleStaticFlowStatus(config);
        } else {
        	result = new Status(StatusCode.BADREQUEST, "Unknown action");
        }
        
        return result.getDescription();
    }
    
    /**
     * Is the operation permitted for the given level
     * 
     * @param level
     */
    private boolean authorize(UserLevel level) {
    	IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, this);
        if (userManager == null) {
        	return false;
        }
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserLevel userLevel = userManager.getUserLevel(username);
        if (userLevel.toNumber() <= level.toNumber()) {
        	return true;
        }
        return false;
    }

}
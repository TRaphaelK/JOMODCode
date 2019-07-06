/*
 * ClientMessage.java
 *
 * Created on 2006. április 28., 19:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.map;

import xml.Element;

/**
 *
 * @author vear
 */
public class ClientMessage {
    
    // ping
    public static final int NOP=0;
    // logint to server
    public static final int LOGIN=1;
    // login request to local server
    public static final int LOGIN_LOCAL=2;
    // server order to client to create a local controller
    public static final int CREATE_LOCAL=3;
    // server order to client to create a local controller
    public static final int DROP=4;
    
    private int messageType;
    
    private int clientId;
    
    // a client is provided with login requests
    private GameClient client;
    
    /** Creates a new instance of ClientMessage */
    public ClientMessage(int messageType) {
       this.messageType = messageType;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }
    
    public void setClient(GameClient client) {
        this.client=client;
        clientId=client.getId();
    }
    
    public GameClient getClient() {
        return client;
    }
    
    public void handleMessage(MapHeader h) {
        switch(messageType) {
            case 2: {
                if(checkControl(h)) {
                    GameClient pgc=h.getLocalClient();
                    // if there was a previous local client, drop it
                    if(pgc!=null) {
                        // remove from header
                        h.removeClient(pgc.getId());
                        pgc.drop();
                    }
                    // create a new
                    GameClient ngc=h.createClient();
                    ngc.fromOther(client);
                    this.setClient(ngc);
                    // add to header
                    h.setLocalClient(ngc);
                    // if it is to control a team, assume control
                    assumeControl(h);
                }
            } break;
        }
    }
    
    private boolean checkControl(MapHeader h) {
        // checks permissins to view or control certain teams
        // if there already is a controller for a certain team
        // dont allow
        int control=-1;
        int mt=h.getMaxTeams();
        for(int i=0;i<mt;i++) {
            if(client.isCanControl(i)) {
                if(control!=-1) {
                    // cant control more than one team
                    return false;
                } else {
                    // is there someone already controlling that team?
                    GameClient cgc=h.getController(i);
                    if(cgc!=null) {
                        // dont allow
                        return false;
                    }
                    control=i;
                }
            }
        }
        return true;
    }
    
    public Element toXML() {
        Element e=new Element("ClientMessage");
        e.setChild("messageType").setText((byte)messageType);
        if(client==null) {
            if(clientId!=0) e.setChild("GameClient").setChild("id").setText((byte)clientId);
        } else {
            e.addContent(client.toXML());
        }
        return e;
    }
    
    public void fromXML(Element e) {
        messageType=e.getChildbyte("messageType");
        Element gce=e.getChild("GameClient");
        if(gce!=null) {
            client = new GameClient();
            client.fromXML(gce);
            clientId=client.getId();
        }
    }

    private void assumeControl(MapHeader h) {
        for(int i=0;i<h.getMaxTeams();i++) {
            if(client.isCanControl(i)) {
                h.setController(i,client);
            }
        }
    }

}

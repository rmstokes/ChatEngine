/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package websocket.chat;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import util.HTMLFilter;

@ServerEndpoint(value = "/chat")
public class ChatAnnotation {

    private static final Log log = LogFactory.getLog(ChatAnnotation.class);

    private static final String GUEST_PREFIX = "Guest";
    private static final AtomicInteger connectionIds = new AtomicInteger(0);
    private static final Set<ChatAnnotation> connections =
            new CopyOnWriteArraySet<>();

    private final String nickname;
    private Session session;

    public ChatAnnotation() {
        nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
    }
    
    


    @OnOpen
    public void start(Session session) {
        this.session = session;
        connections.add(this);
        //set senderID here; clientID is set by broadcast function before sending message to entire group.
        String message = "<message type='alert' clientID='' senderID='"+session.getId()+"'>"+
        "<text>"+nickname+" has joined.</text>"+
        "</message>";        
              
        try {
			broadcast(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }


    

	@OnClose
    public void end() {
        connections.remove(this);
        
        //need to change this to xml format        
        String message = String.format("* %s %s",
                nickname, "has disconnected...");        
        
        try {
			broadcast(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }


    @OnMessage
    public void incoming(String message) throws Exception {
        //parse xml message and send broadcast to group.
    	//may handle certain types of messages differently though
            
        
    	//parse xml
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();
        StringReader sr = new StringReader(message);
        InputSource is = new InputSource(sr);
        Document doc = builder.parse(is);
        
        //get message type (will be utilized in future iterations of web container)
        Element element = doc.getDocumentElement();
        String messageType = element.getAttribute("type");
                       
        //add origin senderID attribute to the document before broadcasting it.
        Attr attribute = doc.createAttribute("senderID");     
        String senderID = session.getId();
        attribute.setValue(senderID); 
        element.setAttributeNode(attribute);   
        
        //now that the senderID has been added to document
        //transform the updated document into a string before broadcasting
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        message = writer.toString();        
    	broadcast(message);                
        
    }


    @OnError
    public void onError(Throwable t) throws Throwable {
        log.error("Chat Error: " + t.toString(), t);
    }
    
    


    private static void broadcast(String msg) throws Exception{
    	//unravel msg xml to add clientID to the message before sending it.
    	//this is so each client knows who they are from server's perspective.
    	
    	//access xml Document
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();
        StringReader sr = new StringReader(msg);
        InputSource is = new InputSource(sr);
        Document doc = builder.parse(is);
        
        //create attribute name
        Attr attribute = doc.createAttribute("clientID");
        Element element = doc.getDocumentElement(); //message element
                
        for (ChatAnnotation client : connections) {
        	String clientID = client.session.getId();
        	attribute.setValue(clientID);
        	element.setAttributeNode(attribute);  
        	
        	//transform the updated document (the one with clientID and senderID) into a string before broadcasting
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);            
            msg = writer.toString();
            try {
                synchronized (client) {
                    client.session.getBasicRemote().sendText(msg);
                }
            } catch (IOException e) {
                log.debug("Chat Error: Failed to send message to client", e);
                connections.remove(client);
                try {
                    client.session.close();
                } catch (IOException e1) {
                    // Ignore
                }
                String message = String.format("* %s %s",
                        client.nickname, "has been disconnected.");
                broadcast(message);
            }
        }
    }
}

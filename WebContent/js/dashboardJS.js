/**
 * 
 */

"use strict";
var Chat = {};
var clientID = window.name;
//util_permIDCheck();

Chat.socket = null;

var host = '';
var setCreated = null;

var groups = [];
var numGroups = 0;

var TAfile = null;

var sentLeaveDash = false;

//Template for group dashboard window
var AMGroupWindow = document.getElementById("GXGroupWindow").cloneNode(true);
document.getElementById("GXGroupWindow").style.display = "none";

window.onbeforeunload = util_closeSocket;

var serverPath = window.location.pathname.match(/^(.*)\./)[1];
var hostname = window.location.host;
//document.getElementById("output").innerHTML = ("serverPath: " + serverPath + " hostname: " + hostname);

//alert("serverPath: " + serverPath + " hostname: " + hostname);

util_openSocket("/dashXML"); //open webSocket

	Chat.socket.onopen = function() {
		// connection opened with server
		$ ("#clientGfx").addClass("activeWS");
		$ ("#serverGfx").addClass("activeWS");
		$ ("#pingGfx").addClass("pingPong");
		util_affirmUserClient();
		//$ ('#createBtn').removeAttr('disabled');
		//util_setUserClient();
		console.log("WebSocket was opened");
	};

	Chat.socket.onclose = function() {
		// connnection closed with server
		$ ("#clientGfx").removeClass("activeWS");
		$ ("#serverGfx").removeClass("activeWS");
		$ ("#pingGfx").removeClass("pingPong");
		console.log("WebSocket was closed.");
		
		$ ('button').attr('disabled', true);
		
		if (!sentLeaveDash) {
			$("#serverGfx").addClass("disconnectWS");
			$("#pingGfx").addClass("reconnect");

			setTimeout(util_reconnectSocket, 1000); //reconnect after 1s
		}
	};
	
	Chat.socket.onmessage = function(message) {
		//
		//dashLog("message received");
		// message received from server
		//alert that got message and send some text from message
		//message.getNode
		var xml = message.data;
		var parser = new DOMParser();
		var xmlDoc = parser.parseFromString(xml, "text/xml");
		
		var messageNode = xmlDoc.getElementsByTagName('message')[0];
		var messageType = messageNode.getAttribute('type');
		
		
		
		
		if (messageType == 'permIDSet' || messageType == 'permIDConfirm') {
			console.log(messageType+" "+messageNode.getAttribute('senderID'));
			util_setPermID(messageNode.getAttribute('senderID'));
			
		} else if (messageType == 'DashUpdate') {
			updateDash(messageNode);
			printGroups();
		} 
		else {
			console.log("Could not parse server message: \n" + message.data);
		}

	}; // end onmessage
	
	function updateDash(message){
		
		//dashLog("updateDash entered");
		
		
		//Parses and places the statistics at the bottom of the chatBox
		var groupStatArr = message.getElementsByTagName('group_summary');
		
		
		groups = [];
		numGroups = groupStatArr.length;
		
		dashLog(numGroups);
		
		for(var i=0; i<groupStatArr.length; i++) {
			var groupStat = groupStatArr[i];
			
			var groupID = String(groupStat.getAttribute('groupname'));
			//dashLog("GRP: " + groupID);
			//getGroupStats(groupStat);
			groups[i] = new Group(groupStat);
			
			//dashLog(groups[i].toString());
		}
		
		
	};
	
	function Group(group){
		this.name = String(group.getAttribute('groupname'));
		this.sessionName = String(group.getAttribute('sessionname'));
		this.count = group.getElementsByTagName('group_person_summary').length;
				
		var tempMembers = group.getElementsByTagName('group_person_summary');
			
		var newMembers = []
		for(var i=0; i<this.count; i++) {
			
			newMembers[i] = new Member(tempMembers[i]);
			
			}
		this.members = newMembers;			
			
		
		
		this.toString = function(){
			
			var out = this.name;
			
			out += " " + this.sessionName;
			
			for (var i = 0; i<this.count; i++){
				
				out += " " + this.members[i].toString();
				
			}
			return out;
			

		}
	}
	
	function Member(member){
		this.name = String(member.getAttribute('name'));
		
		
		
		this.attrNames = member.attributes;
		var attrDict = {};
		
		for(var j=0; j<this.attrNames.length; j++){
			var attribute = this.attrNames[j].localName;
			var value = this.attrNames[j].value;
			if (attribute == 'name'){
				continue;
			} else{
				attrDict[attribute] = value;
			}
		}
		
		this.attributes = attrDict;	
			
		this.toString = function(){
			//dashLog(this.attrNames.length);
			var out = this.name;
			for (var key in this.attributes){
				out += " " + key + ": " + this.attributes[key];
			}
			
			return out;
				
		}		
	}
	
	
//	function getGroupStats(group){
//		//dashLog("Entered getGroupStats");
//		var result = {};
//		//dashLog("array created");
//		var members = group.getElementsByTagName('group_person_summary');
//		//dashLog("Got Members");
//		for(var i=0; i<members.length; i++) {
//			var member = members[i];
//			var name = String(member.getAttribute('name'));
//			//dashLog(name);
//			var attrNames = member.attributes;
//			var attributes = {};
//			//dashLog("got attributes")
//			for(var j=0; j<attrNames.length; j++){
//				var attribute = attrNames[i];
//				if (attribute == 'name'){
//					continue;
//				} 
//				attributes[attribute] = member.getAttribute(attribute);
//				
//			}
//			result[name] = attributes;
//			//dashLog(name);
//			
//			
//		}
//		
//		
//		return result;
//	}
	
	function printGroups(){
		dashLog("printGroups Called");
		for (var i = 0; i<numGroups; i++){
			dashLog(groups[i].toString());
		}
		
	}
	
	function dashLog(message){
		document.getElementById("output").innerHTML = (document.getElementById("output").innerHTML + "<br>" + message);
	}
	
	
	window.onbeforeunload = function () {
		//This is assuming the user has purposely closed the page/refreshed the page.
		//Send a closeSocket message to the server- the server must know that client disconnect on purpose
		if(Chat.socket==null) //If Chat.socket is not loaded, dont run
			return;
		
		var xml = '<message type="dashLeave" senderID="'+clientID+'"/>';
		Chat.socket.send(xml);
		sentLeaveDash = true;
		console.log("before unload has run");
	};

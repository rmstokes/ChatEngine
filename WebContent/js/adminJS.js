/**
 * 
 */

"use strict";
var Chat = {};
var clientID = window.name;
util_permIDCheck();

Chat.socket = null;

var host = '';
var adminGroupsCreated = null;

// this function called by Chat.initialize function below
Chat.connect = (function(host) {
	// create websocket if supported by client
	if ('WebSocket' in window) {
		Chat.socket = new WebSocket(host);
		// alert('admin page client id: '+clientID);
	} else if ('MozWebSocket' in window) {
		Chat.socket = new MozWebSocket(host);
	} else {
		Console.log('Error: WebSocket is not supported by this browser.');
		return;
	}

	Chat.socket.onopen = function() {
		// connection opened with server
		util_setUserClient();
		util_changeIcon(true);
	};

	Chat.socket.onclose = function() {
		// connnection closed with server
		util_changeIcon(false);

	};
	// Chat.socket.onclose = util_close();

	Chat.socket.onmessage = function(message) {
		// message received from server
		// if groups have been created then alert user and
		// provide a way to navigate to the login page

		var xml = message.data;
		var parser = new DOMParser();
		var xmlDoc = parser.parseFromString(xml, "text/xml");
		var messageNode = xmlDoc.getElementsByTagName('message')[0];
		var messageType = messageNode.getAttribute('type');
		if (messageType == 'groupsCreated') {
			alert('Groups have been successfully created');
			// may want to automatically log admin into all the groups in
			// multiple tabs

		} else if (messageType == 'checkGroups') {
			var checkGroups = messageNode.getAttribute('checkGroups');
			var checkGroupHTML = document.getElementById("checkGroupPara");
			var groupOff = messageNode.getAttribute('groupOffset');
			checkGroupHTML.innerHTML = "GroupNum: " + checkGroups
					+ " <br/> Offset: " + groupOff;
			if (checkGroups == "Not Created") {
				checkGroupHTML.style.backgroundColor = "Navy";
				adminGroupsCreated = false;
			} else {
				checkGroupHTML.style.backgroundColor = "SeaGreen";
				adminGroupsCreated = true;
			}
			if (adminGroupsCreated) {
				document.getElementById("adminMonitorBtn").disabled = false;
				document.getElementById("loginBtn").disabled = false;
			}
		} else if (messageType == 'redirect') {
			util_redirect(messageNode);
		} else if (messageType == 'noPermID') {
			util_noPermID();
		} else if (messageType == 'AMStatus') {
			//capture but do nothing
		} else if (messageType == 'alert') {
			var msgtext = messageNode.childNodes[0].nodeValue;
			util_alert(msgtext);
		} else {
			alert("Could not parse server message: \n" + message.data);
		}

	}; // end onmessage
}); // end Chat.connect()


Chat.initialize = util_webSocketConnect;
Chat.initialize();
window.onunload = util_closeSocket();

function createGroups() {

	if (adminGroupsCreated) {
		var adminOverwrite = confirm("A list of groups already exist. Do you want to overwrite them? \n"
				+ "This will reset the group documentation.");
		if (!adminOverwrite) {
			return;
		}
	}
	// send xml message to server to create groups.
	var numGroups = document.getElementById('groupCount').value;
	var instruct = document.getElementById('instruct').value;
	var groupOff = document.getElementById('groupOffset').value;
	// alert('numGroups: '+numGroups);
	var xml = '<message type="groupCreation" senderID="' + clientID
			+ '" instructor="' + instruct + '" groupOffset="' + groupOff + '">'
			+ '<text>' + numGroups + '</text>' + '</message>';
	Chat.socket.send(xml);
}
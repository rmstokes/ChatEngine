"use strict";

var Chat = {};
var clientID = window.name;

// Before permIDCheck, get ID if none exists- ONLY FOR LOGIN PAGE

Chat.socket = null;

var host = '';
var adminGroupsCreated = null;
var groupPrompt = false;
// true - user has been prompted that groups exist/not exist - needs to be
// reminded
// false group has not been prompted and assumes groups exist

Chat.connect = (function(host) {
	if ('WebSocket' in window) {
		Chat.socket = new WebSocket(host);
	} else if ('MozWebSocket' in window) {
		Chat.socket = new MozWebSocket(host);
	} else {
		Console.log('Error: WebSocket is not supported by this browser.');
		return;
	}

	Chat.socket.onopen = function() {
		// Login special access for clientID
		/*
		 * if (clientID == null || clientID == "") { var xml = '<message
		 * type="setUserType">'+ '<text>PermIDGet</text>'+ '</message>';
		 * setTimeout(function () {Chat.socket.send(xml);}, 100);
		 * //Chat.socket.send(xml); //Assume 100% success and let server side
		 * validate on connect //Give it a second //setTimeout(function ()
		 * {util_permIDCheck();}, 100);
		 *  }
		 */

		util_setUserClient();
		util_changeIcon(true);
	};

	Chat.socket.onclose = function() {
		
		util_changeIcon(false);
	};

	// Chat.socket.onclose = util_close();

	Chat.socket.onmessage = function(message) {
		// message.data here will be in xml format
		var xml = message.data;
		var parser = new DOMParser();
		var xmlDoc = parser.parseFromString(xml, "text/xml");
		var messageNode = xmlDoc.getElementsByTagName('message')[0];
		var messageType = messageNode.getAttribute('type');

		if (messageType == 'displayChat') {
			util_webRedirect('chat');
			// window.location.href = 'http://'+window.location.host +
			// '/Chat/chat.xhtml';

		} else if (messageType == 'redirect') {
			util_redirect(messageNode);
		} else if (messageType == 'noPermID') {
			util_noPermID();
		} else if (messageType == 'AMStatus') {
			//capture but do nothing
		} else if (messageType == 'alert') {
			var msgtext = messageNode.childNodes[0].nodeValue;
			util_alert(msgtext);
		} else if (messageType == 'checkGroups') {
			var checkGroups = messageNode.getAttribute('checkGroups');
			if (checkGroups == 'Not Created') {
				alert("There are no groups created currently.");
				document.getElementById("joinBtn").disabled = true;
				groupPrompt = true;
			} else {
				if (groupPrompt)
					alert("Groups have been created/updated!");

				// generate number of groups to join, groupNum = checkGroups
				var groupPicker = document.getElementById("groupSelection");
				var groupNum = Number(checkGroups);
				var groupOff = Number(messageNode.getAttribute('groupOffset'));
				groupNum += groupOff;

				// Just remove everything cause offset if unreliable
				while (groupPicker.length > 0) {
					groupPicker.remove(groupPicker.length - 1);
				}
				// Add groups until we reach max
				for (var i = groupOff; i < groupNum; i++) {
					var groupOpt = document.createElement('option');
					groupOpt.text = "Group " + (i + 1);
					groupOpt.value = (i + 1);
					groupPicker.add(groupOpt);
				}
				document.getElementById("joinBtn").disabled = false;
			}
		} else {
			alert("Could not parse server message: \n" + message.data);
		}

	}

});

/*
 * Chat.initialize = function() { if (window.location.protocol == 'http:') {
 * Chat.connect('ws://' + window.location.host + '/Chat/login'); } else {
 * Chat.connect('wss://' + window.location.host + '/Chat/login'); } }
 * 
 * Chat.initialize();
 */

Chat.initialize = util_webSocketConnect;
Chat.initialize();
window.onunload = util_closeSocket();

window.onload = function() {
	// alert('');
	// Given up trying to make this work in Firefox
	setTimeout("document.getElementById('emailAddress').focus();", 1);

	document.getElementById('emailAddress').onkeyup = function(event) {
		if (event.keyCode == 13) // 13 == Enter
			joinGroup();
	};
	
};

function joinGroup() {
	var groupNumber = document.getElementById('groupSelection').value;
	var username = document.getElementById('emailAddress').value;
	if (username == "") {
		alert("Username is blank!");
		return;
	}
	// store the username in persistent variable so chat page can access it.
	// window.name = window.name + "$" + username; dont change window.name
	var xml = '<message type="joinGroup" senderID="' + clientID
			+ '" username="' + username + '"><text>' + groupNumber
			+ '</text></message>';
	Chat.socket.send(xml);
}
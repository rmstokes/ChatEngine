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
		document.getElementById("output").innerHTML = ("messgae received");
		alert("message received")
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
		} 
		else {
			console.log("Could not parse server message: \n" + message.data);
		}

	}; // end onmessage
	
	function updateDash(message){
		
		
		//Parses and places the statistics at the bottom of the chatBox
		var groupStatArr = message.getElementsByTagName('group_summaries');
		
		for(var i=0; i<groupStatArr.length; i++) {
			var groupStat = groupStatArr[i];
			var groupID = parseInt(groupStat.getAttribute('groupname'));
			
			for (element in groupStat){
				
			}
		}
		
		
	};
	
	
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

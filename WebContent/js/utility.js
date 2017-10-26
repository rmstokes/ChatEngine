/**
 * Utility functions for all client pages of the program
 * Means you don't have to rewrite code for each page
 */

function util_redirect(messageNode) {
	var redirect = messageNode.getAttribute('path');
	var serverPath = window.location.pathname.match(/^(\/\w*\/)/)[0];
	window.location.href = 'http://'+window.location.host + serverPath +redirect+'.html';
}

function util_webRedirect(webpage) {
	var serverPath = window.location.pathname.match(/^(\/\w*\/)/)[0];
	window.location.href = 'http://'+window.location.host + serverPath +webpage+'.html';
}

function util_getHostPath() {
	var serverPath = window.location.pathname.match(/^(\/\w*\/)/)[0];
	return window.location.host + serverPath;
}

function util_getServerPath() {
	return window.location.pathname.match(/^(\/\w*\/)/)[0];
}

//Used to connect to the right path for Websocket; page = currentPage
//Ignore error, due to default argument
function util_webSocketConnect(page) {
	//Extract pathname from the URL to connect to the right path
	var serverPath = window.location.pathname.match(/^(\/\w*\/)/)[0];
	if (page == null || page == undefined) {
		serverPath = window.location.pathname.match(/^(.*)\./)[1];
		page = '';
	}
	
	 if (window.location.protocol == 'http:') {                
         Chat.connect('ws://' + window.location.host + serverPath + page);
     } else {
         Chat.connect('wss://' + window.location.host + serverPath +page);
     }
}

function util_openSocket (){
	var serverPath = window.location.pathname.match(/^(.*)\./)[1];
	var wspath;
	 if (window.location.protocol == 'http:') {            
         wspath = 'ws://' + window.location.host + serverPath;
     } else {
         wspath = 'wss://' + window.location.host + serverPath;
    }
	 
	if ('WebSocket' in window) {
		Chat.socket = new WebSocket(wspath);
	} else if ('MozWebSocket' in window) {
		Chat.socket = new MozWebSocket(wspath);
	} else {
		console.log('Error: WebSocket is not supported by this browser.');
		return;
	}
}

function util_closeSocket() {
	if (Chat.socket.readyState==1) //if socket is open
		Chat.socket.close();
}

var reconnectNumber = 0;
function util_reconnectSocket() {
	if (reconnectNumber==0) {
		Chat.Open = Chat.socket.onopen;
		Chat.Error = Chat.socket.onerror;
		Chat.Message = Chat.socket.onmessage;
		Chat.Close = Chat.socket.onclose;
	} else if (reconnectNumber>10) { //too many attempts
		console.log("Failed to reconnect WebSocket-Too many attempts-"+reconnectNumber);
		$ ("#clientGfx").removeClass().addClass("circleBase");
		$ ("#serverGfx").removeClass().addClass("circleBase");
		$ ("#pingGfx").removeClass().addClass("circleBase ping");
		return;
	}
	
	if (Chat.socket.readyState==3) { //Closed
		console.log("Attempting reconnect attempt-"+reconnectNumber);
		util_openSocket();
		Chat.socket.onopen = Chat.Open;
		Chat.socket.onerror = Chat.Error;
		Chat.socket.onmessage = Chat.Message;
		Chat.socket.onclose = Chat.Close;
		reconnectNumber++;
	}
	
	//Close WS already calls this, no need for timeout
//	if (Chat.socket.readyState!=1) { //If not open, redo function
//		setTimeout(util_reconnectSocket, 1000); //1s wait
//	} else { //connected
//		reconnectNumber = 0; //reset counter
//		console.log("Reconnect successful!!");
//		return;
//	}
	
}

function util_webSocketCheck() {
	//create websocket object if browser supports it
    if ('WebSocket' in window) {
        Chat.socket = new WebSocket(host);                 
    } else if ('MozWebSocket' in window) {
        Chat.socket = new MozWebSocket(host);                   
    } else {
        //browser doesn't support websocket
        alert('Error: WebSocket is not supported by this browser.');
        return;
    }
}

function util_noPermID() {
	alert("No Permanent ID was detected for this page, or no Client was created for this page. \n" +
			"Redirecting you back to the landing page to refresh the ID.");
	var serverPath = window.location.pathname.match(/^(\/\w*\/)/)[0];
	window.location.href = 'http://'+window.location.host + serverPath;
}

function util_alert(value) {
	alert(value);
}

function util_permIDCheck() {
    console.log("clientID is "+clientID);
	if(window.name=="" || window.name == null)
		util_noPermID();
}

function util_setUserClient() {
	var xml = '<message type="userClientGet" senderID="'+clientID+'">'+
    //'<text>'+numGroups+'</text>'+
    '</message>';
	Chat.socket.send(xml);
}

function util_affirmUserClient() {
	var xml = '<message type="userClientAffirm" senderID="'+clientID+'">'+
    //'<text>'+numGroups+'</text>'+
    '</message>';
	Chat.socket.send(xml);
}

function util_setPermID(permID) {
	clientID = permID;
	window.name = permID;
}

function util_chatHighlightAlert (alertText) {
	alertText = alertText.replace(/submit\w*/gi, 
			'<span style="color:green;">$&</span>');
	
	alertText = alertText.replace(/withdraw\w*/gi, 
			'<span style="color:slateblue;">$&</span>');
	
	alertText = alertText.replace(/review\w*/gi, 
			'<span style="color:salmon;">$&</span>');
	
	alertText = alertText.replace(/wrong\w*/gi, 
		'<span style="color:red; font-weight: bold;">$&</span>');

	alertText = alertText.replace(/correct\w*/gi, 
			'<span style="color:green; font-weight: bold;">$&</span>');
	
	return alertText;
}
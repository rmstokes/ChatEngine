/**
 * Utility functions for all client pages of the program
 * Means you don't have to rewrite code for each page
 */

function util_redirect(messageNode) {
	var redirect = messageNode.getAttribute('path');
	var serverPath = window.location.pathname.match(/^(\/\w*\/)/)[0];
	window.location.href = 'http://'+window.location.host + serverPath +redirect+'.xhtml';
}

function util_getHostPath() {
	var serverPath = window.location.pathname.match(/^(\/\w*\/)/)[0];
	return window.location.host + serverPath;
}

//Used to connect to the right path for Websocket; page = currentPage
//Ignore error, due to default argument
function util_webSocketConnect(page = null) {
	//Extract pathname from the URL to connect to the right path
	var serverPath = window.location.pathname.match(/^(\/\w*\/)/)[0];
	if (page == null) {
		serverPath = window.location.pathname;
		page = '';
	}
	
	 if (window.location.protocol == 'http:') {                
         Chat.connect('ws://' + window.location.host + serverPath + page);
     } else {
         Chat.connect('wss://' + window.location.host + serverPath +page);
     }
}

function util_webSocketCheck() {
	//create websocket object if browser supports it
    if ('WebSocket' in window) {
        Chat.socket = new WebSocket(host);                 
    } else if ('MozWebSocket' in window) {
        Chat.socket = new MozWebSocket(host);                   
    } else {
        //browser doesn't support websocket
        Console.log('Error: WebSocket is not supported by this browser.');
        return;
    }
}

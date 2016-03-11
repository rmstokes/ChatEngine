/**
 * Utility functions for all client pages of the program
 * Means you don't have to rewrite code for each page
 */

function util_redirect(messageNode) {
	var redirect = messageNode.getAttribute('path');
	var serverPath = window.location.pathname.match(/^(\/\w*\/)/)[0];
	window.location.href = 'http://'+window.location.host + serverPath +redirect+'.xhtml';
}

function util_webRedirect(webpage) {
	var serverPath = window.location.pathname.match(/^(\/\w*\/)/)[0];
	window.location.href = 'http://'+window.location.host + serverPath +webpage+'.xhtml';
}

function util_getHostPath() {
	var serverPath = window.location.pathname.match(/^(\/\w*\/)/)[0];
	return window.location.host + serverPath;
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
	if(window.name=="" || window.name == null)
		util_noPermID();
}

function util_close() {
	alert("An error occurred. The websocket was closed.");
}
/**
 * Fairly expansive javascript for chat settings
 */

"use strict";

console.log("Not this kind of Console.log");

// Shamelessly stolen from StackOverflow
// http://stackoverflow.com/questions/1267283/how-can-i-create-a-zerofilled-value-using-javascript/1268377#1268377
Number.prototype.pad = function(numZeros) {
	// var n = Math.abs(num);
	var zeros = Math.max(0, numZeros - Math.floor(this).toString().length);
	var zeroString = Math.pow(10, zeros).toString().substr(1);

	return zeroString + this;
}

//Chat color constants
//var TYPING_BGC = '#ebf2f9';
var TYPING_BGC = 'Lavender';
var USER_TYPING_BGC = 'CornSilk';
var USER_BGC = 'LavenderBlush';
var CHAT_HISTORY_BGC = 'LightGoldenRodYellow';
var CHAT_HISTORY_DBGC = 'Beige';
var SERVER_COLOUR = 'Navy';

//SenderID and ClientID
var clientID = window.name;

var Chat = {};
Chat.socket = null;

var host = '';
var Chat_Alert_Past = false; // highlight past chat messages
var Scroll_To_Bot = true; // user scrolling up
//var User_Type_Confirm = null;

var sentLeaveChat = false;
var reconnectAttempt = null;
var reconnectAttemptCounter = 0;

// emoticon constants
// All the strings need to be escaped, so there will be weird formatting
// Will put a commented version of both on the right
// The Unicode emoticon also needs to be escaped
var delmt = '\\';
var EMO_N_ICON = {
	'&#x1F601;' : ':B', // :B teeth
	'&#x1F604;' : ':D', // :D 😁 (looks like they dont work in Eclipse rip
	'&#x1F606;' : 'XD', // XD
	'&#x1F631;' : 'D:', // D:

	'&#x1F60A;' : ':\)', // :)
	'&#x1F629;' : ':\(', // :(
	'&#x1F610;' : ':\|', // :|

	'&#x1F612;' : ':\/', // :/ Bound to the same key so gotta change
	'&#x1F60F;' : ':\\', // :\
	'&#x1F609;' : ';\/', // ;/
	'&#x1F625;' : ';\\', // ;\

	'&#x1F623;' : '>.<', // >.<
	'&#x1F60B;' : ':P', // :P
	'&#x1F61D;' : 'XP', // XP
	'&#x1F61C;' : ';P', // ;P

	'&#x1F62D;' : 'T^T', // T^T
	'&#x1F622;' : 'T.T', // T.T
	'&#x1F602;' : 'TwT', // TwT

	'&#x1F60D;' : '\(&lt;3', // (<3
	'&#x1F60E;' : 'B\)', // B) cool dude
	'&#x1F636;' : ':x', // :x

	// '&#x1F632;' : ':O', // :O dunno what to use here
	'&#x1F632;' : ':o', // :o
	// '&#x1F620;' : '>:O', // >:O
	'&#x1F620;' : '>:o', // >:o
	'&#x1F635;' : '@.@', // @.@
	'&#x1F62A;' : 'zzz', // zzz

	'&#x2764;' : '&lt;3', // <3
	'&#x26FA;' : '^^', // ^^

};

// add padding to emoticons to prevent invalid strings from being caught
for ( var emoUni in EMO_N_ICON) {
	// grab the emoticon and put into this format \s(:P)\b
	if (!EMO_N_ICON.hasOwnProperty(emoUni))
		continue;

	var emoStr = EMO_N_ICON[emoUni];
	// Convert into string literal into regex parsable string
	emoStr = emoStr.replace(/(\()|(\))|(\|)|(\/)|(\\)|(\^)|(\>)|(\<)/g, '\\$&');

	/*
	 * Ok so the thing is, for emoticons with letters like :P, I can use \b to
	 * test for a word boundary behind the P; which can test if there are more
	 * letters AND if it ends the string. For non letter emoticons I cant do
	 * this. So now Im generating regex, 2 for letter emoticons and 4 for others
	 * cause Im going over the full chat paragraph, > & < are start & end
	 * letters are ^EMO\b or \sEMO\b (or is |) non letters are ^EMO$ or
	 * \sEMO(?=\s) or \sEMO$ or ^EMO\s
	 */
	if (emoStr.search(/\w$/) != -1) // if ENDS a word character- B, D, etc
		emoStr = '^\(' + emoStr + '\)\\b|\\s(' + emoStr + ')\\b'; // letter regex
	else
		emoStr = '^\(' + emoStr + '\)(?=\\s)|\\s\(' + emoStr
				+ '\)\(?=\\s\)|\\s\(' + emoStr + '\)$|^\(' + emoStr + '\)$';

	var regexTest = new RegExp(emoStr, 'gm');
	// window.console.log(regexTest);
	EMO_N_ICON[emoUni] = regexTest; // convert into regex
}

//Setting up Chat websocket (this is the entrance of the websocket init)
Chat.connect = (function(host) {
	if ('WebSocket' in window) {
		Chat.socket = new WebSocket(host);
	} else if ('MozWebSocket' in window) {
		Chat.socket = new MozWebSocket(host);
	} else {
		console.log('Error: WebSocket is not supported by this browser.');
		return;
	}

	Chat.socket.onopen = function() {
		// create onkeyup listener 
		document.getElementById('chat').onkeyup = function(event) {
			Chat.sendMessage(event);
		};
		//put anti-scroll mech
		document.getElementById('console').onscroll = function(event) {
			// if scrolled up, freeze the scroll bar
			var consoleC = document.getElementById('console');
			var scrollCoef = consoleC.scrollTopMax - consoleC.scrollTop;
			if (isNaN(scrollCoef)) //chrome stuff
				scrollCoef = consoleC.scrollHeight - consoleC.scrollTop - consoleC.clientHeight;
			Scroll_To_Bot = !(scrollCoef > 100); // average p is about 22px
		}
		
		//Remove everything in console window
		var consoleC = document.getElementById('console');
		while (consoleC.firstElementChild)
			consoleC.removeChild(consoleC.firstElementChild);

		if (reconnectAttemptCounter>0) { //reconnection was attempted and was successful
			Console.log('Info: WebSocket connection reconnected.');
			var xml = '<message type="reconnectChat" senderID="' + clientID
				+ '" ></message>';
			Chat.socket.send(xml);
			//Cancel set interval
			//clearInterval(reconnectAttempt);
		} else {
			Console.log('Info: WebSocket connection opened.');

			// broadcast to group that user has joined
			var xml = '<message type="joinChat" senderID="' + clientID
				+ '" ></message>';
			Chat.socket.send(xml);
		}
		
		util_changeIcon(true);
	};

	Chat.socket.onclose = function() {
		document.getElementById('chat').onkeydown = null;
		document.getElementById('console').onscroll = null;
		Console.log('Info: WebSocket closed... host: ' + host);
		
		util_changeIcon(false);
		
		//if disconnected, attempt reconnect
		if (!sentLeaveChat) {
			Console.log('Info: Attempting reconnect.');
			document.getElementById('chat').value = '';
			
			//Reconnect is run everytime socket fails- setInterval not needed
			//reconnectAttemptCounter = 0;
			//reconnectAttempt = setInterval(function () {
				reconnectAttemptCounter++;
				if(reconnectAttemptCounter>100) { //tried to reconnect 100 times 
					Console.log('Info: Failed to reconnect >100 times. Unable to reach server.');
					return;
				}
				if (Chat.socket.readyState>1) { //Not open or connecting
					Console.log('Info: Trying to reconnect');
					setTimeout(Chat.initialize(), 1000); //wait 1 second
					//Chat.initialize();
				}
			//}, 100); //Attempt every 0.1s
		}
		
	};

	Chat.socket.onmessage = function(message) {
		// message arrives in xml, parse data into xml format and get attributes
		// sort by message type
		// Typing- user still typing, save reference
		// Chat - user finished typing
		// Alert - Info from server

		var xml = message.data;
		var parser = new DOMParser();
		var xmlDoc = parser.parseFromString(xml, "text/xml");
		var messageNode = xmlDoc.getElementsByTagName('message')[0];
		var messageType = messageNode.getAttribute('type');

		// Capture non chat related messages here
		if (messageType == 'redirect') {
			alert("It seems you refreshed the chat page while the websocket was still active. \n"
					+ "Unfortunately, the server now considers you as having logged out and \n"
					+ "you'll have to login again. Sorry.");
			util_redirect(messageNode);
			return;
		} else if (messageType == 'noPermID') {
			sentLeaveChat = true;
			window.onbeforeunload = null; //remove reconnect/leave data
			
			util_noPermID();
			return;
		} else if (messageType == 'checkGroups') {
			// ignore but capture this
			return;
		} else if (messageType == 'lGroupMembers') {
			showUsers(messageNode);
			return;
		}

		//Begin to parse chat messages here

		var textNode; // this thing is the cause of so many issues
		var innerText = '';
		if (xmlDoc.getElementsByTagName('text') !== undefined) { // error handling for textNode
			textNode = xmlDoc.getElementsByTagName('text')[0];
			if (textNode.childNodes[0] !== undefined) {
				// innerText = textNode.childNodes[0].nodeValue;
				innerText = textNode.textContent;

				// Properly REparse & out of text otherwise XML Parser WILL CRASH
				innerText = innerText.replace(/&/gm, '&amp;');
				innerText = innerText.replace(/</gm, '&lt;');
			}
		}

		var senderColor = messageNode.getAttribute('senderColor');
		var senderID = messageNode.getAttribute('senderID');
		var userLabel = messageNode.getAttribute('senderName') + ": ";
		userLabel = '<b style="color:' + senderColor + '">' + userLabel	+ '</b>';

		// Parse a date string and then pull time string from that (sorry no AM/PM)
		var serverTS = messageNode.getAttribute('timestamp');
		var timestamp = serverTS.match(/_(\S+)/)[1];
		timestamp = '<div>' + timestamp + '</div>';

		// var serverTS = new Date();
		// serverTS.setTime(messageNode.getAttribute('timestamp'));
		// var timestamp =
		// serverTS.getHours().pad(2)+":"+serverTS.getMinutes().pad(2)+":"
		// +serverTS.getSeconds().pad(2)+"."+serverTS.getMilliseconds().pad(3);

		var console = document.getElementById('console'); // get console
		var newline = document.createElement('br'); // get break tag
		var chatLineBreak = document.createElement('div');
		chatLineBreak.className = 'chatLine';
		chatLineBreak.style.backgroundColor = senderColor;
		var paragraph = document.getElementById(senderID); // get para if already created

		var chatText = userLabel + innerText + timestamp; // put everything into 1

		if (messageType == 'typing') { // Sender is still typing
				if (paragraph == null) {
					// quit if innerText is ""
					if (innerText == "") return;
					// need to create <p> element to display text in console
					paragraph = document.createElement('p'); // get paragraph tag

					paragraph.innerHTML = chatText;
					paragraph.id = senderID; // label paragraph after the userID
					
					paragraph.style.backgroundColor = TYPING_BGC;
					if (clientID == senderID)
						paragraph.style.backgroundColor = USER_TYPING_BGC;
					
					paragraph.appendChild(newline);
					paragraph.appendChild(chatLineBreak);
					console.appendChild(paragraph); // add to console
				} else if (textNode != undefined
						&& textNode.childNodes[0] === undefined) { 
					// No text in typing msg, remove paragraph from console
					paragraph.innerHTML = '';
					paragraph.id = '';
					console.removeChild(paragraph);
				} else {
					// <p> element already exists to add to that value
					paragraph.innerHTML = chatText;
					paragraph.appendChild(newline);
					paragraph.appendChild(chatLineBreak); //gotta re-add this due to overwriting innerHTML
					
					// place paragraph at the bottom- only move para if its above a chat message (non id)
					if (paragraph.nextElementSibling != null
							&& paragraph.nextElementSibling.id == "") {
						console.removeChild(paragraph);
						console.appendChild(paragraph);
					}
				}
		} else if (messageType == 'chat') {
			// Complete message has been logged - applies to all group members
			// no exceptions like on typing

			// do some fancy emoticon replacement
			for ( var emoUni in EMO_N_ICON) {
				var emoReg = EMO_N_ICON[emoUni];
				if (innerText.search(emoReg) != -1) {
					// window.console.log(innerText +" "+emoReg.toString());
					innerText = innerText.replace(emoReg, ' ' + emoUni);
					chatText = userLabel + innerText + timestamp;
				}
			}

			if (paragraph == null) {
				// means that the user is the broadcaster, so new <p> for chat
				paragraph = document.createElement('p');
				// paragraph.style.color = senderColor;
				paragraph.style.backgroundColor = USER_BGC;
				paragraph.innerHTML = chatText;

				//can also be a chat history broadcast- so set colours if so
				if (messageNode.getAttribute("chatHistory") != undefined) {
					paragraph.style.backgroundColor = CHAT_HISTORY_BGC;
				}

				paragraph.appendChild(newline);
				paragraph.appendChild(chatLineBreak);
				console.appendChild(paragraph);
			} else {	// <p> element already exists
				paragraph.innerHTML = chatText;
				paragraph.id = ''; // remove reference by senderID
				paragraph.style.backgroundColor = 'Snow';
				if (clientID == senderID)
					paragraph.style.backgroundColor = USER_BGC;

				paragraph.appendChild(newline);
				paragraph.appendChild(chatLineBreak); //again chatText overwrite
			}
		} else if (messageType == 'alert') {
			// server message from no user in particular; using console.log
			// For join/leave group, do some fancy formatting on username
			//if (messageNode.getAttribute("chatHistory") != undefined)
			Chat_Alert_Past = messageNode.getAttribute("chatHistory") != undefined;
			var usernameString = messageNode.getAttribute('senderName');
			innerText = innerText.replace(usernameString,
					'<span style="color: ' + senderColor
							+ '; font-weight: bold;" >' + usernameString
							+ '</span>');
			Console.log(innerText + timestamp);
		} else {
			alert('invalid message from server: \n' + xml);
		}

		// Scroll to bottom on console if user not scrolled up
		if (Scroll_To_Bot)
			console.scrollTop = console.scrollHeight;

	};

}); // end of Socket.connect

Chat.sendMessage = function(event) {
	var keycode = event.keyCode || event.which;
	var message = document.getElementById('chat').value;
	//var charCode = evt.keyCode || evt.which;
	//message += String.fromCharCode(keycode);
	//if (keycode!=13)
		//message += String.fromCharCode(keycode);
	
	var chatType = 'typing'; // typing by default
	// If Enter clicked or Chat btn clicked
	// If message is empty, it should be a typing event so we're not
	// broadcasting blank text
	if (message != '' && (keycode == 13 || event.type === 'click')) {
		chatType = 'chat';
		document.getElementById('chat').value = '';
	}

	// Properly parse & out of text otherwise XML Parser WILL CRASH
	message = message.replace(/&/gm, '&amp;');
	message = message.replace(/</gm, '&lt;');

	var xml = '<message type="' + chatType + '" senderID="' + clientID + '">'
			+ '<chat>' + '<text>' + message + '</text>' + '</chat>'
			+ '</message>';
	Chat.socket.send(xml);
};

var Console = {};

// Log is for system messages; get printed verbatim in indigo
Console.log = (function(message) {
	var console = document.getElementById('console');
	var p = document.createElement('p');
	p.innerHTML = message;
	//p.align = "center";
	p.style.color = SERVER_COLOUR;
	if (Chat_Alert_Past)
		p.style.backgroundColor = CHAT_HISTORY_DBGC;
	console.appendChild(p);

	console.scrollTop = console.scrollHeight;
});

// No idea how you'd get this far without JS lol
document.addEventListener("DOMContentLoaded", function() {
	// Remove elements with "noscript" class - <noscript> is not allowed in
	// XHTML
	var noscripts = document.getElementsByClassName("noscript");
	for (var i = 0; i < noscripts.length; i++) {
		noscripts[i].parentNode.removeChild(noscripts[i]);
	}
}, false);

window.onbeforeunload = function () {
	//This is assuming the user has purposely closed the page/refreshed the page.
	//Send a closeSocket message to the server- the server must know that client disconnect on purpose
	//Works on all modern browsers except Chrome on close browser
	if(Chat.socket==null) //If Chat.socket is not loaded, dont run
		return;
	var xml = '<message type="leaveChat" senderID="'+clientID+'"/>';
	Chat.socket.send(xml);
	sentLeaveChat = true;
	console.log("unload");
};

function buttonSend(event) {
	// send message from text field when button is clicked
	Chat.sendMessage(event);
}

function showUsers(messageNode) {
	var userSideBar = document.getElementById('userContainer');
	// clear the whole thing
	while (userSideBar.firstChild) {
		userSideBar.removeChild(userSideBar.firstChild);
	}
	var users = messageNode.getElementsByTagName('member');

	for (var i = 0; i < users.length; i++) {
		var userP = document.createElement('p');
		var userColor = users[i].getAttribute('senderColor');
		var username = users[i].textContent;

		userP.style.color = 'Snow';
		userP.style.backgroundColor = userColor;
		// userP.style.backgroundColor = 'Snow';
		userP.style.borderColor = userColor;
		userP.style.textAlign = 'center';
		userP.className = "username"; // class name but not class??
		userP.innerHTML = "# " + username + " ";

		userSideBar.appendChild(userP);
	}

	// Set Group Name
	var groupNum = messageNode.getAttribute("groupNumber");
	document.getElementById('groupName').innerHTML = "Group " + groupNum;
}


//JS initialization starts here
util_permIDCheck(); //check if this user has an ID from the server

Chat.initialize = util_webSocketConnect;
Chat.initialize();

window.onunload = util_closeSocket();
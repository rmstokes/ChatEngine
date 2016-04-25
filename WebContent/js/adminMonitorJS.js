/**
 * Fairly expansive javascript for chat settings
 */

"use strict";


//Chat color constants
var TYPING_BGC = 'Lavender';
var USER_TYPING_BGC = 'CornSilk';
var USER_BGC = 'LavenderBlush';
var CHAT_HISTORY_BGC = 'LightGoldenRodYellow';
var CHAT_HISTORY_DBGC = 'Beige';
var SERVER_COLOUR = 'Navy';
var AM_MONITOR_BGC = 'rgba(140, 0, 26, 0.7)';
var AM_CHAT_BGC = 'rgba(31, 122, 31, 0.7)';
var AM_NONE_BGC = 'rgba(128, 128, 128, 0.7)';

var AM_UPDATE_TIME = 1.5*1000; //in milliseconds 
var AM_Update_TimeoutID = null;

var clientID = window.name;
var groupPrompt = false;


var Chat = {};
Chat.socket = null;

var host = '';
var Chat_Alert_Past = []; // highlight past chat messages
var Display_Chat = []; //User is monitoring/displaying this 
var GroupType = [];
var Scroll_To_Bot = []; // user scrolling up
//var User_Type_Confirm = [];
var sentLeaveAM = false;
var reconnectAttempt = null;
var reconnectAttemptCounter = 0;
var Global_Group_Num = 0;
var Global_Group_Offset = 0;


//Admin Monitor HTML constants
//Template for group info
var AMGroupInfo = document.getElementById("GXAMGroupInfo").cloneNode(true);

//User Table- the template for 1 user
var AMUser = document.getElementById("GXuserTableUser").cloneNode(true);
//remove user row before copying chat so its not in the chat template
document.getElementById("GXuserTableUser").parentElement.removeChild(document.getElementById("GXuserTableUser"));

//Template for chat window
var AMChat = document.getElementById("GXChat").cloneNode(true);


var serverStatus = document.getElementById('serverStatus');

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
		
		//Wait here so groupUpdate can run first on update
		setTimeout(util_setUserClient(), 1000); 
		serverStatus.innerHTML = "Connected- Awaiting Login";
		serverStatus.style.backgroundColor = "SeaGreen";

		util_changeIcon(true);
	};

	Chat.socket.onclose = function() {
		serverStatus.innerHTML = "Disconnected";
		serverStatus.style.backgroundColor = "DarkGreen";
		clearTimeout(AM_Update_TimeoutID);

		util_changeIcon(false);
		
		//attempt reconnect if disconnected unsuccessfully
		if (!sentLeaveAM) {
			console.log('Info: Trying to reconnect');
			//serverStatus.innerHTML = "Reconnecting...";
			//reconnectAttempt = setInterval(function () {
				reconnectAttemptCounter++;
				if(reconnectAttemptCounter>100) { //tried to reconnect 100 times 
					console.log('Info: Failed to reconnect >100 times. Unable to reach server.');
					return;
				}
				if (Chat.socket.readyState>1) //Not open or connecting
					setTimeout(Chat.initialize(), 1000); //wait 1 second
					//Chat.initialize();
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
			//alert('Redirect captured- retry joinChat');
			util_redirect(messageNode);
			return;
		} else if (messageType == 'noPermID') {
			util_noPermID();
			return;
		} else if (messageType == 'checkGroups') {
			updateGroupInfo(messageNode); //update the group info on the page
			return;
		} else if (messageType == 'lGroupMembers') {
			//showUsers(messageNode);
			//adminMonitor has a more advanced version of this, capture and ignore
			return;
		} else if (messageType == 'AMStatus') {
			updateServerStatus(messageNode);
			return;
		} else if (messageType == 'groupStatData') {
			updateGroupStats(messageNode);
			return;
		} else if (messageType == 'alertWebPage') {
			console.log(xmlDoc.textContent);
			alert(xmlDoc.textContent);
			return
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
		var groupID = messageNode.getAttribute('groupNumber');
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

		var console = document.getElementById('console'+groupID); // get console
		var newLine = document.createElement('br'); // get break tag
		var chatLineBreak = document.createElement('div');
		chatLineBreak.className = 'chatLine';
		chatLineBreak.style.backgroundColor = senderColor;
		var paragraph = document.getElementById(senderID); // get para if already created
		//Special condition for Admin Monitor since a user can type/chat in different groups and should be overwrit
		if (paragraph!=null && paragraph.parentElement!=console) //if this paragraph is not in the same console/group, make new one
			paragraph = null; //doesnt resolve multiple typing in different windows

		var chatText = userLabel + innerText + timestamp; // put everything into 1

		if (messageType == 'typing') { // Sender is still typing
			//if (clientID != senderID) {
				// Do nothing if client recieved its own info since its already in the chatbox
				if (paragraph == null) {
					// quit if innerText is ""
					if (innerText == "") return;
					// need to create <p> element to display text in console
					paragraph = document.createElement('p'); // get paragraph tag

					// paragraph.style.wordWrap = 'break-word';
					paragraph.innerHTML = chatText;
					paragraph.id = senderID; // label paragraph after the userID
					paragraph.align = "left";
					paragraph.style.backgroundColor = TYPING_BGC;
					if (clientID == senderID)
						paragraph.style.backgroundColor = USER_TYPING_BGC;
					
					paragraph.appendChild(newLine);
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
					paragraph.appendChild(newLine);
					paragraph.appendChild(chatLineBreak); //gotta re-add this due to overwriting innerHTML
					
					// place paragraph at the bottom- only move para if its above a chat message (non id)
					if (paragraph.nextElementSibling != null
							&& paragraph.nextElementSibling.id == "") {
						console.removeChild(paragraph);
						console.appendChild(paragraph);
					}
				}
			//} // close !senderID
			/*else { // show confirmation that user is sending text
				if (document.getElementById(clientID) == null) {
					var userConfirm = document.createElement('p');
					userConfirm.text = 'User is typing';
					document.getElementById("console-container").appendChild(userConfirm);
					User_Type_Confirm = setInterval(
							function () {
								
							}, 1000);
				}
			}*/
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
				paragraph.align = "left";
				paragraph.style.backgroundColor = USER_BGC;
				paragraph.innerHTML = chatText;

				//can also be a chat history broadcast- so set colours if so
				if (messageNode.getAttribute("chatHistory") != undefined) {
					paragraph.style.backgroundColor = CHAT_HISTORY_BGC;
				}

				paragraph.appendChild(newLine);
				paragraph.appendChild(chatLineBreak);
				console.appendChild(paragraph);
			} else {	// <p> element already exists
				paragraph.innerHTML = chatText;
				
				paragraph.id = ''; // remove reference by senderID
				paragraph.style.backgroundColor = 'Snow';
				
				if (clientID == senderID)
					paragraph.style.backgroundColor = USER_BGC;
				
				paragraph.appendChild(newLine);
				paragraph.appendChild(chatLineBreak); //again chatText overwrite
			}
		} else if (messageType == 'alert') {
			// server message from no user in particular; using console.log
			// For join/leave group, do some fancy formatting on username
			if (messageNode.getAttribute("chatHistory") != undefined)
				Chat_Alert_Past = true;
			var usernameString = messageNode.getAttribute('senderName');
			innerText = innerText.replace(usernameString,
					'<span style="color: ' + senderColor
							+ '; font-weight: bold;" >' + usernameString
							+ '</span>');
			//var console = document.getElementById('console');
			var p = document.createElement('p');
			p.innerHTML = innerText;
			p.style.color = SERVER_COLOUR;
			if (Chat_Alert_Past)
				p.style.backgroundColor = CHAT_HISTORY_DBGC;
			console.appendChild(p);

			console.scrollTop = console.scrollHeight;
		} else {
			alert('invalid message from server: \n' + xml);
		}

		// Scroll to bottom on console if user not scrolled up
		var gID = parseInt(groupID);
		if (Scroll_To_Bot[groupID-Global_Group_Offset-1])
			console.scrollTop = console.scrollHeight;

	};

}); // end of Socket.connect

Chat.sendMessage = function(event, groupID) {
	var keycode = event.keyCode;
	var message = document.getElementById('chat'+groupID).value;
	var chatType = 'typing'; // typing by default
	// If Enter clicked or Chat btn clicked
	// If message is empty, it should be a typing event so we're not
	// broadcasting blank text
	if (message != '' && (keycode == 13 || event.type === 'click')) {
		chatType = 'chat';
		document.getElementById('chat'+groupID).value = '';
	}

	// Properly parse & out of text otherwise XML Parser WILL CRASH
	// Parsing some invalid Unicode from text string to prevent crashes
	message = message.replace(/&/gm, '&amp;');
	message = message.replace(/</gm, '&lt;');

	var xml = '<message type="' + chatType + '" senderID="' + clientID + '" groupNumber="'+groupID+'">'
			+ '<chat>' + '<text>' + message + '</text>' + '</chat>'
			+ '</message>';
	Chat.socket.send(xml);
};

var Console = {};

// Log is for system messages; get printed verbatim in indigo
Console.log = (function(message) {
	var console = document.getElementById('console');
	var p = document.createElement('p');
	// p.style.wordWrap = 'break-word';
	p.innerHTML = message;
	p.style.color = SERVER_COLOUR;
	if (Chat_Alert_Past)
		p.style.backgroundColor = CHAT_HISTORY_DBGC;
	else
		p.style.backgroundColor = 'White';
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

function buttonSend(event) {
	// send message from text field when/button enter is pressed
	var groupID = event.currentTarget.id;
	if (event.currentTarget.tagName=="button")
		groupID = parseInt(groupID.slice(11)); //remove 'chat_button' from string
	else
		groupID = parseInt(groupID.slice(4)); //remove 'chat' from string
	
	//console.log("Sent to group "+groupID);
	Chat.sendMessage(event, groupID);
}

function AMLogin() {
	var usernameText = document.getElementById('usernameAM').value;
	if (usernameText=="") {
		alert("Username is empty!");
		return;
	}
	var xml = '<message type="adminMonitorLogin"  senderID="' + clientID + '">'
				+usernameText+
				'</message>';
	Chat.socket.send(xml);
	
}

//This is set on a recursive timeout of X sec until DisplayChat is empty
function AMUpdate() {
	for (var i=0; i<Display_Chat.length; i++) {
		if (!Display_Chat[i])
			continue;
		
		var xml = '<message type="adminMonitorUpdate"  senderID="' + clientID + '">'
			+""+
			'</message>';
		Chat.socket.send(xml);
		AM_Update_TimeoutID = setTimeout(AMUpdate, AM_UPDATE_TIME); //set new timer
		break;
	}
}

//Function runs to initialize group info
function updateGroupInfo(messageNode) {
	//clear the first table of values
	var groupMonitorTable = document.getElementById('AMGroupTable');
	//remove every row except the header
	if (groupMonitorTable.lastElementChild != groupMonitorTable.children[1]) //caption counts as a child
		groupMonitorTable.removeChild(groupMonitorTable.lastElementChild);

	//check groups created
	var checkGroups = messageNode.getAttribute('checkGroups');
	if (checkGroups == 'Not Created') {
		alert("There are no groups created currently.");
		document.getElementById("loginBtn").disabled = true;
		groupPrompt = true;
	} else {
		if (groupPrompt)
			alert("Groups have been created/updated!");
		
		//add rows based on groupInfo
		var groupNum = Number(checkGroups);
		var groupOff = Number(messageNode.getAttribute('groupOffset'));

		Global_Group_Num = groupNum;
		Global_Group_Offset = groupOff;
		
		groupNum += groupOff;
		
		var groupTable = document.getElementById('AMGroupTable');
		for (var i=groupOff; i<groupNum; i++) {
			var groupInfo = AMGroupInfo.cloneNode(true); //clone 
			groupTable.appendChild(groupInfo); 
			groupInfo.id = "AMGroupInfo"+(i+1);
			groupInfo.cells[0].textContent = "Group "+(i+1);
		}

		document.getElementById("loginBtn").disabled = false;
	}
	
	//Set up background variables to begin to set up the system
	//Use dynamic javascript array length here
	for (var i=0; i<Global_Group_Num; i++) {
		Display_Chat[i] = 0; //Not monitoring, not chatting
		Scroll_To_Bot[i] = true;
		GroupType[i] = 0;
	}
	
}

//Sending changes to AM to server
function updateAMStatus(event) {
	var justChanged = event.target; //the checkbox that just changed
	var groupTable = document.getElementById('AMGroupTable');
	
	var xml = '<message type="adminMonitorStatus" senderID="'+clientID+'">';
	
	for(var i=0; i<Global_Group_Num; i++) {
		var groupInfo = document.getElementById('AMGroupInfo'+(i+Global_Group_Offset+1));
		var groupM = groupInfo.cells[1].firstElementChild;
		var groupC = groupInfo.cells[2].firstElementChild;
		groupM.disabled = false;
		groupC.disabled = false;
		if (groupM.checked && groupC.checked) { //both cant be true at the same time
			if (groupM==justChanged)
				groupC.checked = false;
			else if (groupC==justChanged)
				groupM.checked = false;
			else
				groupC.checked = false; //default monitor
		}
		//add to xml
		xml += '<groupAMInfo monitor="'+groupM.checked+'" chat="'+groupC.checked+'" />';
	}
	
	xml += '</message>';
	//Now send the updated info to the server
	Chat.socket.send(xml);
}

//Update based on server confirmation
function updateServerStatus(messageNode) {
	var username = messageNode.getAttribute('senderName');
	document.getElementById('usernameAM').value = username;
	username = "User: ["+username+"] status up to date.";
	serverStatus.textContent = username;
	serverStatus.style.backgroundColor = "RoyalBlue";
	
	var groupOff = Number(messageNode.getAttribute('groupOffset'));
	var groupNum = Number(messageNode.getAttribute('groupNum'));
	
	for (var i=0; i<groupNum; i++) {
		var groupInfoRow = document.getElementById("AMGroupInfo"+(i+groupOff+1));
		var groupInfo = messageNode.getElementsByTagName('groupAMInfo')[i];
		var groupMonitor = groupInfo.getAttribute('monitor')=='true';
		var groupChat = groupInfo.getAttribute('chat')=='true';
		
		groupInfoRow.cells[1].firstElementChild.checked = false;
		groupInfoRow.cells[2].firstElementChild.checked = false;
		if (groupMonitor) {
			groupInfoRow.cells[1].firstElementChild.checked = true;
			groupInfoRow.cells[3].textContent = "Monitor";
			groupInfoRow.cells[3].style.backgroundColor = AM_MONITOR_BGC;
		} else if (groupChat) {
			groupInfoRow.cells[2].firstElementChild.checked = true;
			groupInfoRow.cells[3].textContent = "Chat";
			groupInfoRow.cells[3].style.backgroundColor = AM_CHAT_BGC;
		} else {
			groupInfoRow.cells[3].textContent = "None";
			groupInfoRow.cells[3].style.backgroundColor = AM_NONE_BGC;
		}
		groupInfoRow.cells[1].firstElementChild.disabled = false;
		groupInfoRow.cells[2].firstElementChild.disabled = false;
		
		Display_Chat[i] = groupMonitor || groupChat;
		GroupType[i] = (groupMonitor) ? 1 : 0;
		GroupType[i] = (groupChat) ? 2 : GroupType[i];
	}
	
	//update chat display
	updateAMChat();
	if (AM_Update_TimeoutID!=null)
		clearTimeout(AM_Update_TimeoutID);
	AM_Update_TimeoutID = setTimeout(AMUpdate);
	
}


function updateGroupStats(messageNode) {
	//Parses and places the statistics at the bottom of the chatBox
	var groupStatArr = messageNode.getElementsByTagName('groupStat');
	
	for(var i=0; i<groupStatArr.length; i++) {
		var groupStat = groupStatArr[i];
		var groupID = parseInt(groupStat.getAttribute('groupNumber'));
		
		var userTable = document.getElementById('userTable'+groupID);
		while (userTable.firstElementChild!=userTable.lastElementChild) //remove all except header
			userTable.removeChild(userTable.lastElementChild);
		
		var memList = groupStat.getElementsByTagName('memberStat');
		for(var j=0; j<memList.length; j++) {
			var userStat = AMUser.cloneNode(true);
			userStat.children[0].textContent = memList[j].getAttribute("username");
			userStat.children[1].textContent = memList[j].getAttribute("typed");
			userStat.children[2].textContent = memList[j].getAttribute("chatted");
			userStat.children[3].textContent = memList[j].getAttribute("participation")+"%";
			userTable.appendChild(userStat);
		}
		var total = groupStat.getElementsByTagName('totalStat')[0];
		var totalUser = AMUser.cloneNode(true);
		totalUser.children[0].textContent = 'Total';
		totalUser.children[1].textContent = total.getAttribute("typed");
		totalUser.children[2].textContent = total.getAttribute("chatted");
		totalUser.children[3].innerHTML = '&#x221e;'; //Infinity symbol
		userTable.appendChild(totalUser);
	}
}

function updateAMChat() {
	//check display_chat, add if there, remove if not
	
	for (var i=0; i<Display_Chat.length; i++) {
		var chatBox = document.getElementById('AMChatBox'+(i+Global_Group_Offset+1));
		var groupId = (i+Global_Group_Offset+1);
		if (Display_Chat[i]) {
			if (chatBox==null) {
				chatBox = AMChat.cloneNode(true);
				chatBox.id = 'AMChatBox'+(i+Global_Group_Offset+1);
				chatBox.getElementsByTagName('h2')[0].textContent = 'Group '+(i+Global_Group_Offset+1);
				chatBox.getElementsByClassName('chatConsole')[0].id = 'console'+(i+Global_Group_Offset+1);
				chatBox.getElementsByClassName('chat')[0].id = 'chat'+(i+Global_Group_Offset+1);
				//chat button takes the groupNumber as a param
				chatBox.getElementsByTagName('button')[0].onclick = function(event) { buttonSend(event)};
				chatBox.getElementsByTagName('button')[0].id = "chat_button"+groupId;
				chatBox.getElementsByTagName('table')[0].id = 'userTable'+(i+Global_Group_Offset+1);
				getChatRow().appendChild(chatBox);
			}
			
			//Anti scroll tech for both
			document.getElementById('console'+groupId).onscroll = function(event) {
				// if scrolled up, freeze the scroll bar
				var groupIDName = parseInt(event.currentTarget.id.slice(7)); //take off the console
				var consoleC = document.getElementById('console'+groupIDName);
				var scrollCoef = consoleC.scrollTopMax - consoleC.scrollTop;
				if (isNaN(scrollCoef)) //chrome stuff
					scrollCoef = consoleC.scrollHeight - consoleC.scrollTop - consoleC.clientHeight;
				Scroll_To_Bot[groupIDName-Global_Group_Offset-1] = !(scrollCoef > 100); // average p is about 22px
			}
			
			//change colour
			if (GroupType[i]==1) { //Monitoring
				chatBox.style.backgroundColor = AM_MONITOR_BGC;
				var pNum = chatBox.getElementsByTagName('p').length;
				chatBox.getElementsByTagName('p')[pNum-1].style.backgroundColor = 'GRAY';
				chatBox.getElementsByTagName('button')[0].disabled = true;
				document.getElementById('chat'+groupId).placeholder = "Chat is disabled while monitoring";
				document.getElementById('chat'+groupId).disabled = true;
				//Disable chat message
			} else if (GroupType[i]==2) {
				chatBox.style.backgroundColor = AM_CHAT_BGC;
				var pNum = chatBox.getElementsByTagName('p').length;
				chatBox.getElementsByTagName('p')[pNum-1].style.backgroundColor = 'inherit';
				chatBox.getElementsByTagName('button')[0].disabled = false;
				document.getElementById('chat'+groupId).placeholder = "Type and press Enter to chat";
				document.getElementById('chat'+groupId).disabled = false;
				
				document.getElementById('chat'+groupId).onkeyup = function(event) {
					buttonSend(event);
				};
				//put anti-scroll tech
				/*document.getElementById('console'+groupId).onscroll = function(event) {
					// if scrolled up, freeze the scroll bar
					var groupIDName = parseInt(event.currentTarget.id.slice(7)); //take off the console
					var consoleC = document.getElementById('console'+groupIDName);
					var scrollCoef = consoleC.scrollTopMax - consoleC.scrollTop;
					if (isNaN(scrollCoef)) //chrome stuff
						scrollCoef = consoleC.scrollHeight - consoleC.scrollTop - consoleC.clientHeight;
					Scroll_To_Bot[groupIDName-Global_Group_Offset-1] = !(scrollCoef > 100); // average p is about 22px
				}*/
				//remove all chatHistory
				//var chatConsole = document.getElementById('console'+(i+Global_Group_Offset+1));
				//while (chatConsole.firstElementChild!=null)
					//chatConsole.removeChild(chatConsole.firstElementChild);
				
			}
			
		} else if (!Display_Chat[i]) {
			if (chatBox!=null) 
				chatBox.parentElement.removeChild(chatBox);
			
		}
	}
}

function getChatRow() {
	if (Chat_Row_One.childElementCount <3)
		return Chat_Row_One;
	else if (Chat_Row_Two.childElementCount <3)
		return Chat_Row_Two;
	else if (Chat_Row_Three.childElementCount <3)
		return Chat_Row_Three;
	else 
		return Chat_Row_Four;
}


//JS Entry point
document.getElementById('usernameAM').onkeyup = function(e) {if(e.keyCode==13) AMLogin()};
util_permIDCheck(); //check if this user has an ID from the server

Chat.initialize = util_webSocketConnect;
Chat.initialize();

window.onunload = util_closeSocket();

var Chat_Row_One = document.getElementById("chatRow1");
var Chat_Row_Two = document.getElementById("chatRow2");
var Chat_Row_Three = document.getElementById("chatRow3");
var Chat_Row_Four = document.getElementById("chatRow4");
Chat_Row_One.removeChild(document.getElementById("GXChat"));
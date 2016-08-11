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

//SenderID and ClientID
var clientID = window.name;

var Chat = {};
Chat.socket = null;

var host = '';
var Scroll_To_Bot = true; // user scrolling up
//var User_Type_Confirm = null;

var sentLeaveChat = false; //user left of his own accord
var reconnectAttempt = null;
var reconnectAttemptCounter = 0;

var swapPanel = 0;
var lastAnswerEdit = 0;
var groupPrompt = false;
var SetReset = false; //if user has logged in, go to landingPage (closes webSocket)
var AnswerStatus = false;

util_openSocket(); //open the webSocket

	Chat.socket.onopen = function() {
		$ ("#clientGfx").removeClass().addClass("circleBase activeWS");
		$ ("#serverGfx").removeClass().addClass("circleBase activeWS");
		$ ("#pingGfx").removeClass().addClass("circleBase ping pingPong");
		util_affirmUserClient();
		console.log("WebSocket was opened");
		
		$("#chatInput").val("");
		$("#chatInput")[0].oninput = sendChat;
		$("#chatInput")[0].onkeydown = sendChat;
		
		$("#answerInput")[0].oninput = sendAnswer;
		
		//clear chat window for reconnect
		$(".chatConsole p").remove(); //remove all messages
	};

	Chat.socket.onclose = function() {
		$ ("#clientGfx").removeClass().addClass("circleBase");
		$ ("#serverGfx").removeClass().addClass("circleBase");
		$ ("#pingGfx").removeClass().addClass("circleBase ping");
		console.log("WebSocket was closed.");
		
		if (!sentLeaveChat) { //did not leave purposely
			//console.log("Attempting reconnect in 3s");
			$("#serverGfx").addClass("disconnectWS");
			$("#pingGfx").addClass("reconnect");
			
			setTimeout(util_reconnectSocket, 1000); //reconnect after 1s
			//util_reconnectSocket(); //self resolving reconnect
		}
	};

	Chat.socket.onmessage = function(message) {

		var xml = message.data;
		var parser = new DOMParser();
		var xmlDoc = parser.parseFromString(xml, "text/xml");
		var messageNode = xmlDoc.getElementsByTagName('message')[0];
		var messageType = messageNode.getAttribute('type');

		// Capture non chat related messages here
		
		if (messageType == 'permIDSet' || messageType == 'permIDConfirm') {
			console.log(messageType+" "+messageNode.getAttribute('senderID'));
			util_setPermID(messageNode.getAttribute('senderID'));
			return;
		} else if (messageType=='groupInfo') {
//			if (SetReset) {
//				alert("This Set of groups have expired. This page will now reset.");
//				util_webRedirect("landingPage");
//				return;
//			}
			if (messageNode.getAttribute("setStatus")=="FALSE") {
				$("#joinBtn").prop("disabled", true);
				$("#joinBtn").text("Groups Not Created");
				groupPrompt = true;
				//console.log();
			} else {
				if (groupPrompt)
					alert("Groups have been created/updated!");
				
				$("#joinBtn").prop("disabled", false);
				$("#joinBtn").text("Join");
				
				$("#joinBtn").click(loginGroup);
				$("#firstName").keydown(loginShortcut);
				$("#lastName").keydown(loginShortcut);
				
				var groupSelect = $("#groupSelection")[0];
				var groupTotal = Number(messageNode.getAttribute("groupTotal"));
				var groupOffset = Number(messageNode.getAttribute("groupOffset"));
				
				while(groupSelect.length > 0) 
					groupSelect.remove(0);
				
				for (var i=groupOffset; i<(groupTotal+groupOffset); i++) {
					var groupOption = document.createElement('option');
					groupOption.text = "Group "+(i+1);
					groupOption.value = (i+1);
					groupSelect.add(groupOption);
				}
			}
			
			return;
		} else if (messageType == 'displayChat') {
			//successful login, go to info page
			//SetReset = true;
			changePanel();
			return;
		} else if (messageType == 'goToChat') {
			//during reconnect, force to chat window
			goToChat();
			return;
		} else if (messageType == 'setClose') {
			//set was closed, send to landing page
			alert("This Group has expired. The page will now reset.");
			util_webRedirect("landingPage");
			return;
		} else if (messageType == 'lGroupMembers') {
			var sidebar = $("#sidebar")[0];
			
			while (sidebar.childElementCount>1)
				sidebar.removeChild(sidebar.lastElementChild);
			
			var users = messageNode.getElementsByTagName('member');
			$("#groupName").text("Group "+messageNode.getAttribute('groupID'));
			
			for (var i=0; i<users.length; i++) {
				var userP = document.createElement('p');
				userP.style.backgroundColor = users[i].getAttribute('senderColor');
				userP.style.borderColor = users[i].getAttribute('senderColor');
				userP.textContent = users[i].textContent;
				userP.id = "sidebar"+users[i].getAttribute('senderID');
				sidebar.appendChild(userP);
			}
			var sidebarOffset = sidebar.scrollWidth-25;
			//console.log(sidebarOffset);
			$("#sidebar").addClass("show");
			setTimeout( function () {
				$("#sidebar").removeClass("show");
				//console.log("removing");
			}, 1500);
			sidebar.style.transform = "translateX(-"+sidebarOffset+"px)";
//			showUsers(messageNode);
			return;
		} else if (messageType=='answerType') {
			var answerText = xmlDoc.getElementsByTagName('text')[0].textContent;
			$("#answerPara").text(answerText);
			var senderID = messageNode.getAttribute('senderID');
			var chatHistory = messageNode.getAttribute('chatHistory') == "chatHistory";
			if (senderID != clientID && !chatHistory) {
				//lockout user from answer
				var senderName = messageNode.getAttribute('senderName');
				var senderColor = messageNode.getAttribute('senderColor');
				$("#answerInput").prop("disabled", true);
				$("#answerInput").val(answerText);
				$("#lockedPara").css("opacity", 1);
				$("#lockedPara").text("Locked by "+senderName);
				$("#lockedPara").css("background-color", senderColor);
				
				lastAnswerEdit = new Date().getTime();
				
				setTimeout(function () {
					var time = new Date().getTime();
					if(time-lastAnswerEdit>2900) {
						$("#answerInput").prop("disabled", false);
						$("#lockedPara").css("opacity", 0);
					}
				}, 3000);
			}
			return;
		} else if (messageType =='AnswerGroupStatus') {
			$("#answerPara").text(xmlDoc.getElementsByTagName('answer')[0].textContent);
			$("#answerInput").val(xmlDoc.getElementsByTagName('answer')[0].textContent);
			$("#prevPara").text(xmlDoc.getElementsByTagName('prevAnswer')[0].textContent);
			
			var answerLock = messageNode.getAttribute("answerLock")=="true";
			
			if (!answerLock) {
				$("#lockedPara").css("opacity", 0);
				$("#submitBtn").text("Submit").removeClass();
				$("#answerInput").prop("disabled", false);
			} else {
				$("#lockedPara").css("opacity", 1).css("background-color", "Green")
					.text("Answer is awaiting review");
				$("#submitBtn").text("Withdraw").removeClass().addClass("withdrawBtn");
				$("#answerInput").prop("disabled", true);
			}
			
			var members = messageNode.getElementsByTagName('member');
			for (var i=0; i<members.length; i++) {
				var sidebarP = $("#sidebar"+members[i].getAttribute("senderID"));
				var memStatus = members[i].textContent=="true";
				sidebarP.removeClass();
				
				if (memStatus && !answerLock)
					sidebarP.addClass("whiteBlink");
				else if (memStatus && answerLock)
					sidebarP.addClass("redWhiteBlink");
				
				//changes only to user
				if (clientID == members[i].getAttribute("senderID")) {
					AnswerStatus = memStatus;
					if (memStatus) {
						if (!answerLock)
							$("#submitBtn").text("XSubmit")
								.removeClass().addClass("cancelBtn");
						else
							$("#submitBtn").text("XWithdraw")
								.removeClass().addClass("cancelBtn");
					}
				}
			}
			return;
		} else if (messageType!='typing' && messageType!='chat' && messageType!='alert') {
			console.log("unparsable: "+xml);
			return;
		}
		
		//****************************************************
		//Begin to parse chat messages here
		//****************************************************
		var chatConsole = document.getElementById('chatConsole');
		chat_processMessage(messageNode, messageType, chatConsole);
		
		/*
		var textNode; // this thing is the cause of so many issues
		var innerText = '';
		if (messageNode.getElementsByTagName('text') !== undefined) { // error handling for textNode
			textNode = messageNode.getElementsByTagName('text')[0];
			if (textNode !== undefined && textNode.childNodes[0] !== undefined) {
				innerText = textNode.textContent;

				//Properly REparse & out of text otherwise XML Parser WILL CRASH
				innerText = innerText.replace(/&/gm, '&amp;');
				innerText = innerText.replace(/</gm, '&lt;');
			}
		}
		innerText = chat_replaceEmote(innerText);

		var senderColor = messageNode.getAttribute('senderColor');
		var senderID = messageNode.getAttribute('senderID');
		var userLabel = messageNode.getAttribute('senderName') + ": ";
		var groupID = messageNode.getAttribute('groupNumber');
		userLabel = '<strong style="color:' + senderColor + '">' + userLabel	+ '</strong>';

		// Parse a date string and then pull time string from that (sorry no AM/PM)
		var serverTS = messageNode.getAttribute('timestamp');
		var timestamp = serverTS.match(/_(\S+)/)[1];
		timestamp = '<div>' + timestamp + '</div>';	

		var chatConsole = document.getElementById('chatConsole'); // get console
		var newline = document.createElement('br'); // get break tag
		var chatLineBreak = document.createElement('div');
		chatLineBreak.className = 'chatLine';
		chatLineBreak.style.backgroundColor = senderColor;
		var paragraph = document.getElementById(senderID+"l"+groupID); // get para if already created

		var chatText = userLabel + innerText + timestamp; // put everything into 1

		if (paragraph == null && innerText!="") {
			paragraph = document.createElement('p');
			//paragraph.innerHTML = chatText;
			paragraph.id = senderID+"l"+groupID;
			//paragraph.className = "stuff";
			//paragraph.appendChild(newline);
			//chatLineBreak.style.backgroundColor = senderColor;
			//paragraph.appendChild(chatLineBreak);
			chatConsole.appendChild(paragraph);
		}
		
		if (messageType == 'typing') {
			if (innerText=="" && paragraph!=null) { //Remove this paragraph
				paragraph.id = '';
				chatConsole.removeChild(paragraph);
			} else if (innerText!="") {
				paragraph.innerHTML = chatText; //changing innerHTML overwrites all childs
				paragraph.appendChild(newline);
				paragraph.appendChild(chatLineBreak);
				
				if (clientID==senderID)
					paragraph.className = "typingUserMessage";
				else
					paragraph.className = "typingMessage";
				
				if (paragraph.nextElementSibling != null
						&& chatConsole.lastElementChild.id == "") {
					chatConsole.removeChild(paragraph);
					chatConsole.appendChild(paragraph);
				}
			}
			
		} else if (messageType == 'chat') {
			paragraph.id = ''; //remove id reference
			paragraph.innerHTML = chatText; //changing innerHTML overwrites all childs
			paragraph.appendChild(newline);
			paragraph.appendChild(chatLineBreak);
			

			if (messageNode.getAttribute("chatHistory")!=undefined)
				paragraph.className = "chatHistoryMessage";
			else if (clientID==senderID)
				paragraph.className = "chatUserMessage";
			else 
				paragraph.className = "";
			
		} else if (messageType == 'alert') {
			var usernameString = messageNode.getAttribute('senderName');
			innerText = innerText.replace(usernameString,
					'<span style="color: ' + senderColor
							+ '; font-weight: bold;" >' + usernameString
							+ '</span>');
			
			innerText = util_chatHighlightAlert(innerText);
			innerText += timestamp;
			
			paragraph.id = '';
			paragraph.innerHTML = innerText;
			paragraph.className = "serverMessage";
			if (messageNode.getAttribute("chatHistory")!=undefined)
				paragraph.className += " chatHistoryMessage";
			paragraph.appendChild(newline);
			chatLineBreak.style.backgroundColor = SERVER_COLOUR;
			paragraph.appendChild(chatLineBreak);
		}
		
		*/
		// Scroll to bottom on console if user not scrolled up
		var scrollCoef = chatConsole.scrollHeight - chatConsole.scrollTop - chatConsole.clientHeight;
		var scrollMax = chatConsole.scrollHeight - chatConsole.clientHeight;
		
		if (Scroll_To_Bot && scrollCoef!=0) {
			$("#chatConsole")[0].scrollTop += scrollCoef;
//			$("#chatConsole").animate({scrollTop: scrollMax }, "slow");
		}

	};

function sendChat(event) {
	var keycode = event.keyCode || event.which;
	var message = $('#chatInput').val();
	
	var chatType = 'typing'; // typing by default
	// If Enter clicked or Chat btn clicked
	// If message is empty, it should be a typing event so we're not broadcasting blank text
	if (event.type!='input' && message != '' && (event.type === 'click' || keycode == 13)) {
		chatType = 'chat';
		$('#chatInput').val("");
	} else if (event.type!='input'){
		//console.log("typing with no change");
		return;
	}

	// Properly parse & out of text otherwise XML Parser WILL CRASH
	message = message.replace(/&/gm, '&amp;');
	message = message.replace(/</gm, '&lt;');

	var xml = '<message type="' + chatType + '" senderID="' + clientID + '">'
			+ '<chat>' + '<text>' + message + '</text>' + '</chat>'
			+ '</message>';
	
	//console.log("WS buffer: "+Chat.socket.bufferedAmount);
	Chat.socket.send(xml);
};

function sendAnswer(event) {
	var message = $('#answerInput').val();

	// Properly parse & out of text otherwise XML Parser WILL CRASH
	message = message.replace(/&/gm, '&amp;');
	message = message.replace(/</gm, '&lt;');
	
	var xml = '<message type="answerType" senderID="' + clientID + '">'
		+ '<chat>' + '<text>' + message + '</text>' + '</chat>'
		+ '</message>';
	Chat.socket.send(xml);
}

function changePanel () {
//	console.log("swappedPanels "+swapPanel);
	if (swapPanel==0) {
		$("#loginDiv").removeClass("centerDiv").addClass("offLeftDiv");
		$("#infoDiv").removeClass("offRightDiv").addClass("centerDiv");
		$("#chatDiv").removeClass().addClass("offRightDiv");
		$("#sidebar").css("opacity", "1");
		$("#bodyDiv").css("overflow-y", "scroll");
		$("#bodyDiv").animate({scrollTop: 0}, "slow");
		swapPanel = 1;
	} else if (swapPanel==1) {
		$("#infoDiv").removeClass("centerDiv").addClass("offLeftDiv");
		$("#chatDiv").removeClass("offRightDiv").addClass("centerDiv");
		$("#bodyDiv").css("overflow-y", "hidden");
//		$("#bodyDiv")[0].scrollTo(0,0);
		$("#bodyDiv").animate({scrollTop: 0}, "slow");
		swapPanel = 2;
	} else {
//		$("#loginDiv").removeClass().addClass("centerDiv");
		$("#infoDiv").removeClass().addClass("centerDiv");
		$("#chatDiv").removeClass().addClass("offRightDiv");
		$("#bodyDiv").css("overflow-y", "scroll");
		$("#bodyDiv").animate({scrollTop: 0}, "slow");
		swapPanel = 1;
	}
}

function goToChat () {
	while (swapPanel!=2)
		changePanel();
}

function loginShortcut (event) {
	var keycode = event.keyCode || event.which;
	if (keycode==13)
		loginGroup();
}
function loginGroup () {
	var groupNum = $("#groupSelection").val();
	var username = $("#firstName").val() + " "+$("#lastName").val();
	username = $.trim(username);
	if (username=="") {
		alert("Username is blank!");
		return;
	}
	
	var xml = '<message type="joinGroup" senderID="' + clientID
		+ '" username="' + username + '"><text>' + groupNum
		+ '</text></message>';
	$("#joinBtn").prop("disabled", true);
	$("#joinBtn").text("...");
	
	Chat.socket.send(xml);
}

function submitAnswer (event) {
	var status = !AnswerStatus;
	
	var xml = '<message type="answerStatus" senderID="' + clientID
	+ '" status="' + status + '">'
	+ '</message>';
	
	Chat.socket.send(xml);
}

function getKeyPress (event) {
	console.log("key="+event.key+" which="+event.which);
}

function manageTextChange (event) {
	console.log("text="+event.target.value);
}

function captureTab (event) {	
//Very shamelessly copied from StackOverflow kasdega at
//http://stackoverflow.com/questions/6637341/use-tab-to-indent-in-textarea
//But in my defense, I would have used a similar implementation, just would have taken longer
	var keyCode = event.keyCode || event.which;
	var et = event.currentTarget;

	  if (keyCode == 9) {
	    event.preventDefault();
	    var start = $(et).get(0).selectionStart;
	    var end = $(et).get(0).selectionEnd;

	    // set textarea value to: text before caret + tab + text after caret
	    $(et).val($(et).val().substring(0, start)
	                + "    "        //+ "\t"  //adding 4 spaces due to problems with parsing tabs in logs?
	                + $(et).val().substring(end));

	    // put caret at right position again
	    $(et).get(0).selectionStart =
	    	$(et).get(0).selectionEnd = start+4;// + 1;
	    $(et).trigger("input"); //tab not caught by input since programmable
	  }
}

window.onbeforeunload = function () {
	//This is assuming the user has purposely closed the page/refreshed the page.
	//Send a closeSocket message to the server- the server must know that client disconnect on purpose
	if(Chat.socket==null) //If Chat.socket is not loaded, dont run
		return;
	
	//spoof chat event to convert typing event to chat
	var fakeBtnEvent = {type:"click"};
	sendChat(fakeBtnEvent);
	
	var xml = '<message type="leaveChat" senderID="'+clientID+'"/>';
	Chat.socket.send(xml);
	sentLeaveChat = true;
	console.log("beforeunload has run");
};

//JS initialization starts here

//So the mythical solution to prevent Chrome throwing errors on tab close/browser close
//is to run util_closeSocket on exit, which forces the browser to send WS close msg? I dunno
//I'm not sniffing packets or anything. If the browser crashes, it still errors but I'm fine with this
window.onunload = util_closeSocket;

var specialStr = "";
document.onkeypress = function (event) {
	specialStr = (event.keyCode || event.which) + specialStr;
	specialStr = specialStr.substr(0, "131221203937393740403838".length);
	//console.log(specialStr);
	if (specialStr == "131221203937393740403838") {
		console.log("Reconnect code activated");
		//Its the konami code obviously- Adrian Anyansi
		Chat.socket.close();
	}
	
	
};


$("#loginDiv").css("transition", "left 2s, transform 2s");
//$("#loginDiv").one("transitionend", function () {
//	$("#loginDiv").css("transition", "left 2s, transform 2s");
//	console.log("hu");
//})
$("#infoAcceptBtn").click(changePanel);
$("#back_InfoBtn").click(changePanel);

$("#bodyDiv").animate({scrollTop: 0}, "slow");
$("#chatConsole").scroll(function (event) {
	var scrollCoef = event.target.scrollHeight - event.target.scrollTop - event.target.clientHeight;
	Scroll_To_Bot = !(scrollCoef > 100); // average p is about 22px?
});
$("#submitBtn").click(submitAnswer);
$("#answerInput").keydown(captureTab);
populateEmojiTable();
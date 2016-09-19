/**
 * Fairly expansive javascript for chat settings
 */

"use strict";

////Chat color constants
//var TYPING_BGC = 'Lavender';
//var USER_TYPING_BGC = 'CornSilk';
//var USER_BGC = 'LavenderBlush';
//var CHAT_HISTORY_BGC = 'LightGoldenRodYellow';
//var CHAT_HISTORY_DBGC = 'Beige';
//
//var AM_MONITOR_BGC = 'rgba(140, 0, 26, 0.7)';
//var AM_CHAT_BGC = 'rgba(31, 122, 31, 0.7)';
//var AM_NONE_BGC = 'rgba(128, 128, 128, 0.7)';
//
//var AM_UPDATE_TIME = 1.5*1000; //in milliseconds 
//var AM_Update_TimeoutID = null;

var SERVER_COLOUR = 'Navy';

var clientID = window.name;
var groupPrompt = false;

var Chat = {};
Chat.socket = null;

var host = '';
var Display_Chat = []; //Designates group window type/display
var Scroll_To_Bot = []; // user scrolling up
var sentLeaveAM = false;
var Global_Group_Total = 0;
var Global_Group_Offset = 0;
var reconnectAttempt = null;
var reconnectAttemptCounter = 0;

//Populate Emoji before we copy table
populateEmojiTable();
//Admin Monitor HTML constants
//Template for group info
var AMGroupInfo = document.getElementById("GXAMGroupInfo").cloneNode(true);

//Template for chat window
var AMGroupWindow = document.getElementById("GXGroupWindow").cloneNode(true);
document.getElementById("GXGroupWindow").style.display = "none";

var AMCredentials = {username:"", loggedIn:false};

util_openSocket(); //open webSocket

	Chat.socket.onopen = function() {
		$ ("#clientGfx").addClass("activeWS");
		$ ("#serverGfx").addClass("activeWS");
		$ ("#pingGfx").addClass("pingPong");
		util_affirmUserClient();
		console.log("WebSocket was opened");
		
	};

	Chat.socket.onclose = function() {
		$ ("#clientGfx").removeClass("activeWS");
		$ ("#serverGfx").removeClass("activeWS");
		$ ("#pingGfx").removeClass("pingPong");
		console.log("WebSocket was closed.");
		
		if (!sentLeaveAM) {
			$("#serverGfx").addClass("disconnectWS");
			$("#pingGfx").addClass("reconnect");

			setTimeout(util_reconnectSocket, 1000); //reconnect after 1s
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
		} else if (messageType == 'groupInfo') {
			updateGroupInfo(messageNode); //update the group info on the page
			return;
		} else if (messageType == "setClose") {
			//set is closed, remove all windows/side bar
			$(".groupWindow").remove();
			$("#AMStatusTBody tr").remove();
			return;
		} else if (messageType == 'lGroupMembers') {
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
			return;
		} else if (messageType == 'answerType') {//typing
			var groupID = messageNode.getAttribute("groupNumber");
			$("#answerPara"+groupID).text(messageNode.textContent);
			return;
		} else if (messageType == 'AnswerGroupStatus') {//change of status
			var groupID = messageNode.getAttribute("groupNumber");
			$("#answerPara"+groupID).text(messageNode.getElementsByTagName("answer")[0].textContent);
			
			var answerLock = messageNode.getAttribute("answerLock")=="true";
			//change answerStatus based on answerLock
			if (answerLock) {//answer submitted
				$("#answerCorrect"+groupID).prop("disabled", false);
				$("#answerWrong"+groupID).prop("disabled", false);
			} else { //answer being edited
				$("#answerCorrect"+groupID).prop("disabled", true);
				$("#answerWrong"+groupID).prop("disabled", true);
				$("#answerAlert"+groupID).prop("id", "answerAlert").removeClass();//hide and make answerAlert available
//				if ($("#answerGroupID"+groupID).text()==groupID) { //remove alert
//					$("#answerAlert"+groupID).removeClass();
//				}
			}
				
			return;
		} else if (messageType == 'answerAlert') {
			var groupID = messageNode.getAttribute("groupNumber");
			var jqAvailAnswerAlert = $("#answerAlert").eq(-1); //get last available answerAlert
			
			jqAvailAnswerAlert.find("span").text(groupID); //groupID span
			jqAvailAnswerAlert.find("button").off("click").click(function () {
				showAnswer(groupID);
				$(window).scrollTop($(window).scrollTop()-100); //scroll up by 100px??
			}); //place button handler
			jqAvailAnswerAlert.prop("id", "answerAlert"+groupID).addClass("show"); //change ID & add class
			
			//$("#answerAlert span").text(groupID);
			//$("#answerAlert button")
			//$("#answerAlert").prop("id", "answerAlert"+groupID).addClass("show");
			
			$("#answerAlarm")[0].play();
			$("#answerCorrect"+groupID).prop("disabled", false);
			$("#answerWrong"+groupID).prop("disabled", false);
			return;
			
		} else if (messageType=="answerStatus"||messageType=="answerPrompt"||messageType=="answerUpdate"
			||messageType=="answerSubmitReview" || messageType=="answerReview" || messageType=="answerUnderReview") {
			//capture these but do nothing
			return;
		} else if (messageType!='typing' && messageType!='chat' && messageType!='alert') {
			console.log("unparsable: "+xml);
			return;
		}
		
		//****************************************************
		//Begin to parse chat messages here
		//****************************************************
		
		var groupID = messageNode.getAttribute('groupNumber');
		var chatConsole = document.getElementById('chatConsole'+groupID); // get console for AM
		chat_processMessage(messageNode, messageType, chatConsole);
		//return;
		
		/*var textNode; // this thing is the cause of so many issues
		var innerText = '';
		if (xmlDoc.getElementsByTagName('text') !== undefined) { // error handling for textNode
			textNode = xmlDoc.getElementsByTagName('text')[0];
			if (textNode !== undefined && textNode.childNodes[0] !== undefined) {
				innerText = textNode.textContent;

				// Properly REparse & out of text otherwise XML Parser WILL CRASH
				innerText = innerText.replace(/&/gm, '&amp;');
				innerText = innerText.replace(/</gm, '&lt;');
			}
		}
		
		var senderColor = messageNode.getAttribute('senderColor');
		var senderID = messageNode.getAttribute('senderID');
		var userLabel = messageNode.getAttribute('senderName') + ": ";
		var groupID = messageNode.getAttribute('groupNumber');
		userLabel = '<strong style="color:' + senderColor + '">' + userLabel	+ '</strong>';

		// Parse a date string and then pull time string from that (sorry no AM/PM)
		var serverTS = messageNode.getAttribute('timestamp');
		var timestamp = serverTS.match(/_(\S+)/)[1];
		timestamp = '<div>' + timestamp + '</div>';

		var chatConsole = document.getElementById('chatConsole'+groupID); // get console
		var newline = document.createElement('br'); // get break tag
		var chatLineBreak = document.createElement('div');
		chatLineBreak.className = 'chatLine';
		chatLineBreak.style.backgroundColor = senderColor;
		var paragraph = document.getElementById(senderID+"l"+groupID); // get para if already created
		
		//Special condition for Admin Monitor since a user can type/chat in different groups and should be overwrit
		/*if (paragraph!=null && paragraph.parentElement!=console) {//if this paragraph is not in the same console/group, make new one
			//if paragraph is an array, parentElement is null and fails check
			if (paragraph instanceof Array) { //if is an array
				for (var ii=0; i<paragraph.length; i++) { //find the right paragragh
					if (paragraph[ii].parentElement==console)
						paragraph = paragraph[ii];
				}
			}
			if (paragraph instanceof Array) //if couldnt find paragraph in that array
				paragraph = null; 
		}

		var chatText = userLabel + innerText + timestamp; // put everything into 1
		
		if (paragraph == null && innerText!="") {
			paragraph = document.createElement('p');
			paragraph.id = senderID+"l"+groupID;
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
			innerText += timestamp; //undecided on this rn
			
			paragraph.id = '';
			paragraph.innerHTML = innerText;
			paragraph.className = "serverMessage";
			paragraph.appendChild(newline);
			chatLineBreak.style.backgroundColor = SERVER_COLOUR;
			paragraph.appendChild(chatLineBreak);
		} else {
			console.log('invalid message from server: \n' + xml);
		}
		*/

		// Scroll to bottom on console if user not scrolled up
		var gID = parseInt(groupID);
		if (Scroll_To_Bot[groupID-Global_Group_Offset-1])
			chatConsole.scrollTop = chatConsole.scrollHeight;
	};

/*function buttonSend(event) {
	// send message from text field when/button enter is pressed
	var groupID = event.currentTarget.id;
	if (event.currentTarget.tagName=="button")
		groupID = parseInt(groupID.slice(11)); //remove 'chat_button' from string
	else
		groupID = parseInt(groupID.slice(4)); //remove 'chat' from string
	
	//console.log("Sent to group "+groupID);
	Chat.sendMessage(event, groupID);
}*/

function sendChat(event) {
	var keycode = event.keyCode || event.which;
	var groupID = Number(/\d*$/.exec(event.target.id)); //event.currentTarget.id was used before
	var message = $('#chatInput'+groupID).val();
	
	var chatType = 'typing'; // typing by default
	// If Enter clicked or Chat btn clicked
	// If message is empty, it should be a typing event so we're not broadcasting blank text
	if (event.type!='input' && message != '' && (event.type === 'click' || keycode == 13)) {
		chatType = 'chat';
		$('#chatInput'+groupID).val("");
	} else if (event.type!='input'){
		//console.log("typing with no change");
		return;
	}

	// Properly parse & out of text otherwise XML Parser WILL CRASH
	message = message.replace(/&/gm, '&amp;');
	message = message.replace(/</gm, '&lt;');

	var xml = '<message type="' + chatType + '" senderID="' + clientID + '" groupNumber="' + groupID +'">'
			+ '<chat>' + '<text>' + message + '</text>' + '</chat>'
			+ '</message>';
	Chat.socket.send(xml);
};

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
	$("#loginBtn").text("...");
	$("#loginBtn")[0].disabled = true;
	$("#usernameAM")[0].disabled = true;
	AMCredentials.username = usernameText;
}

//Function runs to initialize group info
function updateGroupInfo(messageNode) {
	//clear the first table of values
	var AMTBody = $("#AMStatusTBody")[0];
	//remove every row except the header
	while (AMTBody.children.length!=0) //tbody
		AMTBody.removeChild(AMTBody.lastElementChild);
	
	var oldChatWindows = $(".groupWindow").remove(); //get rid of all chat windows

	//check groups created
	var setStatus = messageNode.getAttribute('setStatus');
	if (setStatus == 'FALSE') {
		alert("There are no groups created currently.");
		document.getElementById("loginBtn").disabled = true;
		document.getElementById("loginBtn").textContent = "No Groups Created";
		groupPrompt = true;
	} else {
		if (groupPrompt) {
			alert("Groups have been created/updated!");
			groupPrompt = false;
		}
		
		if (AMCredentials.loggedIn)
			AMLogin(); //run login script
		
		//add rows based on groupInfo
		var groupTotal = Number(messageNode.getAttribute('groupTotal'));
		var groupOffset = Number(messageNode.getAttribute('groupOffset'));

		Global_Group_Total = groupTotal;
		Global_Group_Offset = groupOffset;
		
		groupTotal += groupOffset;
		
		for (var i=groupOffset; i<groupTotal; i++) {
			var groupInfo = AMGroupInfo.cloneNode(true); //clone 
			AMTBody.appendChild(groupInfo); 
			groupInfo.id = "AMGroupInfo"+(i+1);
			groupInfo.cells[0].textContent = "Group "+(i+1);
			
			groupInfo.cells[1].children[0].id = "CBgroupChat"+(i+1);
			groupInfo.cells[1].children[0].onchange = updateAMStatus;
			groupInfo.cells[1].children[1].htmlFor  = "CBgroupChat"+(i+1);
			
			groupInfo.cells[2].children[0].id = "CBgroupMonitor"+(i+1);
			groupInfo.cells[2].children[0].onchange = updateAMStatus;
			groupInfo.cells[2].children[1].htmlFor = "CBgroupMonitor"+(i+1);
		}

		document.getElementById("loginBtn").disabled = false;
		document.getElementById("loginBtn").textContent = "Login";
		document.getElementById('usernameAM').onkeyup = function(e) {if(e.keyCode==13 || e.which==13) AMLogin()};
	}
	
	//Set up background variables to begin to set up the system
	//Use dynamic javascript array length here
	for (var i=0; i<Global_Group_Total; i++) {
		Display_Chat[i] = 0; //Not monitoring, not chatting
		Scroll_To_Bot[i] = true;
		//GroupType[i] = 0;
	}
	
}

//Send changes to AM to server
function updateAMStatus(event) {
	var justChanged = event.target; //the checkbox that just changed
	
	var xml = '<message type="adminMonitorUpdateStatus" senderID="'+clientID+'" ';//  >';
	
	var groupID = Number(/\d*$/.exec(event.target.id));
	xml += 'groupID="'+groupID+'">';
	
	var groupM = $("#CBgroupMonitor"+groupID)[0];
	var groupC = $("#CBgroupChat"+groupID)[0];
	groupM.disabled = true; //disable both until server confirmation
	groupC.disabled = true;
	
	//xml += '<groupAMInfo groupID="'+groupID+'">';
	if (groupM.checked && !groupC.checked)
		xml += "2";
	else if (!groupM.checked && groupC.checked)
		xml += "1";
	else if (!groupM.checked && !groupC.checked)
		xml += "0";
	else {//groupM && groupC - cant be both
		if (groupM==justChanged) {
			groupC.checked = false;
			xml += "2";
		} else {//groupC == justChanged
			groupM.checked = false;
			xml += "1";
		}
	}
	//xml += "</groupAMInfo>";
	
	xml += '</message>';

	$("[for='"+justChanged.id+"']")[0].classList.add("waiting"); //change label class
	//Now send the updated info to the server
	//return;
	Chat.socket.send(xml);
}

//Update based on server confirmation
function updateServerStatus(messageNode) {
	
	$("#usernameSpan").text(messageNode.getAttribute("senderName"));
	$("#loginDiv").css("height", "0px").css("padding", "0px");
	$("#usernameDiv").css("height", "5em");
	
	var groupOff = Number(messageNode.getAttribute('groupOffset'));
	var groupTotal = Number(messageNode.getAttribute('groupNum'));
	
	for (var i=0; i<groupTotal; i++) {
		
		var groupID = (i+groupOff+1);
		var groupInfoRow = document.getElementById("AMGroupInfo"+groupID);
		var groupInfo = messageNode.getElementsByTagName('groupAMInfo')[i];
		var groupStat = Number(groupInfo.textContent);

		var groupC = groupInfoRow.cells[1].firstElementChild;
		var groupM = groupInfoRow.cells[2].firstElementChild;

		groupInfoRow.cells[1].lastElementChild.className = "chat";
		groupInfoRow.cells[2].lastElementChild.className = "monitor";
		
		//IM DISABLING MONITOR CAUSE NO ONE USES IT PROPERLY
//		groupM.disabled = false;
		groupC.disabled = false;
		groupM.checked = false;
		groupC.checked = false;
		
		if (groupStat==1)
			groupC.checked = true;
		else if (groupStat==2)
			groupM.checked = true;
		
		Display_Chat[i] = groupStat;
	}
	//update chat display
	updateAMChat();
	AMCredentials.loggedIn = true;
}

function updateAMChat() {
	//check display_chat, add if there, remove if not
	
	for (var i=0; i<Display_Chat.length; i++) {
		var groupId = (i+Global_Group_Offset+1);
		var chatBox = document.getElementById('GroupWindow'+groupId);
		if (Display_Chat[i]>0) {
			if (chatBox==null) {
				chatBox = AMGroupWindow.cloneNode(true);
				chatBox.id = "GroupWindow"+groupId;
				
				$(chatBox).find("*[id]").each(function(){
					this.id = this.id+groupId;
				});
				$(chatBox).find("*[for]").attr("for", "windowType"+groupId);
				$(chatBox).find(".groupHeader").text("Group "+groupId);
				//$(chatBox).find('button')
				$(chatBox).find('.chatConsole').scroll(function (event) {
					var scrollCoef = event.target.scrollHeight - event.target.scrollTop - event.target.clientHeight;
					var groupID = Number(/\d*$/.exec(event.target.id))-Global_Group_Offset-1;
					Scroll_To_Bot[groupID] = !(scrollCoef > 100); // average p is about 22px?
				});
				$(chatBox).find(".chat")[0].oninput = sendChat;
				$(chatBox).find(".chat")[0].onkeydown = sendChat;
				/*
				chatBox.getElementsByTagName('button')[0].onclick = function(event) { buttonSend(event)};
				*/
				$("#chatEndMarker").before(chatBox);
			}
			
			if (Display_Chat[i]==2) { //Monitor
				$(chatBox).addClass("monitor").removeClass("chat");
				$(chatBox).find("button").prop("disabled", true); //disable all buttons
				$(chatBox).find("input[type=text]").prop("disabled", true); //disable chat input
				$(chatBox).find(".chatInteract .answerInteract").css("background-color", "gray");
				$(chatBox).find(".chat").prop("placeholder", "Chat is disabled while monitoring");
				
			} else if (Display_Chat[i]==1) { //Chat
				$(chatBox).addClass("chat").removeClass("monitor");
				$(chatBox).find(".chatInteract button").prop("disabled", false); //enable send chat button
				$(chatBox).find("input[type=text]").prop("disabled", false); //enable chat input
				$(chatBox).find(".chatInteract .answerInteract").css("background-color", "inherit");
				$(chatBox).find(".chat").prop("placeholder", "Type and press Enter to chat");
			}
		} else if (Display_Chat[i]==0) {
			if (chatBox!=null) 
				chatBox.parentElement.removeChild(chatBox);
		}
	}
}

//called when the review button is pressed
function showAnswer(groupID) {
	//console.log("reviewing answer "+groupID);
	$("#windowType"+groupID).prop("checked", true); //display answer via css
	location.href = "adminMonitor.html#windowType"+groupID; //go to answer
	
	//$("#answerAlert").removeClass();
	$("#answerAlert"+groupID).prop("id", "answerAlert").removeClass();//hide and make answerAlert available
	//$("#answerLink"+groupID).prop("id", "answerLink");
	//$("#answerGroupID"+groupID).prop("id", "answerGroupID");
	
	
	var xml = '<message type="answerUnderReview" senderID="' + clientID + '" groupNumber="' + groupID +'">'
	+ '</message>';
	Chat.socket.send(xml);
}

//called when answer has been evaluated
function review(event) {
	var type = /^answer(\D*)/.exec(event.currentTarget.id)[1];
	type = type.toLowerCase();
	var groupID = Number(/\d*$/.exec(event.currentTarget.id));
	
	$("#answerWrong"+groupID).prop("disabled", true);
	$("#answerCorrect"+groupID).prop("disabled", true);
	
	$("#windowType"+groupID).prop("checked", false);
	
	var xml = '<message type="answerReview" senderID="' + clientID + '" groupNumber="' + groupID +'"'
	+ ' answerReview="'+type+'" >'
	+ '</message>';
	Chat.socket.send(xml);
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

window.onbeforeunload = function () {
	//This is assuming the user has purposely closed the page/refreshed the page.
	//Send a closeSocket message to the server- the server must know that client disconnect on purpose
	if(Chat.socket==null) //If Chat.socket is not loaded, dont run
		return;
	
	var xml = '<message type="adminMonitorLeave" senderID="'+clientID+'"/>';
	Chat.socket.send(xml);
	sentLeaveAM = true;
	console.log("beforeunload has run");
};

//JS Entry point

window.onunload = util_closeSocket;


$("#loginBtn").click(AMLogin);
//Chat_Row_One.removeChild(document.getElementById("GXChat"));
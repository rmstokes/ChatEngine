
"use strict";

/*
* The purpose of this javascript RIGHT NOW is to set up the emote library
* create the emote UI with imgs & etc
* Do emote replacement in HMTL
*
* Later I might extend this to do maintain chat & console & etc so I dont
* have to maintain two codebases in AM and LoginChat, but Im not sure whether
* I want the two to handle identically atm.
*/

//might make this into an actual object/class if necessary
var emoteArr = [
                "1f600",
                "1f601",
                "1f602",
                "1f603",
                "1f604",
                "1f605",
                "1f606",
                "1f609",
                "1f60a",
                "1f60b",
                "1f60e",
                "1f913",
                "1f60d",
                "263a",
                "1f914",
                "1f610",
                "1f611",
                "1f644",
                "1f60f",
                "1f623",
                "1f625",
                "1f62e",
                "1f910",
                "1f62f",
                "1f62a",
                "1f62b",
                "1f634",
                "1f61c",
                "1f61d",
                "1f612",
                "1f613",
                "1f615",
                "1f643",
                "1f632",
                "1f641",
                "1f616",
                "1f61e",
                "1f622",
                "1f62d",
                "1f626",
                "1f627",
                "1f628",
                "1f62c",
                "1f630",
                "1f631",
                "1f633",
                "1f635",
                "1f621",
                "1f608",
                "261d",
                "1f44d",
                "1f44c",
                "1f440",
                "2764",
                "1f494",
                "23f3",
                "2754",
                "1f4af",
                ];

/*Right now our emotes are proudly sponsored by EmojiOne! A free open-source
* emoji site that provided the art files for this. It's under the CC license!
* Its pretty great so I want to thank them
*/
function populateEmojiTable() {
	//$(".emoteTable").css("display", "none");
	var emoteLen = emoteArr.length;
	var nodesInsert = ''
	for (var i=0; i<emoteLen; i++){
		//$(".emoteTable").append("<li><img src='img/emojiOne-Svg/"+emoteArr[i]+".svg' alt='"+String.fromCodePoint(parseInt(emoteArr[i], 16))+"' onclick='pasteEmote()'/></li>");
		//$(".emoteTable").append("<li><img src='img/emojiOne-Svg/"+emoteArr[i]+".svg' alt='&#x"+emoteArr[i]+";' onclick='pasteEmote(event)'/></li>");
		nodesInsert += "<li><img src='img/emojiOne-Svg/"+emoteArr[i]+".svg' alt='&#x"+emoteArr[i]+";' onmousedown='pasteEmote(event)'/></li>";
		//$(".emoteTable").append(li);
	}
	//$(".emoteTable").css("display", "block");
	$(".emoteTable").append(nodesInsert);
}

function pasteEmote(event) {
	// trigger = img; img ^ li ^ ul.emoteTable ^ div v input.chatInteract
	
	var chatInput = event.currentTarget.parentElement.parentElement.parentElement.getElementsByClassName('chat')[0];
	var chatValue = chatInput.value;
	var start = chatInput.selectionStart;
	
	var emoteStr = event.currentTarget.alt +" ";
	if (chatValue.charAt(chatValue.length-1)!=" ")
		emoteStr = " "+emoteStr;
	
	chatInput.value = chatValue.substring(0, start) +emoteStr+ chatValue.substring(start);
	chatInput.selectionStart = chatInput.selectionEnd = start + emoteStr.length;//replace cursor
	$(chatInput).trigger("input"); //cause value changes doesnt
	setTimeout(function () {$(chatInput).focus();}, 20); //Give focus on delay to account for blur event
	
}

function chat_replaceEmote(innerHTML) {
	//Replace the unicode with innerText
	//Kinda sucks computation wise but I dunno what else to do but pre-check
	
	//if (innerHTML.search(/&#x\w+;/)!=-1) {
		var emoteLen = emoteArr.length;
		var ptest = document.createElement('p');
		 for (var i=0; i<emoteLen; i++) {
			 ptest.innerHTML = '&#x'+emoteArr[i]+';';
			 var regex = new RegExp(ptest.innerHTML, 'g');
			 //innerHTML = innerHTML.replace("&#x"+emoteArr[i]+";",
			 innerHTML = innerHTML.replace(regex,
					 "<img src='img/emojiOne-Svg/"+emoteArr[i]+".svg' alt='&#x"+emoteArr[i]+";'/>");
		 }
	//}
	return innerHTML;
}

//You would think a rapidly evolving language like JS wouldnt need a hacky
//solution like this
//Thanks to ThinkingStuff on StackOverflow for this implementation
function htmlEncode( html ) {
    return document.createElement( 'a' ).appendChild( 
        document.createTextNode( html ) ).parentNode.innerHTML;
};

function chat_processMessage (messageNode, messageType, chatConsole) {
	//Processing incoming chat message- chat, typing or alert
	
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

	//var chatConsole = document.getElementById('chatConsole'); // get console
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
	} else {
		console.log('invalid message from server: \n' + xml);
	}
	

//	// Scroll to bottom on console if user not scrolled up
//	var scrollCoef = chatConsole.scrollHeight - chatConsole.scrollTop - chatConsole.clientHeight;
//	var scrollMax = chatConsole.scrollHeight - chatConsole.clientHeight;
	
}


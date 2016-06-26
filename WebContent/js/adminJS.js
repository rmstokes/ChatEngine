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

window.onbeforeunload = util_closeSocket;

util_openSocket();

	Chat.socket.onopen = function() {
		// connection opened with server
		$ ("#clientGfx").addClass("activeWS");
		$ ("#serverGfx").addClass("activeWS");
		$ ("#pingGfx").addClass("pingPong");
		util_affirmUserClient();
		$ ('#createBtn').removeAttr('disabled');
		//util_setUserClient();
	};

	Chat.socket.onclose = function() {
		// connnection closed with server
		$ ("#clientGfx").removeClass("activeWS");
		$ ("#serverGfx").removeClass("activeWS");
		$ ("#pingGfx").removeClass("pingPong");
		console.log("WebSocket was closed.");
	};

	Chat.socket.onmessage = function(message) {
		// message received from server

		var xml = message.data;
		var parser = new DOMParser();
		var xmlDoc = parser.parseFromString(xml, "text/xml");
		var messageNode = xmlDoc.getElementsByTagName('message')[0];
		var messageType = messageNode.getAttribute('type');
		
		
		if (messageType == 'permIDSet' || messageType == 'permIDConfirm') {
			console.log(messageType+" "+messageNode.getAttribute('senderID'));
			util_setPermID(messageNode.getAttribute('senderID'));
			
		} else if (messageType == 'groupSetInfo') {
			var setStatus = messageNode.getAttribute('setStatus');
			var groupOffset = messageNode.getAttribute('groupOffset');
			var groupTotal = messageNode.getAttribute('groupTotal');
			var setStartTime = messageNode.getAttribute('setStartTime');
			var logName = messageNode.getAttribute('logName');
			
			var logSaveLast = messageNode.getAttribute('logSaveLast');
			var serverStartTime = messageNode.getAttribute('serverStartTime');
			
			$ ('#serverStatus').text(setStatus);
			$ ('#currentSetDiv').removeClass().addClass(setStatus);
			if (setStatus=="TRUE") {
				setCreated = true;
				$('#closeBtn').removeAttr('disabled');
			} else {
				setCreated = false;
				$('#closeBtn').attr('disabled');
			}
			
			
			$ ('#serverGroupOffset').text(groupOffset);
			$ ('#serverGroupTotal').text(groupTotal);
			$ ('#serverSetStartTime').text(setStartTime);
			$ ('#serverLogName').text(logName);
			$ ('#serverStartTime').text(serverStartTime);
			$ ('#logSaveLast').text(logSaveLast);
			
			
//			var checkGroups = messageNode.getAttribute('checkGroups');
//			var checkGroupHTML = document.getElementById("checkGroupPara");
//			var groupOff = messageNode.getAttribute('groupOffset');
//			checkGroupHTML.innerHTML = "GroupNum: " + checkGroups
//					+ " <br/> Offset: " + groupOff;
//			if (checkGroups == "Not Created") {
//				checkGroupHTML.style.backgroundColor = "Navy";
//				setCreated = false;
//			} else {
//				checkGroupHTML.style.backgroundColor = "SeaGreen";
//				setCreated = true;
//			}
//			if (adminGroupsCreated) {
//				document.getElementById("adminMonitorBtn").disabled = false;
//				document.getElementById("loginBtn").disabled = false;
//			}
		} else if (messageType == 'alert') {
			var msgtext = messageNode.childNodes[0].nodeValue;
			util_alert(msgtext);
		} else if (messageType=='redirect' || messageType=='groupInfo') {
			//capture but do nothing
		} else {
			console.log("Could not parse server message: \n" + message.data);
		}

	}; // end onmessage

function createGroups() {

	if (adminGroupsCreated) {
		var adminOverwrite = confirm("A list of groups already exist. Do you want to overwrite them? \n"
				+ "This will reset the group documentation.");
		if (!adminOverwrite) {
			return;
		}
	}
	// send xml message to server to create groups.
	var numGroups = document.getElementById('groupCount').value;
	var instruct = document.getElementById('instruct').value;
	var groupOff = document.getElementById('groupOffset').value;
	// alert('numGroups: '+numGroups);
	var xml = '<message type="groupCreation" senderID="' + clientID
			+ '" instructor="' + instruct + '" groupOffset="' + groupOff + '">'
			+ '<text>' + numGroups + '</text>' + '</message>';
	Chat.socket.send(xml);
}

function createSet () {
	
	if (setCreated==true) {
		var setOverwrite = confirm("There is already a Set of groups in progress. \n"+
				"Creating a new Set of groups will cause the current Set to save and quit. \n"+
				"Would you like to continue making a new Set of Groups?");
		if (!setOverwrite)
			return;
	}
	
	
	var groupOffset = $ ("#groupOffset").prop("value");
	groupOffset = parseInt(groupOffset);
	groupOffset = (groupOffset<0) ? 0 : groupOffset;
	var groupTotal = $('#groupTotalPicker').prop("value");
	var logName = $('#logName').prop("value");
	
	var xml = '<message type="groupCreation" senderID="' + clientID
	+ '" logName="' + logName + '" groupOffset="' + groupOffset + '">'
	+ '<text>' + groupTotal + '</text>' + '</message>';
	
	Chat.socket.send(xml);
	
}

function deleteSet () {
	if (setCreated==true) {
		var setOverwrite = confirm("Are you sure you want to delete this Set of Groups? \n"
				+"This will close all groups, kick all chatters, and save the final logs.");
		if (!setOverwrite)
			return;
	}
	
	var xml = '<message type="groupDeletion" senderID="' + clientID + '">'
	+ '</message>';
	
	Chat.socket.send(xml);
}

function fileSelected (files) {
	var file = files[0];
	//var file = document.getElementById('fileUpload').files[0];
	//$('#fileUpload')[0].files = files; cant do this for security reasons
	if (file) {
		$("#fileName").text("FileName: "+file.name);
		TAfile = file;
	}
}

function dropFile (event) {
	event.stopPropagation();
	event.preventDefault();
	
	var dt = event.dataTransfer;
	var files = dt.files;
	
	fileSelected(files);
}

function preventDefault(event) {
	event.stopPropagation();
	event.preventDefault();
}

function uploadFile() {
	if (!TAfile)
		return;
	var fd = new FormData();
	fd.append("fileUpload", TAfile);
	var xhr = new XMLHttpRequest();
	xhr.upload.addEventListener("progress", uploadProgress, false);
	xhr.addEventListener("load", uploadComplete, false);
    //xhr.addEventListener("error", uploadFailed, false);
    //xhr.addEventListener("abort", uploadCanceled, false);
    xhr.open("POST", "HTTP");
    xhr.send(fd);
}

//Most of the XHR request is copied from http://pedrolobito.com/demos/html5_file_upload/ source
function uploadProgress(evt) {
    if (evt.lengthComputable) {
    	var percentComplete = Math.round(evt.loaded * 100 / evt.total);
    	$('#fileUploadDate').innerHTML = percentComplete.toString() + '%';
    }
    else {
    	$('#fileUploadDate').innerHTML = 'unable to compute';
    }
  }

function uploadComplete(event) {
	alert("Upload done?");
}

$("#fileDrop")[0].addEventListener("drop", dropFile, false);
$("#fileDrop")[0].addEventListener("dragenter", preventDefault, false);
$("#fileDrop")[0].addEventListener("dragover", preventDefault, false);

$ ("#createBtn").click(createSet);
$('#closeBtn').click(deleteSet);
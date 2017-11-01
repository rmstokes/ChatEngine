<!DOCTYPE html>
<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
<meta charset="UTF-8"></meta>
<title>Login Chat (Beta)</title>
<link rel="stylesheet" type="text/css" href="css/siteWide.css" />
<link rel="stylesheet" type="text/css" href="css/chat.css" />
<link rel="stylesheet" type="text/css" href="css/loginChat.css" />
<link id="titleIcon" href="img/chatbubble-working.png"
	rel="shortcut icon" />
<meta http-equiv="Cache-Control"
	content="no-cache, no-store, must-revalidate" />
<meta http-equiv="Pragma" content="no-cache" />
<meta http-equiv="Expires" content="0" />
</head>
<body>
	<!-- <div style="display:flex; flex-direction:column"> -->
	<div id="headerDiv">
		<div id="webSocketGfx">
			<div id="clientGfx" class="circleBase"></div>
			<div id="serverGfx" class="circleBase"></div>
			<div id="pingGfx" class="circleBase ping"></div>
		</div>

		<h2>
			Login <span style="float: right; top: 30px">COMPS</span>
		</h2>

	</div>

	<p class="navMessage">
		Try not to use <strong>refresh</strong> or <strong>back</strong> to
		change pages while on this site
	</p>
	<br />

	<div id="bodyDiv">
	<?php 
	if ($_SERVER['REQUEST_METHOD'] === 'POST') {?>
		<input type='hidden' id='groupSelection' name='groupSelection' value='<?php echo $_POST[groupSelection]?>'>
		<input type='hidden' id='lastName' placeholder='Last Name/Initial' name='lastName' value='<?php echo $_POST[lastName]?>'>
		<input type='hidden' id='firstName' name='firstName' placeholder='First Name' tabindex="1" value='<?php echo $_POST[firstName]?>'>		
		<body onload="loginGroup()">		
	<?php}
	
	
	?>
		
		
		<div id="loginDiv" class="centerDiv">
			<h1 style="color: slateblue">Login</h1>
			<label>
				<p class="inputLabel">First Name</p> <input autofocus type='text' id='firstName' placeholder='First Name' tabindex="1"></input>
			</label>

			<div style="height: 20px"></div>

			<label>
				<p class="inputLabel">Last Name or Initial</p> <input type='text'
				placeholder='Last Name/Initial' id='lastName'></input>
			</label>

			<div style="height: 20px"></div>

			<label>
				<p class="inputLabel">Group Number</p> <select id='groupSelection'
				style="font-size: 120%; text-align: center">
					<option value="1">?</option>
					<option value="2">??</option>
					<option value="3">???</option>
					<option value="4">Missingno</option>
			</select>
			</label>

			<div style="height: 30px"></div>

			<button id="joinBtn" style='font-size: 130%' disabled="">Join</button>
		</div>


		<div id="infoDiv" class="offRightDiv">
			<h1>Info Page</h1>

			<p>Please take time to read this if you haven't read this before.
			</p>

			<div
				style="transform: translateX(350px); background-color: orangered; border-radius: 5%; width: 100px; height: 100px"></div>

			<div style="clear: both"></div>

			<p>
				An <strong>RED Answer</strong> window has been added to make it
				easier to submit your final answer. The final answer for each
				question should be typed here <strong>with an explanation.</strong>
			</p>

			<p>
				Once you have completed your answer, the team leader should click
				the <strong>submit</strong> button. Everyone else will get a popup
				and will have to <strong>agree</strong> or <strong>disagree</strong>
				with that answer. <strong>Everyone</strong> must respond for the
				answer to be submitted to the <strong>TA!</strong>
			</p>

			<p>Once the answer is submitted, the TA will be alerted and will
				respond to your answer.</p>

			<p>
				Remember that this lab is about <strong>collaboration!</strong> Work
				together with your team and make sure you understand how the answer
				was reached for the post-test! Don't be afraid to ask questions and
				help others!
			</p>

			<!-- 		<p>If you haven't read this before, <strong>please read this in its entirety!</strong> </p> -->

			<!-- 		<p>If you have read this before, at least skim over the <strong>bold</strong>/<span class="UL">underlined</span> words since the system changes frequently!</p> -->

			<!-- 		<p>There are now <strong>2</strong> windows in the chat page. </p> -->

			<!-- 		<div style="width:300px; transform:translateX(60%)"> -->
			<!-- 			<div style="float:left; background-color:orangered; border-radius:5%; width:100px; height:100px"></div> -->
			<!-- 			<div style="float:right; background-color:steelblue; border-radius:5%; width:100px; height:100px"></div> -->
			<!-- 		</div> -->

			<!-- 		<div style="clear:both"></div> -->

			<!-- 		<p style="float:right; width:48%"> The <strong style="color:BLUE">Discussion</strong> Window is where you will chat with your other team members. -->
			<!-- 			 </p> -->


			<!-- 		<p style="float:left; width:48%"> The <strong style="color:RED">Answer</strong> Window is where you will type the <span class="UL">final answer</span>   -->
			<!-- 			 for your group. The answer must be <strong>submitted</strong> by the whole group to the TA. -->
			<!-- 			 </p> -->

			<!-- 		<br style="clear:both"/> -->

			<!-- 		<p> When you are ready to <strong>submit</strong> your answer to the TA, all group members must agree and click <strong>Submit</strong> on the sidebar. -->
			<!-- 			There is a sidebar on the right that shows your group members and the TA for your group. Those whose names are <span class="UL">blinking</span> are currently ready to -->
			<!-- 			 <span class="UL">submit</span> the answer. You can choose to <span class="UL">Submit</span> by clicking your own name OR clicking the -->
			<!-- 			<strong>Submit</strong> button. </p> -->

			<!-- 		<p> Once the whole group has agreed to <strong>submit</strong>, the <span class="UL">final answer</span> cannot be edited unless everyone <strong>cancels</strong> -->
			<!-- 			or the TA <span class="UL">reviews</span> your answer. To cancel a submission, click your name or the <strong>Cancel</strong> button. </p> -->

			<!-- 		<p> As soon as the <span class="UL">final answer</span> has been <strong>submitted</strong>, the TA will be alerted. The TA will <span class="UL">review</span> your answer by -->
			<!-- 			approving or declining. If approved, you can go to the next question, otherwise you will have to discuss with your team to get the right answer. -->
			<!-- 			</p> -->

			<!-- 		<p> The aim of this lab is <strong>collaboration</strong>. Remember that you are graded on your <strong>participation</strong> -->
			<!-- 		 so make sure to keep involved, ask your team if you are confused and help others if they are.  -->
			<!-- 			Keep all communication in the chat, stay focused and have fun!!</p> -->

			<button id="infoAcceptBtn">Continue</button>
		</div>


		<div id="chatDiv" class="offRightDiv">


			<div>
				<button id="back_InfoBtn" class="blueBtn">◀ Info</button>
				<h1 id='groupName' style="">Group X</h1>
			</div>

			<div>
				<div id="alertButtonDiv" display="none">
					<button id='alertBtn'>Alert TA</button>
				</div>
				<div id="answerWindow">

					<div class="answerContainer">

						<textarea id="answerInput" rows="10" cols="40"
							placeholder="Type your final answer here"></textarea>
						<p id="lockedPara" class="lockedPara">Locked by User Y</p>

						<div class="prevDiv">
							<p class="prevHeader">Hover to Show Previous Answer</p>
							<p id="prevPara">Its the final countdown!!</p>
						</div>
					</div>

					<p id="lockedPara" class="lockedPara">Answer is unlocked</p>
					<div id="lockedParaTimer"></div>

					<div id="answerDropDown"></div>

					<button id='submitBtn'>Submit</button>
				</div>

				<div id="chatWindow" style="">

					<div class="chat-container">
						<p class="windowSubtitle">CHAT</p>
						<div id="chatConsole" class="chatConsole"></div>
					</div>


					<div>
						<input id="chatInput" class="chat" type="text"
							placeholder="type and press enter to chat" />
						<button id='chat_button' onclick='sendChat(event)'>Chat</button>
						<div class="emoteIcon">
							<img src="img/emojiOne-Svg/1f600.svg" alt="EmoteIconDoNotCopy" />
						</div>
						<div class=emoteTablePadding></div>
						<ul class="emoteTable" ondragstart="return false">
						</ul>
						<div class="emoteSpeak"></div>
					</div>
				</div>
			</div>
		</div>

		<div id="sidebar">
			<img src="img/WhiteArrow2.png" alt="Right Arrow">
			<p>Test User</p>
		</div>


	</div>

	<div id="blackOverlay" class="hidePopup blackOverlay"
		style="visibility: hidden"></div>

	<div id="answerPopup" class="hidePopup answerPopup"
		style="visibility: hidden">
		<p>
			<span id="answerSubmitUser" style="font-weight: bold">User</span> is
			ready to submit the answer. Do you agree?
		</p>
		<p id="answerInputCopy" class=""></p>
		<div style="font-size: 110%">
			<button id="answerAgree" onclick="sendAnswerStatus(true)">Agree</button>
			<button id="answerDisagree" class="cancelBtn"
				onclick="sendAnswerStatus(false)">Disagree</button>
		</div>
		<p id="answerPopupTimer" style="font-size: 120%">15s</p>
		<p style="font-size: 90%">Note that once the answer is submitted,
			it cannot be changed until the TA reviews it.</p>
	</div>
	<!-- </div> -->

</body>

<!--    <script	  src="https://code.jquery.com/jquery-2.2.4.min.js" -->
<!-- 			  integrity="sha256-BbhdlvQf/xTY9gja0Dq3HiwQF8LaCRTXxZKRutelT44=" -->
<!-- 			  crossorigin="anonymous"></script> -->
<script src="js/jquery-3.1.0.min.js" type="text/javascript"></script>
<script src="js/utility.js" type="text/javascript"></script>
<script src="js/chat.js" type="text/javascript"></script>
<script src="js/loginChatJS.js" type="text/javascript"></script>
</html>
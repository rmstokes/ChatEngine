
<!--
  This admin page shows system information and gives the instructor the ability to make new Sets (group of Groups)
-->
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">

  <head>
    <title>Admin Page - COMPS</title>
    <link rel="stylesheet" type="text/css" href="css/siteWide.css"/>
    <link rel="stylesheet" type="text/css" href="css/admin.css"/>
    <link id="titleIcon" href="img/chatbubble-working.png" rel="shortcut icon"/>
    <meta charset="UTF-8"></meta> 
  </head>
  
  <body>
  <div id="headerDiv">
  	<div id="webSocketGfx">
  		<div id="clientGfx" class="circleBase"></div>
  		<div id="serverGfx" class="circleBase"></div>
  		<div id="pingGfx" class="circleBase ping"></div>
  	</div>
   	<h2>Admin 
   		<span class="navH2" style="left:260px"><a href="loginChat.html" >>Login</a></span>
   		<span class="navH2" style="left:400px"><a href="adminMonitor.html" >>Admin-Monitor</a></span>
   		<span  style="float: right; padding: 0em 0.1em">COMPS</span></h2>
   </div>
   <p class="navMessage">Try not to use <strong>refresh</strong> or <strong>back</strong>  to change pages while on this site</p>
  <br/>
  
  
  
  <div id="bodyDiv">
<!--      <?php
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {?>
		<input type='hidden' id='groupOffset'  value='<?php echo $_POST[groupOffSet]?>'>
		<input type='hidden' id='groupTotalPicker'  value='<?php echo $_POST[groupTotalPicker]?>'>
		<input type='hidden' id='logName' placeholder="Log Name" value='<?php echo $_POST[logName]?>'>		
		<body onload="createSet()">		
	<?php}

	?>
	
	-->
	
	
		
    <div class="leftF halfDiv">
    <p style="font-size:80%; padding:5px">Hover over a <span class="highlight">Yellow</span> label to see information about it</p>
    <div id="createSetDiv" style="max-width: 350px; margin: 0px auto;">
    	<h1 class="sectionName">Create A New Set</h1>
    	
    
    	
    	
		
    	<div id="firstDivRow" class="divRow">
    		<div class="halfDiv leftF">
    		<label title="Start the number of groups from this value +1 
Eg. offset of 10, groups start at 11, 12, 13...">
    			<h3 class="yellow">Group Offset</h3>
   			  	<input type='number' pattern='[0-9]' min='0' value='0' max='1000' step='1'  id='groupOffset' style="width:5em; text-align:center"/><br/>
    		</label>	
    		</div>
    		<div class="halfDiv rightF">
    		<label title="Number of groups created. 
Eg. 5 groups, 1,2,3,4,5">
    			<h3 class="yellow">Number of Groups</h3>
    			<select id='groupTotalPicker' style="text-align:center">
        			<option value="1">1</option>
        			<option value="2">2</option>
        			<option value="3">3</option>
        			<option value="4">4</option>
        			<option value="5">5</option>
        			<option value="6">6</option>
        			<option value="7">7</option>
        			<option value="8">8</option>
        			<option value="9">9</option>
        			<option value="10">10</option>     
        			<option value="11">11</option>  
        			<option value="12">12</option>     
    			</select> 
    		</label>
    		</div>
    	</div>
<!--     	<br style="clear:both"></br> -->
		<div id="secondDivRow" class="divRow">
			
			<label title="This string is saved in the log directory. 
Use this to add text to the log directory name. 
For example, you can name a Set (set of groups) 'Lab_3_Wed'. ">
    			<h3 class="yellow">Log Name</h3>
    			<input maxlength='30' type='text' id='logName' placeholder="Log Name" style="text-align:center"/>
    		</label>
    		
    		
    		
		</div>
		<div id="thirdDivRow" class="">
			
			<button id="createBtn" disabled>Create Set</button>
			
			
			
		</div>
    </div>
    </div>
    
    <div class="leftF halfDiv">
    <div id="currentSetDiv" style="max-width: 520px; margin: 0px auto;">
    	<h1 class="sectionName">Current Set</h1>
    	<p id="serverStatus">Waiting for server info</p>
    	
    	<div id="answerWindowDiv" class="divRow">
    		<div class="leftF halfDiv">
			<label title="Unheck this box to disable the answer window">
    			<h3>Answer Window:</h3>
    			
    		</label>
    		</div>
    		<div class="rightF halfDiv">
    		<input id="answerWindowCheck" type="checkbox" onclick="sendAnswerWindowStatus();" checked>
    		</div>
    	</div>
    	
    	<div id="firstDivRow2" class="divRow">
    		<div class="halfDiv leftF">
    			<h3>Group Offset</h3>
    			<p id="serverGroupOffset" class="serverNum">#</p>
    		</div>
    		<div class="halfDiv rightF">
    			<h3>Number of Groups</h3>
    			<p id="serverGroupTotal" class="serverNum">#</p>
    		</div>
    	</div>
    	<div id="secondDivRow2" class="divRow">
    		<div class="halfDiv leftF">
    			<h3>Set Start Time</h3>
    			<p id="serverSetStartTime" class="serverTime">##:##:##:###</p>
    		</div>
    		<div class="halfDiv rightF">
    			<h3>LogName</h3>
				<p id="serverLogName" class="serverText">No DATA</p>
		
    		</div>
    	</div>
    	<div id="thirdDivRow2" class="divRow">
    		<h3>Server Start Time</h3>
    		<p id="serverStartTime" class="serverTime">##:##:##:###</p>
		</div>
		<div class="divRow">
    		<h3>Logs Last Saved</h3>
    		<p id="logSaveLast" class="serverTime">##:##:##:###</p>
		</div>
		<div id="fourthDivRow2" class="">
			<button id="closeBtn" disabled>Close Set</button>
		</div>
    </div>
    </div>
    
    <br style="clear:both"></br>
    
  </div>
  
  <div id="fileUploadDiv" style="display:none">
    	<h1>Upload answer sheet for TAs</h1>
    	<div style="background-color:inherit; padding:10px">
    	<div id="fileDrop" class="dragFile">
    		<p>Drag file here</p>
    	</div>
    	<span>or</span>
<!--     	<form  id="fileUploadForm" enctype="multipart/form-data" method="post"> -->
    	<input type="file" id="fileUpload" name="fileUpload" onchange="fileSelected(this.files)"></input>
<!--     	</form> -->
		</div>
    	<p id="fileName">File Name:</p>
<!--     	<p id="fileSize">File Size:</p> -->
<!--     	<p id="fileType">File Type:</p> -->
    	<p id="fileUploadDate">Upload Date:</p>
    	<button onclick="uploadFile()">Upload</button>
    </div>
    
  </body>
  
<!--    <script	  src="https://code.jquery.com/jquery-2.2.4.min.js" -->
<!-- 			  integrity="sha256-BbhdlvQf/xTY9gja0Dq3HiwQF8LaCRTXxZKRutelT44=" -->
<!-- 			  crossorigin="anonymous"></script> -->
  <script src="js/jquery-3.1.0.min.js" type="text/javascript"></script>
  <script src="js/utility.js"></script>
  <script src="js/adminJS.js"></script>

</html>
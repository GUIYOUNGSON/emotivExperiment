var windowWidth = $(window).width();
var windowHeight = $(window).height();


// Get the id where the stimuli (both pictures) are located
var stimImage1 = $("#stimulus1");
var stimImage2 = $("#stimulus2");

$(window).bind('keypress', function(e) {
     handleKeypress(e.keyCode);
});

var connection;
var DEBUG = true;
var date = new Date();
var startTrialTime;
var responseTime;
var didRespond;
var sentResponse;

/* This function is set when we want to record keypresses.
If we don't (for example, when the participant has already
responded), it's null */

function handleKeypress(key);

function handleKeypressHelper(key){
  recordResponse();
  sendResponse();
}

/* Show 'images/category1/image1' and 'images/category2/image2' with
opacity opacity2 */
function showPics(category1, image1, category2, image2, opacity2) {
  // Hide any text first
  $("#instructs").text("");
  stimImage1.attr("src", "./images/" + category1 + "/" +
    image1 +".jpg");
  stimImage2.attr("src", "./images/" + category2 + "/" +
    image2 +".jpg");
  stimImage2.attr("opacity", opacity2);
}

/* Hide image, display instructions */
function showText(text){
  $("#image-holder").css('visibility', 'hidden');
  $("#instructs").text(text);
}

/* Hide everything */
function showBlank(){
  $("#image-holder").css('visibility', 'hidden');
  $("#instructs").text("");
}


// Tell backend to start the experiment
function initExperiment(){
  if(!$.connection){
    showText("Not connected to back end server!");
  }
  else{
    $.connection.write("init");
  }
}

/* update the opacity of image 2 */
function updateOpacity(opacity2){
  stimImage2.attr("opacity", opacity2);
}

function startTrial(category1, image1, category2, image2, opacity2){
  participantDidRespond = false;
  sentResponse = false;
  startTrialTime = WHATEVER THE FUNCTION THAT GETS THE CURRENT TIME
  // turn on interrupts here
  handleKeypress = handleKeypressHelper;
  showPics(category1, image1, category2, image2, opacity2);
}

function recordResponse(){
  // turn off interrupts here
  handleKeypress = null;
  if(!$.connection || participantDidRespond)  return;
  participantDidRespond = true;
  responseTime =  WHATEVER THE FUNCTION THAT GETS THE CURRENT TIME - startTrialTime;
}

function sendResponse(){
  if(!$.connection) return;
  sentReponse = true;
  if(participantDidRespond){
    // send response time
    $.connection.write("R:" + responseTime);
  }
  else{
    $.connection.write("NR");
  }
}


function startTrial()

var instructions = {
    "showText" : function(args){  showText(args[1]);  },
    "startTrial", function
    "endTrial",
    "updateOpacity" : function(args){ updateOpacity(args[1])  },
    "showBlank" : showBlank
};

$(function () {
  // if user is running mozilla then use it's built-in WebSocket
  window.WebSocket = window.WebSocket || window.MozWebSocket;

  connection = new WebSocket('ws://127.0.0.1:8887');
  $.connection = connection;
  connection.onopen = function () {
      // connection is opened and ready to use
  };

  connection.onerror = function (error) {
      // an error occurred when sending/receiving data
  };

  /* When a message is recieved from the server, look up the function
  in the instructions table and execute it (if it is a valid
  instruction) */

  connection.onmessage = function (message) {
    var args = message.data.split(",");
    var command = args[0];
    if(DEBUG)   console.log(command);
    if(!instructions[command]){
      showText("Got unknown command from backend server");
      return;
    }

    instructions[command](args);
  };
});

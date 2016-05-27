/*
 * (C) Copyright 2014-2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

var ws = new WebSocket('wss://' + location.host + '/tree');
var video;
var webRtcPeer;
var name = 'presenter';
var that = this;

window.onload = function() {
	console = new Console();
	video = document.getElementById('video');
}

window.onbeforeunload = function() {
	ws.close();
}

ws.onmessage = function(message) {
	var parsedMessage = JSON.parse(message.data);
	console.info('Received message: ' + message.data);

	switch (parsedMessage.id) {
	case 'presenterResponse':
		presenterResponse(parsedMessage);
		break;
	case 'viewerResponse':
		viewerResponse(parsedMessage);
		break;
	case 'stopCommunication':
		dispose();
		break;
	case 'iceCandidate':
		iceCandidate(parsedMessage);
		break;
	default:
		console.error('Unrecognized message', parsedMessage);
	}
}

function presenterResponse(message) {
	if (message.response != 'accepted') {
		var errorMsg = message.message ? message.message : 'Unknow error';
		console.info('Call not accepted for the following reason: ' + errorMsg);
		dispose();
	} else {
		webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
			if (error) return console.error (error);
		});
	}
}

function viewerResponse(message) {
	if (message.response != 'accepted') {
		var errorMsg = message.message ? message.message : 'Unknow error';
		console.info('Call not accepted for the following reason: ' + errorMsg);
		dispose();
	} else {
		webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
			if (error) return console.error (error);
		});
	}
}

//from server
function iceCandidate(message) {
	if (webRtcPeer) {
    	var candidate = {
    			candidate: message.candidate,
    			sdpMid: message.sdpMid,
    			sdpMLineIndex: message.sdpMLineIndex
    	}
	    webRtcPeer.addIceCandidate(candidate, function (error) {
	    	if (error) {
	    		console.error("Error adding candidate: " + error);
	        	return;
	        }
	    });
	} else
		console.error("WebRTC endpoint not initialized yet and already " +
				"recvd an ICE candidate: " + message);
}

//from local peer
function onIceCandidate(candidate) {
	  console.log("Local candidate" + JSON.stringify(candidate));
	  var message = {
	    id: 'onIceCandidate',
	    candidate: candidate.candidate,
	    sdpMLineIndex: candidate.sdpMLineIndex,
	    sdpMid: candidate.sdpMid
	  };
	  sendMessage(message);
}

this.offerToReceiveVideo = function(error, offerSdp, wp){
	if (error) return console.error ("sdp offer error")
	console.log('Invoking SDP offer callback function');
	var msg =  {
			id : name,
			sdpOffer : offerSdp
		};
	sendMessage(msg);
}

function presenter() {
	name = 'presenter';
	if (!webRtcPeer) {
		showSpinner(video);

		var constraints = {
				audio : true,
				video : {
					mandatory : {
						maxWidth : 640,
						maxFrameRate : 15,
						minFrameRate : 15
					}
				}
			};

		var options = {
			      localVideo: video,
			      mediaConstraints: constraints,
			      onicecandidate: that.onIceCandidate.bind(that)
			    }
		webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
				function (error) {
					if(error) {
					  return console.error(error);
					}
					this.generateOffer(that.offerToReceiveVideo.bind(that));
				});
	}
}

function viewer() {
	name = 'viewer';
	if (!webRtcPeer) {
		showSpinner(video);

		var options = {
				remoteVideo: video,
				onicecandidate: that.onIceCandidate.bind(that)
		}

		webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
				function (error) {
				  if(error) {
					  return console.error(error);
				  }
				  this.generateOffer(that.offerToReceiveVideo.bind(that));
		});;
	}
}

function stop() {
	var message = {
		id : 'stop'
	}
	sendMessage(message);
	dispose();
}

function dispose() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;
	}
	hideSpinner(video);
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Sending message: ' + jsonMessage);
	ws.send(jsonMessage);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent.png';
		arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].src = '';
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});

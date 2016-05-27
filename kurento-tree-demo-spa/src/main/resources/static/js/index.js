/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

var tree = new KurentoTree('https://' + location.host + '/kurento-tree');
var video;
var active = false;
var name = 'master';
var that = this;

window.onload = function() {
	console = new Console();
	video = document.getElementById('video');
}

window.onbeforeunload = function() {
	tree.close();
}

function readTreeId() {

	var treeId = $("#broacast-name").val();

	if (!treeId || treeId === "") {
		treeId = "C1";
	}

	return treeId;
}

function master() {

	showSpinner(video);

	var treeId = readTreeId();

	var options = {
		localVideo : video,
		mediaConstraints : {
			audio : true,
			video : {
				mandatory : {
					maxWidth : 640,
					maxFrameRate : 15,
					minFrameRate : 15
				}
			}
		}
	};

	tree.setTreeSource(treeId, options);

	// TODO Disable buttons
}

function viewer() {

	name = 'viewer';

	showSpinner(video);

	var treeId = readTreeId();
	
	var options = {
		remoteVideo : video
	}
	
	tree.addTreeSink(treeId, options);

	// TODO Disable buttons
}

function stop() {
	tree.close();
	hideSpinner(video);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = 'center transparent url("./img/spinner.gif") no-repeat';
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

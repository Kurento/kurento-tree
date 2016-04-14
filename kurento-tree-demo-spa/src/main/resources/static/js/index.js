/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

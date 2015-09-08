"use strict";

var MAX_RETRIES = 2000; // Forever...
var RETRY_TIME_MS = 3000; // FIXME: Implement exponential wait times...
var PING_INTERVAL = 5000;
var PING_MSG = JSON.stringify({
	'method' : 'ping'
});

function WebSocketWithReconnection(config) {

	var closing = false;
	var registerMessageHandler;
	var wsUri = config.uri;
	var reconnecting = false;

	var forcingDisconnection = false;

	var ws = new SockJS(wsUri);

	ws.onopen = function() {
		logConnected(ws, wsUri);
		config.onconnected();
	};

	ws.onerror = function(evt) {
		config.onconnected(evt.data);
	};

	function logConnected(ws, wsUri) {
		try {
			console.log("WebSocket connected to " + wsUri + " with id="
					+ ws._transport.url);
		} catch (e) {
			console.error(e);
		}
	}

	var reconnectionOnClose = function() {
		if (ws.readyState === SockJS.CLOSED) {
			if (closing) {
				console.log("Connection Closed by user");
			} else {
				console.log("Connection closed unexpectecly. Reconnecting...");
				reconnectInNewUri(MAX_RETRIES, 1);
			}
		} else {
			console.log("Close callback from previous websocket. Ignoring it");
		}
	};

	ws.onclose = reconnectionOnClose;

	function reconnectInNewUri(maxRetries, numRetries) {

		console.log("reconnectInNewUri");

		if (numRetries === 1) {
			if (reconnecting) {
				console
						.warn("Trying to reconnect when reconnecting... Ignoring this reconnection.")
				return;
			} else {
				reconnecting = true;
			}

			if (config.onreconnecting) {
				config.onreconnecting();
			}
		}

		if (forcingDisconnection) {
			reconnect(maxRetries, numRetries, wsUri);

		} else {

			if (config.newWsUriOnReconnection) {
				config.newWsUriOnReconnection(function(error, newWsUri) {

					if (error) {
						console.log(error);
						setTimeout(function() {
							reconnectInNewUri(maxRetries, numRetries + 1);
						}, RETRY_TIME_MS);
					} else {
						reconnect(maxRetries, numRetries, newWsUri);
					}
				})
			} else {
				reconnect(maxRetries, numRetries, wsUri);
			}
		}
	}

	// TODO Test retries. How to force not connection?
	function reconnect(maxRetries, numRetries, reconnectWsUri) {

		console.log("Trying to reconnect " + numRetries + " times");

		var newWs = new SockJS(reconnectWsUri);

		newWs.onopen = function() {
			console.log("Reconnected in " + numRetries + " retries...");
			logConnected(newWs, reconnectWsUri);
			reconnecting = false;
			registerMessageHandler();
			if (config.onreconnected()) {
				config.onreconnected();
			}

			newWs.onclose = reconnectionOnClose;
		};

		var onErrorOrClose = function(error) {
			console.log("Reconnection error: " + error);

			if (numRetries === maxRetries) {
				if (config.ondisconnect) {
					config.ondisconnect();
				}
			} else {
				setTimeout(function() {
					reconnectInNewUri(maxRetries, numRetries + 1);
				}, RETRY_TIME_MS);
			}
		};

		newWs.onerror = onErrorOrClose;
		newWs.onclose = onErrorOrClose;

		ws = newWs;
	}

	this.close = function() {
		closing = true;
		ws.close();
	};

	this.forceClose = function(millis) {

		console.log("Testing: Force WebSocket close");

		if (millis) {
			console.log("Testing: Change wsUri for " + millis
					+ " millis to simulate net failure");
			var goodWsUri = wsUri;
			wsUri = "wss://21.234.12.34.4:443/";

			forcingDisconnection = true;

			setTimeout(function() {
				console.log("Testing: Recover good wsUri " + goodWsUri);
				wsUri = goodWsUri;

				forcingDisconnection = false;

			}, millis);
		}

		ws.close();
	};

	this.reconnectWs = function() {
		console.log("reconnectWs");
		reconnectInNewUri(MAX_RETRIES, 1, wsUri);
	};

	this.send = function(message) {
		ws.send(message);
	};

	this.addEventListener = function(type, callback) {
		registerMessageHandler = function() {
			ws.addEventListener(type, callback);
		};

		registerMessageHandler();
	};
}
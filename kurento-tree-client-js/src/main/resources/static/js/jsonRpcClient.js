'use strict';

Date.now = Date.now || function() {
	return +new Date;
};

var PING_INTERVAL = 5000;

var RECONNECTING = 'RECONNECTING';
var CONNECTED = 'CONNECTED';
var DISCONNECTED = 'DISCONNECTED';

var RECONNECTING = "RECONNECTING";
var CONNECTED = "CONNECTED";
var DISCONNECTED = "DISCONNECTED";

/**
 * 
 * heartbeat: interval in ms for each heartbeat message,
 * 
 * <pre>
 * ws : {
 * 	uri : URI to conntect to,
 * 	onconnect : callback method to invoke when connection is successful,
 * 	ondisconnect : callback method to invoke when the connection is lost,
 * 	onreconnecting : callback method to invoke when the client is reconnecting,
 * 	onreconnected : callback method to invoke when the client succesfully reconnects,
 * },
 * rpc : {
 * 	requestTimeout : timeout for a request,
 * 	sessionStatusChanged: callback method for changes in session status,
 * 	mediaRenegotiation: mediaRenegotiation
 * }
 * </pre>
 */
function JsonRpcClient(configuration) {

	var self = this;

	var wsConfig = configuration.ws;

	var notReconnectIfNumLessThan = -1;

	var pingNextNum = 0;
	var enabledPings = true;
	var pingPongStarted = false;
	var pingInterval;

	var status = DISCONNECTED;

	var onreconnecting = wsConfig.onreconnecting;
	var onreconnected = wsConfig.onreconnected;
	var onconnected = wsConfig.onconnected;

	configuration.rpc.pull = function(params, request) {
		request.reply(null, "push");
	}

	wsConfig.onreconnecting = function() {
		console.log("--------- ONRECONNECTING -----------");
		if (status === RECONNECTING) {
			console.error("Websocket already in RECONNECTING state when "
					+ "receiving a new ONRECONNECTING message. Ignoring it");
			return;
		}

		status = RECONNECTING;
		if (onreconnecting) {
			onreconnecting();
		}
	}

	wsConfig.onreconnected = function() {
		console.log("--------- ONRECONNECTED -----------");
		if (status === CONNECTED) {
			console.error("Websocket already in CONNECTED state when "
					+ "receiving a new ONRECONNECTED message. Ignoring it");
			return;
		}
		status = CONNECTED;

		enabledPings = true;
		updateNotReconnectIfLessThan();
		self.enablePing();

		if (onreconnected) {
			onreconnected();
		}
	}

	wsConfig.onconnected = function() {
		console.log("--------- ONCONNECTED -----------");
		if (status === CONNECTED) {
			console.error("Websocket already in CONNECTED state when "
					+ "receiving a new ONCONNECTED message. Ignoring it");
			return;
		}
		status = CONNECTED;

		enabledPings = true;
		self.enablePing();

		if (onconnected) {
			onconnected();
		}
	}

	var ws = new WebSocketWithReconnection(wsConfig);

	console.log('Connecting websocket to URI: ' + wsConfig.uri);

	var rpcBuilderOptions = {
		request_timeout : configuration.rpc.requestTimeout
	};

	var rpc = new RpcBuilder(RpcBuilder.packers.JsonRPC, rpcBuilderOptions, ws,
			function(request) {

				console.debug('Received request: ' + JSON.stringify(request));

				try {

					var func = configuration.rpc[request.method];

					if (func === undefined) {
						console.error("Method " + request.method
								+ " not registered in client");
					} else {
						func(request.params, request);
					}

				} catch (err) {
					console.error('Exception processing request: '
							+ JSON.stringify(request));
					console.error(err);
				}
			});

	this.send = function(method, params, callback) {
		if (method !== 'ping') {
			console.debug('Request: method:' + method + " params:"
					+ JSON.stringify(params));
		}

		var requestTime = Date.now();

		rpc.encode(method, params, function(error, result) {
			if (error) {
				try {
					console.error("ERROR:" + error.message
							+ " in Request: method:" + method + " params:"
							+ JSON.stringify(params));
					if (error.data) {
						console.error("ERROR DATA:"
								+ JSON.stringify(error.data));
					}
				} catch (e) {
				}
				error.requestTime = requestTime;
			}
			if (callback) {
				if (result != undefined && result.message !== 'pong') {
					console.debug('Response: ' + JSON.stringify(result));
				}
				callback(error, result);
			}
		});
	}

	function updateNotReconnectIfLessThan() {
		notReconnectIfNumLessThan = pingNextNum;
		console.log("notReconnectIfNumLessThan = " + notReconnectIfNumLessThan);
	}

	function sendPing() {

		if (enabledPings) {

			var params = null;

			if (pingNextNum == 0 || pingNextNum == notReconnectIfNumLessThan) {
				params = {
					interval : PING_INTERVAL
				};
			}

			pingNextNum++;

			// console.log("Sending ping num="+pingNextNum);
			self.send('ping', params, (function(pingNum) {
				return function(error, result) {
					if (error) {
						// console.log("Received ping response to
						// num="+pingNextNum
						// +" with error "+JSON.stringify(error));
						if (pingNum > notReconnectIfNumLessThan) {
							enabledPings = false;
							updateNotReconnectIfLessThan();
							console.log("DSS did not respond to ping message "
									+ pingNum + ". Reconnecting... ");
							ws.reconnectWs();
						}
					} else {
						// console.log("Received ping response to
						// num="+pingNum);
					}
				}
			})(pingNextNum));
		} else {
			console.log("Trying to send ping, but ping is not enabled");
		}
	}

	this.enablePing = function() {

		if (!pingPongStarted) {

			console.log("Starting ping (if configured)")
			pingPongStarted = true;

			if (configuration.heartbeat != undefined) {
				pingInterval = setInterval(sendPing, configuration.heartbeat);
				sendPing();
			}
		}
	}

	this.close = function() {

		console.log("Closing jsonRpcClient explicitely by client");

		if (pingInterval != undefined) {
			clearInterval(pingInterval);
		}
		pingPongStarted = false;
		enabledPings = false;

		if (configuration.sendCloseMessage) {
			this.send('close_session', null, function(error, result) {
				if (error) {
					console.error("Error sending close message: "
							+ JSON.stringify(error));
				}

				ws.close();
			});
		}
	}

	this.forceClose = function(millis) {
		ws.forceClose(millis);
	}

	this.reconnect = function() {
		ws.reconnectWs();
	}

}

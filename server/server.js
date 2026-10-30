/**
 * DuoLink Private Signaling Server
 * 
 * Production-ready, ultra-lightweight WebSocket router.
 * Transmits only ephemeral signaling messages between paired devices:
 * - Device registration & pairing handshake
 * - WebRTC SDP offers/answers & ICE candidates
 * - Call ringing, accept, reject & end states
 * - Ephemeral encrypted envelope relay fallback
 * 
 * Strict Privacy Guarantee:
 * - NEVER stores messages, call logs, or audio/video streams.
 * - Zero database requirement.
 * - All forwarded data is end-to-end encrypted by the Android clients.
 */

const http = require('http');
const { WebSocketServer, WebSocket } = require('ws');

const PORT = process.env.PORT || 8080;
const PING_INTERVAL_MS = 25000;

// Active clients map: deviceId -> WebSocket connection
const clients = new Map();
// Last seen timestamps: deviceId -> timestamp
const lastSeenMap = new Map();

const server = http.createServer((req, res) => {
    // Health check endpoint
    if (req.url === '/health' || req.url === '/') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            status: 'HEALTHY',
            service: 'DuoLink Signaling Service',
            activeConnections: clients.size,
            uptimeSeconds: Math.floor(process.uptime()),
            timestamp: Date.now()
        }));
        return;
    }
    res.writeHead(404);
    res.end();
});

const wss = new WebSocketServer({ server, path: '/ws' });

function broadcastPresence(deviceId, isOnline) {
    const presencePayload = JSON.stringify({
        type: 'PEER_STATUS',
        peerDeviceId: deviceId,
        isOnline: isOnline,
        lastSeen: Date.now()
    });

    for (const [id, socket] of clients.entries()) {
        if (id !== deviceId && socket.readyState === WebSocket.OPEN) {
            socket.send(presencePayload);
        }
    }
}

wss.on('connection', (ws, req) => {
    let boundDeviceId = null;
    ws.isAlive = true;

    ws.on('pong', () => {
        ws.isAlive = true;
    });

    ws.on('message', (data) => {
        try {
            const message = JSON.parse(data.toString());
            const type = message.type;

            switch (type) {
                case 'REGISTER': {
                    const deviceId = message.deviceId;
                    if (!deviceId || typeof deviceId !== 'string') {
                        ws.send(JSON.stringify({ type: 'ERROR', message: 'Invalid deviceId' }));
                        return;
                    }

                    // If existing socket exists for this device, gracefully close old one
                    if (clients.has(deviceId)) {
                        const oldSocket = clients.get(deviceId);
                        if (oldSocket !== ws) {
                            try {
                                oldSocket.close(4001, 'New session registered');
                            } catch (e) {}
                        }
                    }

                    boundDeviceId = deviceId;
                    clients.set(deviceId, ws);
                    lastSeenMap.set(deviceId, Date.now());

                    // Acknowledge registration
                    ws.send(JSON.stringify({
                        type: 'REGISTER_ACK',
                        deviceId: deviceId,
                        serverTime: Date.now()
                    }));

                    // Broadcast online status to potential peer
                    broadcastPresence(deviceId, true);
                    break;
                }

                case 'PAIR_REQUEST':
                case 'PAIR_CONFIRM':
                case 'CALL_RING':
                case 'CALL_ACCEPT':
                case 'CALL_REJECT':
                case 'CALL_END':
                case 'OFFER':
                case 'ANSWER':
                case 'ICE_CANDIDATE':
                case 'RELAY_MESSAGE':
                case 'TYPING':
                case 'READ_RECEIPT': {
                    const recipientId = message.recipientDeviceId;
                    if (!recipientId) {
                        ws.send(JSON.stringify({ type: 'ERROR', message: 'Missing recipientDeviceId' }));
                        return;
                    }

                    const targetSocket = clients.get(recipientId);
                    if (targetSocket && targetSocket.readyState === WebSocket.OPEN) {
                        // Forward message immediately
                        targetSocket.send(JSON.stringify(message));
                    } else {
                        // Recipient offline notification
                        ws.send(JSON.stringify({
                            type: 'PEER_OFFLINE',
                            targetDeviceId: recipientId,
                            originalMessageType: type,
                            messageId: message.messageId || null
                        }));
                    }
                    break;
                }

                case 'QUERY_STATUS': {
                    const targetId = message.targetDeviceId;
                    const isOnline = clients.has(targetId) && clients.get(targetId).readyState === WebSocket.OPEN;
                    const lastSeen = lastSeenMap.get(targetId) || 0;

                    ws.send(JSON.stringify({
                        type: 'PEER_STATUS',
                        peerDeviceId: targetId,
                        isOnline: isOnline,
                        lastSeen: lastSeen
                    }));
                    break;
                }

                case 'PING': {
                    ws.send(JSON.stringify({ type: 'PONG', timestamp: Date.now() }));
                    break;
                }

                default:
                    ws.send(JSON.stringify({ type: 'ERROR', message: `Unknown message type: ${type}` }));
            }
        } catch (err) {
            console.error('Error processing signaling message:', err.message);
        }
    });

    ws.on('close', () => {
        if (boundDeviceId) {
            clients.delete(boundDeviceId);
            lastSeenMap.set(boundDeviceId, Date.now());
            broadcastPresence(boundDeviceId, false);
        }
    });

    ws.on('error', (err) => {
        console.error('WebSocket connection error:', err.message);
    });
});

// Regular heartbeat check
const interval = setInterval(() => {
    for (const [deviceId, ws] of clients.entries()) {
        if (!ws.isAlive) {
            clients.delete(deviceId);
            lastSeenMap.set(deviceId, Date.now());
            broadcastPresence(deviceId, false);
            ws.terminate();
            continue;
        }
        ws.isAlive = false;
        ws.ping();
    }
}, PING_INTERVAL_MS);

wss.on('close', () => {
    clearInterval(interval);
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`====================================================`);
    console.log(` DuoLink Signaling Server is listening on port ${PORT}`);
    console.log(` WebSocket URL: ws://0.0.0.0:${PORT}/ws`);
    console.log(` Health check:  http://0.0.0.0:${PORT}/health`);
    console.log(`====================================================`);
});


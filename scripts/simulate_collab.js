/**
 * Multi-User Real-Time Collaboration Simulator
 * Connects multiple virtual collaborators over WebSockets/STOMP to simulate concurrent editing,
 * live cursor movements, and conflict-free CRDT synchronization.
 */

import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const SHEET_ID = process.argv[2] || 'sheet-1';
const USER_COUNT = parseInt(process.argv[3] || '3', 10);
const SERVER_URL = process.env.COLLAB_WS_URL || 'http://localhost:8080/ws';

const BOT_PROFILES = [
  { name: 'Alice Walker', color: '#3b82f6', id: 'bot-alice' },
  { name: 'Bob Smith', color: '#10b981', id: 'bot-bob' },
  { name: 'Charlie Kim', color: '#f59e0b', id: 'bot-charlie' },
  { name: 'Diana Prince', color: '#8b5cf6', id: 'bot-diana' },
  { name: 'Evan Wright', color: '#ec4899', id: 'bot-evan' },
];

console.log(`\n🚀 Starting Multi-User Simulation for Sheet: ${SHEET_ID}`);
console.log(`👥 Spawning ${USER_COUNT} virtual collaborators...\n`);

const clients = [];

for (let i = 0; i < Math.min(USER_COUNT, BOT_PROFILES.length); i++) {
  const profile = BOT_PROFILES[i];

  const client = new Client({
    webSocketFactory: () => new SockJS(SERVER_URL),
    reconnectDelay: 5000,
    debug: () => {},
  });

  client.onConnect = () => {
    console.log(`✅ [${profile.name}] Connected (Color: ${profile.color})`);

    // 1. Join Presence
    client.publish({
      destination: `/app/sheet/${SHEET_ID}/presence`,
      body: JSON.stringify({
        sheetId: SHEET_ID,
        action: 'JOIN',
        userName: profile.name,
        color: profile.color,
      }),
    });

    // 2. Subscribe to cell updates
    client.subscribe(`/topic/sheet/${SHEET_ID}/cells`, (msg) => {
      const data = JSON.parse(msg.body);
      // console.log(`[${profile.name}] Received cell update at (${data.row}, ${data.col}): ${data.cellState?.value}`);
    });

    // 3. Periodic realistic collaborative activity (moving cursors + typing values)
    let curRow = 1 + i * 2;
    let curCol = 1;

    setInterval(() => {
      // Move cursor
      curRow = Math.max(1, (curRow + (Math.random() > 0.5 ? 1 : -1)) % 10);
      curCol = Math.max(1, (curCol + (Math.random() > 0.5 ? 1 : -1)) % 6);

      client.publish({
        destination: `/app/sheet/${SHEET_ID}/cursor`,
        body: JSON.stringify({
          sheetId: SHEET_ID,
          row: curRow,
          col: curCol,
          userName: profile.name,
          color: profile.color,
        }),
      });

      // 30% chance to edit a cell
      if (Math.random() < 0.35) {
        const randomVal = (Math.floor(Math.random() * 80) + 10) * 10000;
        const now = Date.now();
        client.publish({
          destination: `/app/sheet/${SHEET_ID}/edit`,
          body: JSON.stringify({
            sheetId: SHEET_ID,
            row: curRow,
            col: curCol,
            cellState: {
              value: String(randomVal),
              timestamp: { physicalTime: now, logicalCounter: 0, clientId: profile.id },
              clientId: profile.id,
              userId: profile.id,
              dataType: 'NUMBER',
            },
          }),
        });
        console.log(`📝 [${profile.name}] Edited cell (${curRow + 1}, ${curCol + 1}) -> $${randomVal.toLocaleString()}`);
      }
    }, 2000 + i * 500);
  };

  client.onStompError = (err) => {
    console.error(`❌ [${profile.name}] STOMP error:`, err.headers['message']);
  };

  client.activate();
  clients.push(client);
}

process.on('SIGINT', () => {
  console.log('\n🛑 Disconnecting simulated collaborators...');
  clients.forEach((c) => c.deactivate());
  process.exit(0);
});

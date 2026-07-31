import { useEffect, useRef, useState } from 'react';
import './App.css';
import './components/table.css'
import BiddingGrid from './components/BiddingGrid';
import CallingGrid from './components/CallingGrid';
import PlayingCard from './components/PlayingCard';
import Table from './components/table';

// Mirrors your GameView record on the backend -- keep this in sync by hand
// for now. We'll refine these types once we're actually rendering real data;
// `any` here is a deliberate placeholder, not a final answer.
type Bid = { suit: string; value: number };

type GameView = {
  state: string;
  seat: number;
  handLengths: number[];
  hand: string[];
  bidHistory: { first: number, second: Bid }[];
  bid: Bid | null;
  trumpBroken: boolean;
  bidder: number | null;
  partner: number | null;
  partnerCard: string | null;
  tricks: number[];
  score: number;
  currentTurn: number;
  played: { first: number; second: string }[];
  hasEnded: boolean;
};

function App() {
  const [connected, setConnected] = useState(false);
  const [seat, setSeat] = useState<number | null>(null);
  const [name, setName] = useState<string>('');
  const [players, setPlayers] = useState<(string | null)[]>(["", "", "", ""]);
  const [latestView, setLatestView] = useState<GameView | null>(null);
  const [log, setLog] = useState<string[]>([]);
  const [selectedCard, setSelectedCard] = useState<string | null>(null);
  const [chatInput, setChatInput] = useState<string>('');
  const [chat, setChat] = useState<string[]>([]);
  // useRef, not useState, for the socket itself: we need a stable reference
  // to the SAME WebSocket object across re-renders so we can call .send()
  // on it later -- useState would be for a VALUE that triggers re-renders
  // when it changes, which isn't what we want for the socket object itself.
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    const socket = new WebSocket(import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/game'); socketRef.current = socket;

    socket.onopen = () => {
      setConnected(true);
      setLog((prev) => [...prev, 'connected'].slice(-5));
    };

    socket.onclose = () => {
      setConnected(false);
      setLog((prev) => [...prev, 'disconnected'].slice(-5));
    };

    socket.onerror = (event) => {
      console.error('WebSocket error:', event);
    };

    socket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      switch (message.type) {
        case 'GAME_STATE':
          setLog((prev) => [...prev, `received: ${message.log}`].slice(-5));
          setPlayers(message.players);
          if (message.gameView != null) {
            setLatestView(message.gameView);
            setSeat(message.gameView.seat);
          }
          break;

        case 'CHAT':
          setChat((prev) => [...prev, `${message.sender}: ${message.message}`].slice(-10));
          break;
      }
    };

    // Cleanup: close the socket if this component ever unmounts
    // (e.g. hot-reload during development).
    return () => {
      socket.close();
    };
  }, []); // empty dependency array: connect once, not on every re-render

  function sendMessage(type: string, payload: object) {
    if (socketRef.current && socketRef.current.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify({ type, payload }));
    } else {
      console.warn('Socket not open, cannot send');
    }
  }

  function handleJoinClick() {
    sendMessage('JOIN_ROOM', { roomId: 'test1', name: name.trim() });
  }

  function sendChat() {
    sendMessage('CHAT', { roomId: 'test1', message: chatInput });
  }

  return (
    <div>
      {/* Title and join room button */}
      <h1>Floating Bridge</h1>
      <p>Status: {connected ? 'connected' : 'disconnected'}</p>
      {seat === null && (
        <div>
          <input
            type="text"
            placeholder="Your name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <button onClick={handleJoinClick} disabled={name.trim() === ''}>
            Join Room
          </button>
        </div>
      )}

      {/* Display players in the room*/}
      <p>Current players: {players.filter((player): player is string => player !== null).join(", ")}</p>

      {/* Bidding grid, only visible during bidding phase */}
      {latestView?.state === 'BIDDING' && (
        <BiddingGrid
          currentBid={latestView.bid}
          yourTurn={latestView.currentTurn === seat}
          onConfirmBid={(bid) => {
            if (bid === null) {
              sendMessage('BID', { roomId: 'test1' });
            } else {
              sendMessage('BID', { roomId: 'test1', suit: bid.suit, value: bid.value });
            }
          }}
        />
      )}

      {/* Card grid to choose a partner card, only visible for bidder during calling phase */}
      {latestView?.state === 'CALLING' && latestView.currentTurn === seat && (
        <CallingGrid
          yourTurn={latestView.currentTurn === seat}
          onConfirmCall={(card) =>
            sendMessage('CALL_PARTNER', { roomId: 'test1', partnerCard: card })
          }
        />
      )}

      {/* Bidder, partner and total tricks so far */}
      {latestView && latestView.state !== "BIDDING" && (
        <>
          <p>Bidder: {players[latestView.bidder!]}</p>
          <p>
            Partner:
            {latestView.partner === null
              ? " Unrevealed"
              : ` ${players[latestView.partner]}`}
          </p>
          <p>Bidder's tricks: {latestView.score}</p>
        </>
      )}

      {/* Shows bid history */}
      {latestView && (
        <>
          <h2>Bid History</h2>
          {latestView.bidHistory.map((p, i) => (
            <p key={i}>
              {players[p.first]}: {p.second.value} {p.second.suit}
            </p>
          ))}
        </>
      )}

      {/* Display whose turn it currently is */}
      {latestView &&
        <h3>{players[latestView.currentTurn]}'s turn</h3>
      }

      {/* Table which shows players and which card they played */}
      {seat !== null && latestView && (
        <Table
          yourSeat={seat}
          players={players}
          handLengths={latestView.handLengths}
          played={latestView.played}
          tricks={latestView.tricks}
          currentTurn={latestView.currentTurn}
        />
      )}

      {/* Cards in your hand and confirm play button*/}
      <h2>Your Hand</h2>

      <div
        style={{
          display: "flex",
          justifyContent: "center",
        }}
      >
        {latestView?.hand.map((card: string, i: number) => (
          <div
            key={card}
            style={{
              marginLeft: i === 0 ? 0 : "-40px",
              zIndex: i,
            }}
          >
            <PlayingCard
              code={card}
              selected={card === selectedCard}
              onClick={() => setSelectedCard(card)}
            />
          </div>
        ))}
      </div>
      {latestView?.state == "PLAYING" && <button
        disabled={selectedCard === null || latestView.currentTurn !== seat}
        onClick={() => {
          if (selectedCard === null) return; // defensive; button should already be disabled
          sendMessage('PLAY_CARD', { roomId: 'test1', card: selectedCard });
          setSelectedCard(null);
        }}
      >
        Confirm Play
      </button>}

      {/* Chat */}
      <h2>💬 Chat</h2>
      <div className="chat-box">
        {chat.map((line, i) => (
          <div key={i}>{line}</div>
        ))}
      </div>
      <input
        value={chatInput}
        onChange={(e) => setChatInput(e.target.value)}
        placeholder="Type and press enter to send"
        onKeyDown={(e) => {
          if (e.key === "Enter") {
            sendChat();
            setChatInput('');
          }
        }}
      />

      {/* Display score, only when game ends */}
      {latestView?.hasEnded &&
        <><h2>Tricks: {latestView.score}-{13 - latestView.score}</h2><h2>Contract {latestView.score >= 6 + latestView.bid!.value ? "Made" : "Failed"}</h2></>
      }

      {/* Message log, mainly for tracing now */}
      <h2>Message log</h2>
      <ul>
        {log.map((entry, i) => (
          <li key={i}>{entry}</li>
        ))}
      </ul>

      {/* <pre>{JSON.stringify(latestView, null, 2)}</pre> */}
    </div>
  )
}

export default App;
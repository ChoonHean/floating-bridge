package server;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import bridge.GameState;
import bridge.GameView;
import bridge.Pair;
import bridge.State;


public class Room {
    private final WebSocketSession[] seats;
    private final String[] players;
    private final PriorityBlockingQueue<Integer> availableSeats;
    private GameState game;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ObjectMapper mapper;
    private final Map<Character, String> suitMapper, valueMapper;

    public Room(ObjectMapper mapper) {
        this.seats = new WebSocketSession[4];
        this.players = new String[4];
        this.availableSeats = new PriorityBlockingQueue<>(List.of(0, 1, 2, 3));
        this.game = new GameState();
        this.mapper = mapper;
        this.suitMapper = new HashMap<>();
        this.suitMapper.put('c', "CLUBS");
        this.suitMapper.put('d', "DIAMONDS");
        this.suitMapper.put('h', "HEARTS");
        this.suitMapper.put('s', "SPADES");
        this.valueMapper = new HashMap<>();
        this.valueMapper.put('A', "ACE");
        this.valueMapper.put('2', "TWO");
        this.valueMapper.put('3', "THREE");
        this.valueMapper.put('4', "FOUR");
        this.valueMapper.put('5', "FIVE");
        this.valueMapper.put('6', "SIX");
        this.valueMapper.put('7', "SEVEN");
        this.valueMapper.put('8', "EIGHT");
        this.valueMapper.put('9', "NINE");
        this.valueMapper.put('T', "TEN");
        this.valueMapper.put('J', "JACK");
        this.valueMapper.put('Q', "QUEEN");
        this.valueMapper.put('K', "KING");
    }

    public synchronized int currentPlayers() {
        return 4 - this.availableSeats.size();
    }

    public synchronized int join(WebSocketSession session, String name) {
        if (this.availableSeats.isEmpty()) {
            return -1;
        }
        if (seatOf(session) != -1) {
            return -2;
        }
        int seat = this.availableSeats.poll();
        this.seats[seat] = session;
        this.players[seat] = name;
        broadcastGameMessage(
                String.format("%s joined. Current players: %d", name, currentPlayers()));
        if (this.availableSeats.isEmpty()) {
            game = game.start();
            broadcastGameMessage("Room is full. Starting game");
        }
        return seat;
    }

    public synchronized int seatOf(WebSocketSession session) {
        int seat = 3;
        while (seat >= 0) {
            if (session.equals(this.seats[seat])) {
                break;
            }
            seat--;
        }
        return seat;
    }

    public synchronized void leave(WebSocketSession session) {
        int seat = seatOf(session);
        if (seat == -1) {
            return;
        }
        this.availableSeats.add(seat);
        this.seats[seat] = null;
        this.players[seat] = null;
        broadcastGameMessage(String.format("%s left the room", this.players[seat]));
    }

    TextMessage makeGameMessage(String message, GameView gameView) throws IOException {
        return new TextMessage(
                mapper.writeValueAsString(new ServerMessage(message, this.players, gameView)));
    }

    TextMessage makeChatMessage(String sender, String message) throws IOException {
        return new TextMessage(mapper.writeValueAsString(new ChatMessage(sender, message)));
    }

    public synchronized void broadcastGameMessage(String message) {
        int seat = 0;
        for (WebSocketSession session : this.seats) {
            if (session != null) {
                try {
                    session.sendMessage(makeGameMessage(message, this.game.getView(seat)));
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
            seat++;
        }
    }

    public synchronized void broadcastChatMessage(TextMessage message) {
        for (WebSocketSession session : this.seats) {
            if (session != null) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    private void handleException(WebSocketSession session, Exception e) throws IOException {
        System.err.println(e);
        session.sendMessage(makeGameMessage(e.getMessage(), null));
    }

    public synchronized void bid(WebSocketSession session, Optional<String> suit,
            Optional<Integer> value) throws IOException {
        int seat = seatOf(session);
        try {
            game = game.bid(seat, suit, value);
            broadcastGameMessage(suit.flatMap(
                    x -> value.map(y -> String.format("%s bidded %d %s", this.players[seat], y, x)))
                    .orElse(this.players[seat] + " passed"));

            if (game.state() == State.WASHING) {
                broadcastGameMessage("All players passed. Washing...");
                scheduler.schedule(() -> {
                    synchronized (this) {
                        game = game.start();
                        broadcastGameMessage("Wash complete");
                    }
                }, 3, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            handleException(session, e);
        }
    }

    private Pair<String, String> decodeCard(String card) {
        String value = this.valueMapper.get(card.charAt(0));
        String suit = this.suitMapper.get(card.charAt(1));
        return new Pair<>(suit, value);
    }

    public synchronized void callPartner(WebSocketSession session, String card) throws IOException {
        int seat = seatOf(session);
        Pair<String, String> pair = decodeCard(card);
        try {
            game = game.callPartner(seat, pair.first(), pair.second());
            broadcastGameMessage(String.format("Partner card: %s %s", pair.second(), pair.first()));
        } catch (Exception e) {
            handleException(session, e);
        }
    }

    public synchronized void playCard(WebSocketSession session, String card) throws IOException {
        int seat = seatOf(session);
        Pair<String, String> pair = decodeCard(card);
        try {
            game = game.playCard(seat, pair.first(), pair.second());
            broadcastGameMessage(String.format("%s played %s %s", this.players[seat], pair.second(),
                    pair.first()));
        } catch (Exception e) {
            handleException(session, e);
        }
    }

    public synchronized void chatMessage(WebSocketSession session, String message) throws IOException {
        int seat = seatOf(session);
        broadcastChatMessage(makeChatMessage(this.players[seat], message));
    }
}

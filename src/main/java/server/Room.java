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
import bridge.Pair;
import bridge.State;


public class Room {
    private final WebSocketSession[] seats;
    private final PriorityBlockingQueue<Integer> availableSeats;
    private GameState game;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ObjectMapper mapper;
    private final Map<Character, String> suitMapper, valueMapper;

    public Room(ObjectMapper mapper) {
        this.seats = new WebSocketSession[4];
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

    public synchronized int join(WebSocketSession session) {
        if (this.availableSeats.isEmpty()) {
            return -1;
        }
        if (seatOf(session) != -1) {
            return -2;
        }
        int seat = this.availableSeats.poll();
        this.seats[seat] = session;
        if (this.availableSeats.isEmpty()) {
            game = game.start();
            broadcast("Room is full. Starting game");
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
        broadcast(String.format("Player %d left the room", seat));
    }

    public synchronized void sendMessage(WebSocketSession session, String message)
            throws IOException {
        session.sendMessage(
                new TextMessage(mapper.writeValueAsString(new ServerMessage(message, null))));
    }

    public synchronized void broadcast(String message) {
        int seat = 0;
        for (WebSocketSession session : this.seats) {
            if (session != null) {
                try {
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(
                            new ServerMessage(message, this.game.getView(seat)))));
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
            seat++;
        }
    }

    private void handleException(WebSocketSession session, Exception e) throws IOException {
        System.err.println(e);
        sendMessage(session, e.getMessage());
    }

    public synchronized void bid(WebSocketSession session, Optional<String> suit,
            Optional<Integer> value) throws IOException {
        int seat = seatOf(session);
        try {
            game = game.bid(seat, suit, value);
            broadcast(suit.flatMap(
                    x -> value.map(y -> String.format("Player %d bidded %d %s", seat, y, x)))
                    .orElse("Player " + seat + " passed"));

            if (game.state() == State.WASHING) {
                broadcast("All players passed. Washing...");
                scheduler.schedule(() -> {
                    synchronized (this) {
                        game = game.start();
                        broadcast("Wash complete");
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
            broadcast(String.format("Partner card: %s %s", pair.second(), pair.first()));
        } catch (Exception e) {
            handleException(session, e);
        }
    }

    public synchronized void playCard(WebSocketSession session, String card) throws IOException {
        int seat = seatOf(session);
        Pair<String, String> pair = decodeCard(card);
        try {
            game = game.playCard(seat, pair.first(), pair.second());
            broadcast(String.format("Player %d played %s %s", seat, pair.second(), pair.first()));
        } catch (Exception e) {
            handleException(session, e);
        }
    }
}

package bridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

public class GameState {
    private static final List<Bid> POSSIBLE_BIDS = IntStream.rangeClosed(1, 7).boxed()
            .flatMap(value -> Arrays.stream(Suit.values()).map(strain -> new Bid(strain, value)))
            .toList();
    private final State state;
    private final List<Hand> hands;
    private final List<Pair<Integer, Bid>> bidHistory;
    private final Bid bid;
    private final boolean trumpBroken;
    private final int bidder;
    private final int partner;
    private final Card partnerCard;
    private final boolean partnerRevealed;
    private final List<Integer> tricks;
    private final int currentTurn;
    private final List<Pair<Integer, Card>> played;

    public GameState() {
        this.state = State.WAITING;
        this.hands = List.of();
        this.bidHistory = new ArrayList<>();
        this.bid = new Bid(Suit.NT, 0);
        this.trumpBroken = false;
        this.partner = 0;
        this.partnerCard = new Card(Suit.NT, Rank.ACE);
        this.partnerRevealed = false;
        this.tricks = List.of(0, 0, 0, 0);
        this.currentTurn = new Random().nextInt(4);
        this.bidder = this.currentTurn;
        this.played = List.of();
    }

    public GameState(int seed) {
        this.state = State.WAITING;
        this.hands = Deck.deal(seed);
        this.bidHistory = new ArrayList<>();
        this.bid = new Bid(Suit.NT, 0);
        this.trumpBroken = false;
        this.bidder = 0;
        this.partner = -1;
        this.partnerCard = new Card(Suit.NT, Rank.ACE);
        this.partnerRevealed = false;
        this.tricks = List.of(0, 0, 0, 0);
        this.currentTurn = 0;
        this.played = List.of();
    }

    private GameState(State state) {
        this.state = State.BIDDING;
        this.hands = Deck.deal();
        this.bidHistory = new ArrayList<>();
        this.bid = new Bid(Suit.NT, 0);
        this.trumpBroken = false;
        this.partner = 0;
        this.partnerCard = new Card(Suit.NT, Rank.ACE);
        this.partnerRevealed = false;
        this.tricks = List.of(0, 0, 0, 0);
        this.currentTurn = new Random().nextInt(4);
        this.bidder = this.currentTurn;
        this.played = List.of();
    }

    private GameState(GameState prev) {
        this.currentTurn = (prev.currentTurn + 1) % 4;
        if (this.currentTurn == prev.bidder && prev.bidHistory.isEmpty()) {
            this.state = State.WASHING;
        } else if (this.currentTurn == prev.bidder) {
            this.state = State.CALLING;
        } else {
            this.state = State.BIDDING;
        }
        this.hands = prev.hands;
        this.bidHistory = prev.bidHistory;
        this.bid = prev.bid;
        this.trumpBroken = prev.trumpBroken;
        this.bidder = prev.bidder;
        this.partner = prev.partner;
        this.partnerCard = prev.partnerCard;
        this.partnerRevealed = prev.partnerRevealed;
        this.tricks = prev.tricks;
        this.played = prev.played;
    }

    private GameState(GameState prev, int player, Bid bid, List<Pair<Integer, Bid>> bidHistory) {
        this.hands = prev.hands;
        this.bidHistory = bidHistory;
        this.bid = bid;
        this.trumpBroken = prev.trumpBroken;
        this.bidder = player;
        this.partner = prev.partner;
        this.partnerCard = prev.partnerCard;
        this.partnerRevealed = prev.partnerRevealed;
        this.tricks = prev.tricks;
        this.currentTurn = (prev.currentTurn + 1) % 4;
        this.played = prev.played;
        this.state = this.currentTurn == this.bidder ? State.CALLING : State.BIDDING;
    }

    private GameState(GameState prev, int partner, Card card) {
        this.hands = prev.hands;
        this.bidHistory = prev.bidHistory;
        this.bid = prev.bid;
        this.trumpBroken = prev.trumpBroken;
        this.bidder = prev.bidder;
        this.partner = partner;
        this.partnerCard = card;
        this.partnerRevealed = prev.partnerRevealed;
        this.tricks = prev.tricks;
        this.currentTurn = this.bid.isNT() ? this.bidder : (this.bidder + 1) % 4;
        this.played = new ArrayList<>();
        this.state = State.PLAYING;
    }

    private GameState(GameState prev, List<Hand> hands, List<Pair<Integer, Card>> played,
            Card card) {
        this.hands = hands;
        this.bidHistory = prev.bidHistory;
        this.bid = prev.bid;
        this.trumpBroken = prev.trumpBroken || card.isSameSuit(this.bid.suit());
        this.bidder = prev.bidder;
        this.partner = prev.partner;
        this.partnerCard = prev.partnerCard;
        this.partnerRevealed = prev.partnerRevealed || card.equals(this.partnerCard);
        this.state = State.PLAYING;
        if (played.size() == 4) {
            this.currentTurn = prev.determineWinner(played);
            this.played = new ArrayList<>();
            this.tricks = new ArrayList<>(prev.tricks);
            this.tricks.set(this.currentTurn, this.tricks.get(this.currentTurn) + 1);
        } else {
            this.currentTurn = (prev.currentTurn + 1) % 4;
            this.played = played;
            this.tricks = prev.tricks;
        }
    }

    public int currentTurn() {
        return this.currentTurn;
    }

    public State state() {
        return this.state;
    }

    public List<Hand> hands() {
        return this.hands;
    }

    public GameState start() {
        return new GameState(State.BIDDING);
    }

    public List<Bid> validBids() {
        return POSSIBLE_BIDS.stream().filter(x -> x.compareTo(this.bid) > 0).toList();
    }

    public GameState bid(int player, Optional<String> suit, Optional<Integer> value) {
        if (this.state != State.BIDDING) {
            throw new IllegalStateException("It is currently not the bidding phase");
        }
        if (this.currentTurn != player) {
            throw new IllegalStateException("It is currently not your turn");
        }

        Optional<Bid> bid = suit.flatMap(x -> value.map(y -> new Bid(Suit.valueOf(x), y)));

        return bid.map(x -> {
            if (!validBids().contains(x)) {
                throw new IllegalArgumentException("Illegal Bid: " + x);
            }
            List<Pair<Integer, Bid>> bidHistory = new ArrayList<>(this.bidHistory);
            bidHistory.add(new Pair<>(player, x));
            return new GameState(this, player, x, bidHistory);
        }).orElseGet(() -> new GameState(this));
    }


    public GameState callPartner(int player, String suit, String value) {
        if (this.state != State.CALLING) {
            throw new IllegalStateException("It is currently not the calling phase");
        }
        if (this.currentTurn != player) {
            throw new IllegalStateException("It is currently not your turn");
        }
        Card card = new Card(Suit.valueOf(suit), Rank.valueOf(value));
        int partner = -1;
        for (int i = 0; i < 4; i++)
            if (this.hands.get(i).contains(card))
                partner = i;
        return new GameState(this, partner, card);
    }

    private int determineWinner(List<Pair<Integer, Card>> cards) {
        return cards.stream()
                .reduce((cur, next) -> cur.second().isSameSuit(next.second())
                        ? next.second().compareTo(cur.second()) > 0 ? next : cur
                        : next.second().isSameSuit(this.bid.suit()) ? next : cur)
                .get().first();
    }

    public List<Card> validPlays() {
        return this.hands.get(this.currentTurn).validPlays(
                this.played.stream().findFirst().map(x -> x.second()), this.trumpBroken,
                this.bid.suit());
    }

    public GameState playCard(int player, String suit, String value) {
        if (this.state != State.PLAYING) {
            throw new IllegalStateException("It is currently not the playing phase");
        }
        if (this.currentTurn != player) {
            throw new IllegalStateException("It is currently not your turn");
        }
        Card card = new Card(Suit.valueOf(suit), Rank.valueOf(value));
        if (!this.hands.get(player).contains(card)) {
            throw new IllegalArgumentException("You currently do not have this card");
        }
        if (!validPlays().contains(card)) {
            throw new IllegalArgumentException("You cannot play this card");
        }
        List<Hand> hands = new ArrayList<>(this.hands);
        hands.set(player, hands.get(player).play(card));
        List<Pair<Integer, Card>> played = new ArrayList<>(this.played);
        played.add(new Pair<>(player, card));
        return new GameState(this, hands, played, card);
    }

    public boolean hasEnded() {
        return this.tricks.stream().reduce(0, (x, y) -> x + y) == 13;
    }

    public int score() {
        return this.tricks.get(this.bidder)
                + (this.partnerRevealed ? this.tricks.get(this.partner) : 0);
    }

    public GameView getView(int seat) {
        if (this.state == State.WAITING) {
            return null;
        } else if (this.bidHistory.isEmpty()) {
            return new GameView(this.state, seat, List.of(13, 13, 13, 13), this.hands.get(seat),
                    this.bidHistory, null, false, null, null, null, null, List.of(0, 0, 0, 0),
                    score(), this.currentTurn, List.of(), hasEnded());
        } else if (!this.partnerRevealed) {
            return new GameView(this.state, seat,
                    this.hands.stream().map(hand -> hand.length()).toList(), this.hands.get(seat),
                    this.bidHistory, this.bid, this.trumpBroken, this.bidder,
                    this.partner == seat ? seat : null,
                    this.state == State.PLAYING ? this.partnerCard : null,
                    this.currentTurn == seat ? validPlays() : null, this.tricks, score(),
                    this.currentTurn, this.played, hasEnded());
        } else {
            return new GameView(this.state, seat,
                    this.hands.stream().map(hand -> hand.length()).toList(), this.hands.get(seat),
                    this.bidHistory, this.bid, this.trumpBroken, this.bidder, this.partner,
                    this.partnerCard, this.currentTurn == seat ? validPlays() : null, this.tricks,
                    score(), this.currentTurn, this.played, hasEnded());
        }
    }
}

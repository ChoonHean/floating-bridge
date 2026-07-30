package bridge;

import java.util.List;

public record GameView(State state, int seat, List<Integer> handLengths, Hand hand,
        List<Pair<Integer, Bid>> bidHistory, Bid bid, boolean trumpBroken, Integer bidder,
        Integer partner, Card partnerCard, List<Card> validPlays, List<Integer> tricks, int score,
        int currentTurn, List<Pair<Integer, Card>> played, boolean hasEnded) {
}

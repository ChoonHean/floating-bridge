package bridge;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonValue;

public class Hand {
    private final List<Card> cards;

    Hand(List<Card> cards) {
        this.cards = cards;
    }

    public int length() {
        return this.cards.size();
    }

    public int points() {
        int pointsFromLongSuits = Arrays.stream(Suit.values())
                .map(suit -> (int) Math
                        .max(cards.stream().filter(card -> card.isSameSuit(suit)).count() - 4, 0))
                .reduce(0, (x, y) -> x + y);
        return cards.stream().map(card -> card.points()).reduce(pointsFromLongSuits, (x, y) -> x + y);
    }

    private boolean hasSuit(Card card) {
        return this.cards.stream().anyMatch(x -> x.isSameSuit(card));
    }

    public List<Card> validPlays(Optional<Card> first, boolean trumpBroken, Suit trump) {
        return first
                .map(card -> hasSuit(card)
                        ? this.cards.stream().filter(x -> x.isSameSuit(card)).toList()
                        : this.cards)
                .orElseGet(
                        () -> this.cards.stream().allMatch(x -> x.isSameSuit(trump)) || trumpBroken
                                ? this.cards
                                : this.cards.stream().filter(x -> !x.isSameSuit(trump)).toList());
    }

    public Hand play(Card card) {
        return new Hand(this.cards.stream().filter(x -> !x.equals(card)).toList());
    }

    public boolean contains(Card card) {
        return this.cards.contains(card);
    }

    @JsonValue
    public List<Card> getCards() {
        return this.cards;
    }

    @Override
    public String toString() {
        return this.cards.toString();
    }
}

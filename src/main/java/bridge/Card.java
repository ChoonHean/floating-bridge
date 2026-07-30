package bridge;

import com.fasterxml.jackson.annotation.JsonValue;

class Card implements Comparable<Card> {
    private final Suit suit;
    private final Rank rank;

    Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public boolean isSameSuit(Suit suit) {
        return this.suit == suit;
    }

    public boolean isSameSuit(Card card) {
        return this.suit == card.suit;
    }

    public int points() {
        return this.rank.points();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Card card) {
            return this.suit.equals(card.suit) && this.rank.equals(card.rank);
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(Card other) {
        if (this.suit.equals(other.suit)) {
            return this.rank.compareTo(other.rank);
        } else {
            return this.suit.compareTo(other.suit);
        }
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(this.rank.symbol()) + this.suit.symbol();
    }
}

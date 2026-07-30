package bridge;

record Bid(Suit suit, int value) implements Comparable<Bid> {

    Bid(Suit suit, int value) {
        this.suit = suit;
        this.value = value;
    }

    public Suit suit() {
        return this.suit;
    }

    @Override
    public int compareTo(Bid other) {
        if (this.value == other.value) {
            return this.suit.compareTo(other.suit);
        } else {
            return Integer.compare(this.value, other.value);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Bid bid) {
            return this.compareTo(bid) == 0;
        }
        return false;
    }

    public boolean isNT() {
        return this.suit == Suit.NT;
    }

    @Override
    public String toString() {
        return String.format("%d %s", this.value, this.suit);
    }
}

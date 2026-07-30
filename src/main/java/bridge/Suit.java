package bridge;

enum Suit {
    // CLUBS("♣"), DIAMONDS("♦"), HEARTS("♥"), SPADES("♠"), NT("NT");

    CLUBS("c"), DIAMONDS("d"), HEARTS("h"), SPADES("s"), NT("NT");

    private final String symbol;

    Suit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return this.symbol;
    }
}

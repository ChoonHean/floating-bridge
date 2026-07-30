package bridge;

enum Rank {
    TWO(0, '2'), THREE(0, '3'), FOUR(0, '4'), FIVE(0, '5'), SIX(0, '6'), SEVEN(0, '7'), EIGHT(0,
            '8'), NINE(0, '9'), TEN(0, 'T'), JACK(1, 'J'), QUEEN(2, 'Q'), KING(3, 'K'), ACE(4, 'A');

    private final int points;
    private final char symbol;

    Rank(int points, char symbol) {
        this.points = points;
        this.symbol = symbol;
    }

    public int points() {
        return this.points;
    }

    public char symbol() {
        return this.symbol;
    }
}

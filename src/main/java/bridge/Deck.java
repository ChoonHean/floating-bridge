package bridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class Deck {
    private static final List<Card> cards = Arrays.stream(Suit.values()).limit(4)
            .flatMap(suit -> Arrays.stream(Rank.values()).map(rank -> new Card(suit, rank)))
            .toList();

    public static List<Hand> deal() {
        ArrayList<Card> cards = new ArrayList<>(Deck.cards);
        while (true) {
            Collections.shuffle(cards);
            Hand hand1 =
                    new Hand(IntStream.range(0, 13).mapToObj(i -> cards.get(i)).sorted().toList());
            if (hand1.points() < 4)
                continue;
            Hand hand2 =
                    new Hand(IntStream.range(13, 26).mapToObj(i -> cards.get(i)).sorted().toList());
            if (hand2.points() < 4)
                continue;
            Hand hand3 =
                    new Hand(IntStream.range(26, 39).mapToObj(i -> cards.get(i)).sorted().toList());
            if (hand3.points() < 4)
                continue;
            Hand hand4 =
                    new Hand(IntStream.range(39, 52).mapToObj(i -> cards.get(i)).sorted().toList());
            if (hand4.points() < 4)
                continue;
            return List.of(hand1, hand2, hand3, hand4);
        }
    }

    public static List<Hand> deal(int seed) {
        ArrayList<Card> cards = new ArrayList<>(Deck.cards);
        while (true) {
            Collections.shuffle(cards, new Random(seed));
            Hand hand1 =
                    new Hand(IntStream.range(0, 13).mapToObj(i -> cards.get(i)).sorted().toList());
            if (hand1.points() < 4)
                continue;
            Hand hand2 =
                    new Hand(IntStream.range(13, 26).mapToObj(i -> cards.get(i)).sorted().toList());
            if (hand2.points() < 4)
                continue;
            Hand hand3 =
                    new Hand(IntStream.range(26, 39).mapToObj(i -> cards.get(i)).sorted().toList());
            if (hand3.points() < 4)
                continue;
            Hand hand4 =
                    new Hand(IntStream.range(39, 52).mapToObj(i -> cards.get(i)).sorted().toList());
            if (hand4.points() < 4)
                continue;
            return List.of(hand1, hand2, hand3, hand4);
        }
    }
}

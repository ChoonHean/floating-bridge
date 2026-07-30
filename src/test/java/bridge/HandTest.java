package bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

public class HandTest {
    @Test
    public void TestPoints() {
        List<Hand> hands = Deck.deal(0);
        Hand hand1 = hands.get(0);
        // [7C, JC, 4D, 7D, QD, KD, AD, TH, QH, 6S, TS, JS, AS]
        assertEquals(18, hand1.points());

        Hand hand2 = hands.get(1);
        // [3C, 9C, QC, AC, 2D, 9D, 6H, 7H, KH, 2S, 4S, 5S, 9S]
        assertEquals(9, hand2.points());

        Hand hand3 = hands.get(2);
        // [2C, 5C, 6C, 8C, TC, 3D, 6D, 8D, 3H, 8H, AH, 3S, KS]
        assertEquals(8, hand3.points());

        Hand hand4 = hands.get(3);
        // [4C, KC, 5D, TD, JD, 2H, 4H, 5H, 9H, JH, 7S, 8S, QS]
        assertEquals(8, hand4.points());
    }

    @Test
    public void TestPresence() {
        HashSet<String> set = new HashSet<>();
        List<Hand> hands = Deck.deal(0);
        for (Hand hand : hands) {
            String[] arr = hand.toString().replaceAll("\\[", "").replace("\\]", "").split(", ");
            for (String s : arr)
                set.add(s);
        }
        assertEquals(52, set.size());
    }

    @Test
    public void TestRemove() {
        List<Hand> hands = Deck.deal(0);
        Hand hand1 = hands.get(0);
        // [7C, JC, 4D, 7D, QD, KD, AD, TH, QH, 6S, TS, JS, AS]
        Card card = new Card(Suit.CLUBS, Rank.SEVEN);
        assertEquals(true, hand1.contains(card));
        hand1 = hand1.play(card);
        assertEquals(false, hand1.contains(card));
    }
}

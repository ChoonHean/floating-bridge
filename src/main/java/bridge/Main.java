package bridge;

import java.util.Optional;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        GameState game = new GameState(0);
        Scanner sc = new Scanner(System.in);
        while (!game.hasEnded()) {
            System.out.println(
                    String.format("Player %d's turn, score: %d", game.currentTurn(), game.score()));
            System.out.println(game.hands());
            if (game.state() == State.BIDDING) {
                System.out.println(game.validBids());
                String s = sc.nextLine();
                if (s.equals("pass"))
                    game = game.bid(game.currentTurn(), Optional.empty(), Optional.empty());
                else {
                    String t = sc.nextLine();
                    game = game.bid(game.currentTurn(), Optional.of(t), Optional.of(Integer.parseInt(s)));
                }
            } else if (game.state() == State.CALLING) {
                String s = sc.nextLine();
                String t = sc.nextLine();
                game = game.callPartner(game.currentTurn(), t, s);
            } else {
                System.out.println(game.validPlays());
                String s = sc.nextLine();
                String t = sc.nextLine();
                game = game.playCard(game.currentTurn(), t, s);
            }
            sc.nextLine();
        }
        System.out.println(game.score());
        sc.close();
    }
}

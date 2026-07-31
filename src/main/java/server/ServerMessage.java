package server;

import bridge.GameView;

record ServerMessage(String type, String log, String[] players, GameView gameView) {
    ServerMessage(String log, String[] players, GameView gameView) {
        this("GAME_STATE", log, players, gameView);
    }
}

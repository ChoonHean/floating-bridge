package server;

import bridge.GameView;

record ServerMessage(String log, String[] players, GameView gameView) {
}

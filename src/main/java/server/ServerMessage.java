package server;

import bridge.GameView;

record ServerMessage(String log, GameView gameView) {
}

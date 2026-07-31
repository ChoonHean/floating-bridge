package server;

record ChatMessage(String type, String sender, String message) {
    ChatMessage(String sender, String message) {
        this("CHAT", sender, message);
    }
}

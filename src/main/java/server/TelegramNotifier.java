package server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Posts a single Telegram message reporting who's currently seated, editing
 * that same message in place on every subsequent join/leave rather than
 * spamming a new message each time.
 *
 * Uses java.net.http.HttpClient (built into the JDK since 11) instead of
 * pulling in a Telegram SDK -- the API surface we need (two POST calls) is
 * small enough that a dedicated library would be more dependency than
 * value.
 *
 * NOTE: messageId is tracked as one field, meaning this only works
 * correctly for a single room. If RoomManager is ever upgraded to support
 * multiple rooms, this needs to become a Map<roomId, Integer> instead.
 */
@Component
public class TelegramNotifier {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    private Integer messageId; // null until the first message is sent

    public synchronized void updateOccupancy(String text) {
        try {
            if (messageId == null) {
                sendNewMessage(text);
            } else {
                editExistingMessage(text);
            }
        } catch (Exception e) {
            // A failed Telegram notification should never break the game
            // itself -- log and move on, same "boundary layer, don't let
            // it take anything else down" reasoning as the WebSocket
            // handler's blanket catch.
            System.err.println("Telegram notification failed: " + e.getMessage());
        }
    }

    private void sendNewMessage(String text) throws Exception {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        String body = mapper.writeValueAsString(Map.of("chat_id", chatId, "text", text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(response.body());
        this.messageId = root.get("result").get("message_id").asInt();
    }

    private void editExistingMessage(String text) throws Exception {
        String url = "https://api.telegram.org/bot" + botToken + "/editMessageText";
        String body = mapper.writeValueAsString(
                Map.of("chat_id", chatId, "message_id", messageId, "text", text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());
        // Not parsing this response -- if the edit fails (e.g. the message
        // is too old for Telegram to allow editing), we deliberately don't
        // crash; the next join/leave will just retry against the same
        // (possibly stale) messageId.
    }
}
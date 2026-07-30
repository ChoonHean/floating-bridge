package server;

import java.io.IOException;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private final RoomManager manager;

    GameWebSocketHandler(RoomManager manager) {
        this.manager = manager;
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(message.getPayload());
            String type = root.get("type").asText();
            JsonNode payload = root.get("payload");
            switch (type) {
                case "JOIN_ROOM" -> {
                    Room room = this.manager.getRoom(payload.get("roomId").asText());
                    int seat = room.join(session);
                    if (seat == -1) {
                        room.sendMessage(session, "room is full");
                    } else if (seat == -2) {
                        room.sendMessage(session, "already joined");
                    } else {
                        room.sendMessage(session, "success: seat " + seat);
                        this.manager.addSession(session, room);
                    }
                }

                case "LEAVE_ROOM" -> {
                    Room room = this.manager.findRoom(session);
                    if (room != null) {
                        room.leave(session);
                        this.manager.removeSession(session);
                    }
                }

                case "BID" -> {
                    Room room = this.manager.getRoom(payload.get("roomId").asText());
                    room.bid(session, Optional.ofNullable(payload.get("suit")).map(x -> x.asText()),
                            Optional.ofNullable(payload.get("value")).map(x -> x.asInt()));
                }

                case "CALL_PARTNER" -> {
                    Room room = this.manager.getRoom(payload.get("roomId").asText());
                    room.callPartner(session, payload.get("partnerCard").asText());
                }

                case "PLAY_CARD" -> {
                    Room room = this.manager.getRoom(payload.get("roomId").asText());
                    room.playCard(session, payload.get("card").asText());
                }

                default -> session.sendMessage(
                        new TextMessage(String.format("unexpected command: %s", type)));
            }
        } catch (Exception e) {
            System.err.println(e);
            session.sendMessage(new TextMessage("bad request: " + e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status)
            throws Exception {
        Room room = this.manager.findRoom(session);
        if (room != null) {
            room.leave(session);
            this.manager.removeSession(session);
        }
        super.afterConnectionClosed(session, status);
    }
}

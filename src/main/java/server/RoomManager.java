package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RoomManager {
    private final Map<String, Room> rooms;
    private final Map<WebSocketSession, Room> sessionFinder;
    private final ObjectMapper mapper;

    RoomManager(ObjectMapper mapper) {
        this.rooms = new ConcurrentHashMap<>();
        this.sessionFinder = new ConcurrentHashMap<>();
        this.mapper = mapper;
    }

    public Room getRoom(String roomId) {
        return this.rooms.computeIfAbsent(roomId, id -> new Room(mapper));
    }

    public Room findRoom(WebSocketSession session) {
        return this.sessionFinder.get(session);
    }

    public void addSession(WebSocketSession session, Room room) {
        this.sessionFinder.put(session, room);
    }

    public void removeSession(WebSocketSession session) {
        this.sessionFinder.remove(session);
    }
}

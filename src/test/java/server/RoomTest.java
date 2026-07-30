package server;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class RoomTest {
    private final ObjectMapper mapper = new ObjectMapper();

    // WebSocketSession is an interface, so Mockito can fake it without a
    // real network connection. We don't need to stub any behavior on it --
    // Room's seats array just needs distinct objects to compare by
    // reference/equals, which a bare mock() already gives us.
    private static WebSocketSession fakeSession() {
        return mock(WebSocketSession.class);
    }

    @Test
    void join_emptyRoom_assignsSeatZero() {
        Room room = new Room(mapper);
        int seat = room.join(fakeSession());
        assertEquals(0, seat, "first joiner should get the lowest available seat");
    }

    @Test
    void join_fourSessions_assignsSeatsZeroThroughThree() {
        Room room = new Room(mapper);
        assertEquals(0, room.join(fakeSession()));
        assertEquals(1, room.join(fakeSession()));
        assertEquals(2, room.join(fakeSession()));
        assertEquals(3, room.join(fakeSession()));
    }

    @Test
    void join_fifthSession_returnsMinusOne() {
        Room room = new Room(mapper);
        room.join(fakeSession());
        room.join(fakeSession());
        room.join(fakeSession());
        room.join(fakeSession());

        int fifth = room.join(fakeSession());
        assertEquals(-1, fifth, "a full room must reject a fifth join rather than overwriting a seat");
    }

    @Test
    void leave_seatedSession_freesTheSeatForReuse() {
        Room room = new Room(mapper);
        WebSocketSession sessionA = fakeSession();
        int seatA = room.join(sessionA); // seat 0

        room.leave(sessionA);
        int seatB = room.join(fakeSession());

        assertEquals(seatA, seatB, "the freed seat should be reassigned to the next joiner");
    }

    @Test
    void leave_unseatedSession_doesNothing() {
        // Regression test for the infinite-loop bug from the original
        // seatOf() -- this call must return promptly, not hang.
        Room room = new Room(mapper);
        WebSocketSession sessionA = fakeSession();
        room.join(sessionA); // occupies seat 0

        WebSocketSession stranger = fakeSession();
        room.leave(stranger); // should be a safe no-op

        // sessionA must still be seated afterward -- leave() on an
        // unrelated session must not have disturbed it.
        assertEquals(0, room.seatOf(sessionA));
    }

    @Test
    void seatOf_seatedSession_returnsCorrectSeat() {
        Room room = new Room(mapper);
        WebSocketSession sessionA = fakeSession();
        room.join(sessionA);

        assertEquals(0, room.seatOf(sessionA));
    }

    @Test
    void seatOf_unseatedSession_returnsMinusOne() {
        Room room = new Room(mapper);
        room.join(fakeSession());

        WebSocketSession stranger = fakeSession();
        assertEquals(-1, room.seatOf(stranger));
    }

    @Test
    void join_alreadySeatedSession_returnsMinusOne() {
        Room room = new Room(mapper);
        WebSocketSession session = fakeSession();
        assertEquals(0, room.join(session));
        assertEquals(-1, room.join(session));
    }
}
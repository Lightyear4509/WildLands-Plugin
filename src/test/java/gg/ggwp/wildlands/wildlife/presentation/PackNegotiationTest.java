package gg.ggwp.wildlands.wildlife.presentation;

import static org.junit.jupiter.api.Assertions.*;
import gg.ggwp.wildlands.geyser.PackNegotiationMonitor;
import org.junit.jupiter.api.Test;

public class PackNegotiationTest {
    public record ResourcePackClientResponsePacket(String getStatus) { public String getStatus() { return getStatus; } }
    public interface Handler { int handle(ResourcePackClientResponsePacket packet); }
    public static final class Session {
        public int delegated;
        private Handler handler = packet -> ++delegated;
        public Handler getPacketHandler() { return handler; }
        public void setPacketHandler(Handler handler) { this.handler = handler; }
    }
    public record Upstream(Session session) { public Session getSession() { return session; } }
    public record Connection(Upstream upstream) { public Upstream getUpstream() { return upstream; } }
    public static Connection connection() { return new Connection(new Upstream(new Session())); }
    @Test void packetsAreDelegatedUnchangedAndOnlyCompleteOfferedPackNegotiationActivates() {
        var connection = connection(); var monitor = new PackNegotiationMonitor(true);
        assertTrue(monitor.attach(connection)); var handler = connection.upstream().session().getPacketHandler();
        assertFalse(monitor.loaded());
        assertEquals(1, handler.handle(new ResourcePackClientResponsePacket("COMPLETED"))); assertFalse(monitor.loaded());
        assertEquals(2, handler.handle(new ResourcePackClientResponsePacket("HAVE_ALL_PACKS"))); assertFalse(monitor.loaded());
        assertEquals(3, handler.handle(new ResourcePackClientResponsePacket("COMPLETED"))); assertTrue(monitor.loaded());
        assertEquals(4, handler.handle(new ResourcePackClientResponsePacket("REFUSED"))); assertFalse(monitor.loaded());
        handler.handle(new ResourcePackClientResponsePacket("COMPLETED")); assertFalse(monitor.loaded());
        assertEquals(5, connection.upstream().session().delegated);
    }
    @Test void absentPackAndUnknownProviderShapeAlwaysStayOnFallback() {
        var missing = new PackNegotiationMonitor(false); assertFalse(missing.attach(connection()));
        missing.observe("HAVE_ALL_PACKS"); missing.observe("COMPLETED"); assertFalse(missing.loaded());
        var unknown = new PackNegotiationMonitor(true); assertFalse(unknown.attach(new Object()));
        unknown.observe("HAVE_ALL_PACKS"); unknown.observe("COMPLETED"); assertFalse(unknown.loaded());
    }
}

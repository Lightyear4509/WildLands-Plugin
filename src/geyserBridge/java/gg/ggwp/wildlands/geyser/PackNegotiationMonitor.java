package gg.ggwp.wildlands.geyser;

import java.lang.reflect.*;

/** Pinned Geyser 2.11.3 compatibility adapter. Observes negotiation; never handles/replaces packets. */
public final class PackNegotiationMonitor {
    private final boolean offered;
    private volatile boolean monitoring, haveAll, complete, refused;
    public PackNegotiationMonitor(boolean offered) { this.offered = offered; }
    public boolean loaded() { return offered && monitoring && haveAll && complete && !refused; }
    public void observe(String status) {
        switch (status) {
            case "REFUSED" -> { refused = true; complete = false; }
            case "HAVE_ALL_PACKS" -> haveAll = true;
            case "COMPLETED" -> complete = haveAll && !refused;
            default -> { }
        }
    }
    public boolean attach(Object connection) {
        if (!offered) return false;
        try {
            Object upstream = connection.getClass().getMethod("getUpstream").invoke(connection);
            Object session = upstream.getClass().getMethod("getSession").invoke(upstream);
            Object original = session.getClass().getMethod("getPacketHandler").invoke(session);
            Class<?> handler = session.getClass().getMethod("getPacketHandler").getReturnType();
            if (original == null || !handler.isInterface()) return false;
            Object observer = Proxy.newProxyInstance(handler.getClassLoader(), new Class<?>[]{handler}, (proxy, method, args) -> {
                Object result;
                try { result = method.invoke(original, args); }
                catch (InvocationTargetException failure) { throw failure.getCause(); }
                if (args != null && args.length == 1 && args[0] != null
                        && args[0].getClass().getSimpleName().equals("ResourcePackClientResponsePacket")) {
                    try { observe(String.valueOf(args[0].getClass().getMethod("getStatus").invoke(args[0]))); }
                    catch (ReflectiveOperationException | RuntimeException unavailable) { monitoring = false; }
                }
                return result;
            });
            session.getClass().getMethod("setPacketHandler", handler).invoke(session, observer);
            monitoring = true; return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException unavailable) { return false; }
    }
}

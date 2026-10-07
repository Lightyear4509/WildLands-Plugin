package gg.ggwp.wildlands.storage;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;

// Serializes all disk access. Failed writes remain pending for the next batch.
public final class StorageService {
    @FunctionalInterface public interface Work<T> { T run() throws Exception; }
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "GGWPWildlands-storage");
        thread.setDaemon(true);
        return thread;
    });
    private final Database database = new Database();
    private final PlayerRepository repository = new PlayerRepository(database);
    private final HydrationRepository hydrationRepository = new HydrationRepository(database);
    private final EnvironmentRepository environmentRepository = new EnvironmentRepository(database);
    private final SeasonRepository seasonRepository = new SeasonRepository(database);
    private final WorldRepository worldRepository = new WorldRepository(database);
    private final Map<String, WorldRecord> pendingWorldIdentities = new LinkedHashMap<>();
    private final Map<UUID, SeasonRecord> pendingSeasons = new LinkedHashMap<>();
    private final Map<UUID, EnvironmentRecord> pendingEnvironment = new LinkedHashMap<>();
    private final Map<UUID, HydrationRecord> pendingHydration = new LinkedHashMap<>();
    private boolean playerWriteFailed, hydrationWriteFailed, environmentWriteFailed, seasonWriteFailed, worldWriteFailed;
    private final Map<UUID, PlayerRecord> pending = new LinkedHashMap<>();
    private final AtomicInteger queue = new AtomicInteger();
    private final Logger logger;
    private volatile String health = "STARTING";
    private volatile int pendingCount;
    private volatile long lastSuccessfulSave;
    private boolean closing;

    public StorageService(Logger logger) { this.logger = logger; }
    public synchronized <T> CompletableFuture<T> submit(Work<T> work) {
        if (closing) return CompletableFuture.failedFuture(new IllegalStateException("Storage is closing"));
        queue.incrementAndGet();
        return CompletableFuture.supplyAsync(() -> {
            try { return work.run(); }
            catch (Exception error) { throw new CompletionException(error); }
            finally { queue.decrementAndGet(); }
        }, worker);
    }
    public void open(Path path) throws Exception { database.open(path); health = "OK"; }
    public Optional<PlayerRecord> find(UUID id) throws Exception { return repository.find(id); }
    public void save(Collection<PlayerRecord> records) throws Exception {
        for (PlayerRecord record : records) pending.merge(record.uuid(), record, (a, b) ->
                new PlayerRecord(a.uuid(), b.lastSeen() >= a.lastSeen() ? b.lastKnownName() : a.lastKnownName(),
                        Math.min(a.firstSeen(), b.firstSeen()), Math.max(a.lastSeen(), b.lastSeen())));
        refreshDiagnostics();
        try {
            repository.saveBatch(pending.values());
            if (!pending.isEmpty()) lastSuccessfulSave = System.currentTimeMillis();
            pending.clear();
            playerWriteFailed = false;
            refreshDiagnostics();
        } catch (Exception failure) { playerWriteFailed = true; refreshDiagnostics(); throw failure; }
    }
    public Optional<HydrationRecord> findHydration(UUID id) throws Exception {
        HydrationRecord unsaved = pendingHydration.get(id);
        return unsaved != null ? Optional.of(unsaved) : hydrationRepository.find(id);
    }

    public void saveHydration(Collection<HydrationRecord> records) throws Exception {
        for (HydrationRecord record : records) pendingHydration.put(record.uuid(), record);
        refreshDiagnostics();
        try {
            hydrationRepository.saveBatch(pendingHydration.values());
            if (!pendingHydration.isEmpty()) lastSuccessfulSave = System.currentTimeMillis();
            pendingHydration.clear();
            hydrationWriteFailed = false;
            refreshDiagnostics();
        } catch (Exception failure) {
            hydrationWriteFailed = true;
            refreshDiagnostics();
            throw failure;
        }
    }
    public Optional<EnvironmentRecord> findEnvironment(UUID id) throws Exception {
        EnvironmentRecord unsaved = pendingEnvironment.get(id);
        return unsaved != null ? Optional.of(unsaved) : environmentRepository.find(id);
    }
    public void saveEnvironment(Collection<EnvironmentRecord> records) throws Exception {
        for (EnvironmentRecord record : records) pendingEnvironment.put(record.uuid(), record);
        refreshDiagnostics();
        try {
            environmentRepository.saveBatch(pendingEnvironment.values());
            if (!pendingEnvironment.isEmpty()) lastSuccessfulSave = System.currentTimeMillis();
            pendingEnvironment.clear(); environmentWriteFailed = false; refreshDiagnostics();
        } catch (Exception failure) { environmentWriteFailed = true; refreshDiagnostics(); throw failure; }
    }

    private void refreshDiagnostics() {
        pendingCount = pending.size() + pendingHydration.size() + pendingEnvironment.size() + pendingSeasons.size() + pendingWorldIdentities.size();
        health = playerWriteFailed || hydrationWriteFailed || environmentWriteFailed || seasonWriteFailed || worldWriteFailed ? "WRITE_FAILED" : "OK";
    }

    public Optional<SeasonRecord> findSeason(UUID worldUuid) throws Exception {
        SeasonRecord pending = pendingSeasons.get(worldUuid);
        return pending == null ? seasonRepository.find(worldUuid) : Optional.of(pending);
    }

    public void saveSeasons(Collection<SeasonRecord> records) throws Exception {
        for (SeasonRecord record : records) pendingSeasons.put(record.worldUuid(), record);
        refreshDiagnostics();
        try {
            seasonRepository.saveBatch(pendingSeasons.values());
            if (!pendingSeasons.isEmpty()) lastSuccessfulSave = System.currentTimeMillis();
            pendingSeasons.clear(); seasonWriteFailed = false; refreshDiagnostics();
        } catch (Exception failure) { seasonWriteFailed = true; refreshDiagnostics(); throw failure; }
    }

    public String health() { return health; }
    public List<WorldRecord> worlds() throws Exception { return worldRepository.all(); }
    public void reserveWorld(WorldRecord record) throws Exception { worldRepository.reserve(record); }
    public void identifyWorld(WorldRecord record) throws Exception {
        pendingWorldIdentities.put(record.name(), record); flushWorldIdentities();
    }
    public void flushWorldIdentities() throws Exception {
        refreshDiagnostics();
        try {
            worldRepository.identifyBatch(pendingWorldIdentities.values());
            if (!pendingWorldIdentities.isEmpty()) lastSuccessfulSave = System.currentTimeMillis();
            pendingWorldIdentities.clear(); worldWriteFailed = false; refreshDiagnostics();
        } catch (Exception failure) { worldWriteFailed = true; refreshDiagnostics(); throw failure; }
    }
    public int queueSize() { return queue.get(); }
    public int pendingCount() { return pendingCount; }
    public long lastSuccessfulSave() { return lastSuccessfulSave; }

    // Invoked only at plugin shutdown. Tasks never wait for the server thread.
    public void close() {
        synchronized (this) {
            if (closing) return;
            closing = true;
            worker.submit(() -> {
                try { save(List.of()); }
                catch (Exception failure) { logger.log(Level.SEVERE, "Final database flush failed; pending records could not be saved", failure); }
                try { saveHydration(List.of()); }
                catch (Exception failure) { logger.log(Level.SEVERE, "Final hydration flush failed; pending records could not be saved", failure); }
                try { saveEnvironment(List.of()); }
                catch (Exception failure) { logger.log(Level.SEVERE, "Final environment flush failed; pending records could not be saved", failure); }
                try { saveSeasons(List.of()); }
                catch (Exception failure) { logger.log(Level.SEVERE, "Final season flush failed; pending records could not be saved", failure); }
                try { flushWorldIdentities(); }
                catch (Exception failure) { logger.log(Level.SEVERE, "Final world identity flush failed", failure); }
                finally {
                    try { database.close(); }
                    catch (Exception failure) { logger.log(Level.SEVERE, "Database close failed", failure); }
                }
            });
            worker.shutdown();
        }
        try {
            if (!worker.awaitTermination(20, TimeUnit.SECONDS))
                logger.severe("Storage shutdown exceeded 20 seconds; worker will continue draining. Do not hot-reload.");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            logger.severe("Interrupted while waiting for database shutdown");
        }
    }
}

import gg.ggwp.wildlands.core.*;
import gg.ggwp.wildlands.wildlife.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class RosterProbe extends JavaPlugin implements org.bukkit.event.Listener {
    private final List<Chunk> tickets = new ArrayList<>();
    private final List<UUID> ids = new ArrayList<>();
    private WildlandsPlugin plugin;
    private Path file;
    private final List<Double> measurements = new ArrayList<>();
    private int measureTicks;
    private org.bukkit.scheduler.BukkitTask visualTask;
    private boolean rendererSuspended;
    public void onEnable() {
        plugin=(WildlandsPlugin)getServer().getPluginManager().getPlugin("GGWPWildlands");
        file=getDataFolder().toPath().resolve("actors.txt"); getCommand("rosterprobe").setExecutor(this);
        getServer().getPluginManager().registerEvents(this,this);
        try { if(Files.exists(file)) Files.readAllLines(file).forEach(line->ids.add(UUID.fromString(line))); } catch(Exception error) { throw new RuntimeException(error); }
    }
    private void check(boolean value,String label) { if(!value) throw new IllegalStateException(label); getLogger().info("PASS "+label); }
    private void area(Runnable action) {
        World world=getServer().getWorld("rainforest_dev"); var ready=CompletableFuture.<Void>completedFuture(null);
        for(int x=126;x<=130;x++) for(int z=126;z<=130;z++) { final int cx=x,cz=z;
            ready=ready.thenCompose(ignored->world.getChunkAtAsync(cx,cz,true).thenCompose(chunk->{ var done=new CompletableFuture<Void>();
                getServer().getScheduler().runTask(this,()->{ chunk.addPluginChunkTicket(this); tickets.add(chunk); done.complete(null); }); return done;
            }));
        }
        ready.whenComplete((ignored,error)->getServer().getScheduler().runTask(this,()->{ try { if(error!=null) throw new RuntimeException(error); action.run(); }
            catch(Exception failure) { getLogger().log(java.util.logging.Level.SEVERE,"Roster probe failed",failure); } }));
    }
    public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) { sender.sendMessage("Disposable-server probe is console only."); return true; }
        if(args.length!=1 || plugin.state()!=WildlandsPlugin.State.READY) return false;
        switch(args[0]) {
            case "spawn" -> area(this::spawn);
            case "inspect" -> area(this::inspect);
            case "retain" -> area(this::retain);
            case "toggle" -> toggle();
            case "motion" -> motion();
            case "debug" -> debug();
            case "full" -> area(this::full);
            case "bench" -> area(this::bench);
            case "models" -> models();
            case "fire" -> fire();
            case "night" -> { getServer().getWorld("rainforest_dev").setTime(18000); getServer().getScheduler().runTaskLater(this,this::status,100); }
            case "day" -> { getServer().getWorld("rainforest_dev").setTime(6000); getServer().getScheduler().runTaskLater(this,this::status,100); }
            case "cleanup" -> { restoreRenderer(); for(UUID id:ids) { Entity entity=getServer().getEntity(id); if(entity!=null) entity.remove(); }
                ids.clear(); for(Chunk chunk:tickets) chunk.removePluginChunkTicket(this); tickets.clear(); try { Files.deleteIfExists(file); } catch(Exception error) { throw new RuntimeException(error); } status(); }
            default -> { return false; }
        }
        return true;
    }
    private void spawn() {
        check(ids.isEmpty(),"no old fixture"); World world=getServer().getWorld("rainforest_dev"); world.setTime(6000);
        for(int x=2030;x<=2070;x++) for(int z=2030;z<=2070;z++) {
            world.getBlockAt(x,239,z).setType(Material.STONE); world.getBlockAt(x,240,z).setType(Material.AIR); world.getBlockAt(x,241,z).setType(Material.AIR);
            if(x==2030||x==2070||z==2030||z==2070) { world.getBlockAt(x,240,z).setType(Material.STONE); world.getBlockAt(x,241,z).setType(Material.STONE); }
        }
        for(int x=2050;x<=2056;x++) for(int z=2042;z<=2055;z++) { world.getBlockAt(x,239,z).setType(Material.WATER); world.getBlockAt(x,238,z).setType(Material.STONE); }
        int[][] points={{2064,2064},{2049,2049},{2062,2060},{2040,2040},{2042,2044},{2045,2040},{2057,2048}};
        int index=0;
        for(Species species:Species.values()) {
            int[] point=points[index++]; UUID id=plugin.wildlife().spawn(new Location(world,point[0]+.5,240,point[1]+.5),species).orElseThrow(); ids.add(id);
            saveIds();
            Mob animal=(Mob)getServer().getEntity(id);
            check(species.nativeClass().isInstance(animal),"native carrier for "+species); check(animal.getHealth()==(species==Species.JAGUAR?20:plugin.configuration().expansion().animal(species).health()),"configured health for "+species);
        }
        ids.add(plugin.wildlife().spawn(new Location(world,2041.5,240,2040.5),Species.TOUCAN).orElseThrow());
        saveIds();
        ids.add(plugin.wildlife().spawn(new Location(world,2045.5,240,2044.5),Species.MONKEY).orElseThrow());
        saveIds();
        for(UUID id:ids) { var animal=(Mob)getServer().getEntity(id); animal.setHealth(Math.max(1,animal.getHealth()-1)); }
        try { Files.createDirectories(file.getParent()); Files.write(file,ids.stream().map(UUID::toString).toList()); } catch(Exception error) { throw new RuntimeException(error); }
        inspect(); getServer().getScheduler().runTaskLater(this,this::status,120);
    }
    private void inspect() {
        try {
            check(ids.size()==9,"nine retained fixture IDs");
            for(UUID id:ids) {
                Mob animal=(Mob)getServer().getEntity(id); check(animal!=null&&animal.isValid(),"same actor loaded "+id);
                Species species=WildlifeManager.species(animal).orElseThrow();
                check(species.nativeClass().isInstance(animal),"species/body identity preserved"); check(animal.isPersistent(),"persistent native body");
                double expected=(species==Species.JAGUAR?20:plugin.configuration().expansion().animal(species).health())-1;
                check(animal.getHealth()==expected,"injured health preserved for "+species);
                if(animal instanceof Tameable tameable) check(!tameable.isTamed(),"owned wildlife remains untamed");
                if(animal instanceof Frog) check(!animal.isAware(),"frog Brain is paused for authoritative hops");
                getLogger().info("ACTOR "+id+" "+species+" health="+animal.getHealth()+" position="+animal.getLocation());
                var renderer=plugin.presentation(); var field=renderer.getClass().getDeclaredField("animals"); field.setAccessible(true); Object view=((Map<?,?>)field.get(renderer)).get(id);
                var create=renderer.getClass().getDeclaredMethod("createDisplays",view.getClass()); create.setAccessible(true); create.invoke(renderer,view);
                var bonesField=view.getClass().getDeclaredField("bones"); bonesField.setAccessible(true); var bones=(List<ItemDisplay>)bonesField.get(view);
                check(bones.size()==7&&bones.stream().allMatch(bone->bone.getItemStack().getItemMeta().getItemModel().getKey().startsWith(species.key()+"/")),"native model pieces for "+species);
                var hitboxField=view.getClass().getDeclaredField("hitbox"); hitboxField.setAccessible(true); var hitbox=(Interaction)hitboxField.get(view);
                check(Math.abs(hitbox.getInteractionWidth()-animal.getWidth())<.001&&Math.abs(hitbox.getInteractionHeight()-animal.getHeight())<.001,"shared hitbox dimensions for "+species);
                var clear=renderer.getClass().getDeclaredMethod("clearDisplays",view.getClass()); clear.setAccessible(true); var copies=new ArrayList<>(bones); clear.invoke(renderer,view);
                check(copies.stream().noneMatch(Entity::isValid)&&!hitbox.isValid()&&animal.isValid(),"no visual leak or animal loss");
            }
            status();
        } catch(Exception error) { throw new RuntimeException(error); }
    }
    private void motion() {
        var before=new HashMap<UUID,Location>(); for(UUID id:ids) { Entity entity=getServer().getEntity(id); if(entity!=null) before.put(id,entity.getLocation()); }
        getServer().getScheduler().runTaskLater(this,()->{
            for(Species species:List.of(Species.TOUCAN,Species.FROG,Species.MONKEY,Species.CAIMAN)) {
                boolean moved=before.entrySet().stream().anyMatch(entry->{ Entity entity=getServer().getEntity(entry.getKey());
                    if(entity==null||WildlifeManager.species(entity).orElseThrow()!=species) return false;
                    var now=entity.getLocation(); double x=now.getX()-entry.getValue().getX(),z=now.getZ()-entry.getValue().getZ(); return x*x+z*z>.1; });
                check(moved,"physical movement for "+species);
            }
            status();
        },200);
    }
    private void debug() {
        try {
            var manager=plugin.wildlife(); var field=manager.getClass().getDeclaredField("loaded"); field.setAccessible(true); var sessions=(Map<?,?>)field.get(manager);
            for(UUID id:ids) { Mob animal=(Mob)getServer().getEntity(id); if(animal==null) continue;
                Object session=sessions.get(id); var type=session.getClass();
                var hop=type.getDeclaredField("lastHop"); hop.setAccessible(true); var goal=type.getDeclaredField("navigationGoal"); goal.setAccessible(true);
                getLogger().info("MOTION "+WildlifeManager.species(animal)+" ground="+animal.isOnGround()+" water="+animal.isInWater()+" velocity="+animal.getVelocity()+" hop="+hop.get(session)+" goal="+goal.get(session)+" pos="+animal.getLocation());
            }
        } catch(Exception error) { throw new RuntimeException(error); }
    }
    private void full() {
        check(ids.isEmpty() && measureTicks==0,"clean load fixture with no active measurement"); World world=getServer().getWorld("rainforest_dev"); world.setTime(6000);
        for(int x=2030;x<=2070;x++) for(int z=2030;z<=2070;z++) {
            world.getBlockAt(x,239,z).setType(Material.STONE);
            for(int y=240;y<=244;y++) world.getBlockAt(x,y,z).setType(Material.AIR);
            if(x==2030||x==2070||z==2030||z==2070) { world.getBlockAt(x,240,z).setType(Material.STONE); world.getBlockAt(x,241,z).setType(Material.STONE); }
        }
        int[] counts={2,8,6,4,4,6,2}; int[] slots=new int[4];
        for(int cell=0;cell<4;cell++) { int bx=cell%2==0?2033:2051,bz=cell<2?2033:2051;
            for(int x=bx+10;x<=bx+12;x++) for(int z=bz+2;z<=bz+12;z++) { world.getBlockAt(x,239,z).setType(Material.WATER); world.getBlockAt(x,238,z).setType(Material.STONE); }
        }
        int speciesIndex=0;
        for(Species species:Species.values()) {
            for(int index=0;index<counts[speciesIndex];index++) {
                int cell=index%4,slot=slots[cell]++,bx=cell%2==0?2033:2051,bz=cell<2?2033:2051;
                Location at=species==Species.CAIMAN?new Location(world,bx+9.5,240,bz+5.5):new Location(world,bx+.5+slot%3*3,240,bz+.5+slot/3*3);
                ids.add(plugin.wildlife().spawn(at,species).orElseThrow(()->new IllegalStateException("spawn "+species+" "+at)));
                saveIds();
            }
            speciesIndex++;
        }
        try { Files.createDirectories(file.getParent()); Files.write(file,ids.stream().map(UUID::toString).toList()); } catch(Exception error) { throw new RuntimeException(error); }
        check(ids.size()==32,"full shared population");
        check(plugin.wildlife().spawn(new Location(world,2034.5,240,2034.5),Species.CAPYBARA).isEmpty(),"33rd animal denied by shared world cap");
        status();
    }
    private void bench() {
        check(measureTicks==0,"no overlapping benchmark"); measurements.clear(); measureTicks=-1;
        getServer().getScheduler().runTaskLater(this,()->{ measureTicks=1200; getLogger().info("BENCH BEGIN actors="+ids.size()); },200);
    }
    private void saveIds() {
        try { Files.createDirectories(file.getParent()); Files.write(file,ids.stream().map(UUID::toString).toList()); }
        catch(Exception error) { throw new RuntimeException(error); }
    }
    private void retain() {
        check(ids.size()==32,"32 retained UUIDs");
        for(UUID id:ids) {
            var animal=(Mob)getServer().getEntity(id); check(animal!=null&&animal.isValid()&&!animal.isDead(),"retained native actor "+id);
            var species=WildlifeManager.species(animal).orElseThrow(); check(species.nativeClass().isInstance(animal),"retained carrier "+species);
            check(animal.getHealth()>0,"retained health "+species);
        }
        check(plugin.wildlife().diagnostics().getFirst().contains("registered=32; loaded=32"),"entire population adopted"); status();
    }
    private void toggle() {
        check(measureTicks==0 && visualTask==null,"toggle outside load measurement"); plugin.wildlife().disable();
        for(UUID id:ids) { var animal=(Mob)getServer().getEntity(id); check(animal!=null&&animal.isValid(),"disable preserves native body");
            if(WildlifeManager.species(animal).orElseThrow()==Species.TOUCAN) check(animal.hasGravity(),"disable restores bird gravity"); }
        check(plugin.presentation().diagnostics().contains("displays=0"),"disable leaves no model pieces");
        plugin.wildlife().enable(); check(plugin.wildlife().diagnostics().getFirst().contains("loaded="+ids.size()),"re-enable adopts fixture");
    }
    private void models() {
        try {
            check(ids.size()==32 && visualTask==null,"full fixture ready for model stress");
            var renderer=plugin.presentation(); var field=renderer.getClass().getDeclaredField("animals"); field.setAccessible(true);
            var all=new ArrayList<>(((Map<?,?>)field.get(renderer)).values()); var type=all.getFirst().getClass();
            var speciesField=type.getDeclaredField("species"); speciesField.setAccessible(true); var views=new ArrayList<Object>();
            for(Species species:Species.values()) for(Object view:all) if(speciesField.get(view)==species) { views.add(view); break; }
            check(views.size()==7,"all seven species represented in model stress");
            for(Object view:all) if(views.size()<12&&!views.contains(view)) views.add(view);
            var task=renderer.getClass().getDeclaredField("task"); task.setAccessible(true); ((org.bukkit.scheduler.BukkitTask)task.get(renderer)).cancel(); task.set(renderer,null);
            rendererSuspended=true;
            var create=renderer.getClass().getDeclaredMethod("createDisplays",type); create.setAccessible(true);
            var update=renderer.getClass().getDeclaredMethod("updateDisplays",type,Location.class,boolean.class,boolean.class); update.setAccessible(true);
            var animalField=type.getDeclaredField("animal"); animalField.setAccessible(true);
            var clock=renderer.getClass().getDeclaredField("ticks"); clock.setAccessible(true);
            for(Object view:views) { create.invoke(renderer,view); var location=((Mob)animalField.get(view)).getLocation(); location.setPitch(0); update.invoke(renderer,view,location,true,true); }
            visualTask=getServer().getScheduler().runTaskTimer(this,()->{ try {
                clock.setLong(renderer,clock.getLong(renderer)+6);
                for(Object view:views) { var animal=(Mob)animalField.get(view); var location=animal.getLocation(); location.setPitch(0); update.invoke(renderer,view,location,true,false); }
            } catch(Exception failure) { restoreRenderer(); throw new RuntimeException(failure); } },6,6);
            check(renderer.diagnostics().contains("displays=84"),"84 real model bones and 12 proxies"); bench();
        } catch(Exception failure) { restoreRenderer(); throw new RuntimeException(failure); }
    }
    private void restoreRenderer() {
        if(!rendererSuspended) return;
        if(visualTask!=null) visualTask.cancel(); visualTask=null; rendererSuspended=false;
        try { var renderer=plugin.presentation(); var field=renderer.getClass().getDeclaredField("animals"); field.setAccessible(true);
            for(Object view:((Map<?,?>)field.get(renderer)).values()) { var clear=renderer.getClass().getDeclaredMethod("clearDisplays",view.getClass()); clear.setAccessible(true); clear.invoke(renderer,view); }
            renderer.enable();
        } catch(Exception failure) { throw new RuntimeException(failure); }
    }
    @org.bukkit.event.EventHandler public void tick(com.destroystokyo.paper.event.server.ServerTickEndEvent event) {
        if(measureTicks<=0) return;
        measurements.add(event.getTickDuration());
        if(--measureTicks==0) { var sorted=measurements.stream().sorted().toList();
            getLogger().info("BENCH RESULT count="+sorted.size()+" mean-ms="+sorted.stream().mapToDouble(Double::doubleValue).average().orElseThrow()+" p95-ms="+sorted.get((int)(sorted.size()*.95))+" max-ms="+sorted.getLast()+" api-average-ms="+getServer().getAverageTickTime()); status(); }
    }
    private void fire() {
        Mob animal=ids.stream().map(getServer()::getEntity).filter(Objects::nonNull).filter(entity->WildlifeManager.species(entity).orElseThrow()==Species.CAIMAN).map(entity->(Mob)entity).findFirst().orElseThrow();
        var at=animal.getLocation().add(2,1,0).getBlock(); var old=at.getBlockData(); at.setType(Material.CAMPFIRE);
        check(((org.bukkit.block.data.type.Campfire)at.getBlockData()).isLit(),"dry lit fire fixture");
        getServer().getScheduler().runTaskLater(this,()->{ try {
            var field=plugin.wildlife().getClass().getDeclaredField("loaded"); field.setAccessible(true); Object session=((Map<?,?>)field.get(plugin.wildlife())).get(animal.getUniqueId());
            var memory=session.getClass().getDeclaredField("memory"); memory.setAccessible(true);
            var state=(gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior.Memory)memory.get(session);
            getLogger().info("FIRE state="+state+" lit="+((org.bukkit.block.data.type.Campfire)at.getBlockData()).isLit());
            check(state.phase()==gg.ggwp.wildlands.wildlife.behaviors.JaguarBehavior.Phase.RETREATING,"native caiman fire retreat");
        } catch(ReflectiveOperationException error) { throw new RuntimeException(error); } finally { at.setBlockData(old); } },80);
    }
    private void status() { plugin.wildlife().diagnostics().forEach(getLogger()::info); getLogger().info(plugin.presentation().diagnostics()); }
    public void onDisable() { restoreRenderer(); for(Chunk chunk:tickets) chunk.removePluginChunkTicket(this); }
}

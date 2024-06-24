package com.cjcrafter.vivecraft;

import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import com.cjcrafter.vivecraft.command.ConstructTabCompleter;
import com.cjcrafter.vivecraft.command.ViveCommand;
import com.cjcrafter.vivecraft.compatibility.CompatibilityAPI;
import com.cjcrafter.vivecraft.listeners.CreatureSpawnListener;
import com.cjcrafter.vivecraft.listeners.VivecraftCombatListener;
import com.cjcrafter.vivecraft.listeners.VivecraftItemListener;
import com.cjcrafter.vivecraft.listeners.VivecraftNetworkListener;
import com.cjcrafter.vivecraft.utils.Headshot;

import java.util.*;
import java.util.concurrent.Callable;

public class VSE extends JavaPlugin implements Listener {
    public final static String CHANNEL = "vivecraft:data";
    private final static int bStatsId = 6931;
    public static Map<UUID, VivePlayer> vivePlayers = new HashMap<>();
    public static VSE me;
    public List<String> blocklist = new ArrayList<>();
    public boolean debug = false;
    public boolean vault;
    FileConfiguration config = getConfig();
    private int sendPosDataTask = 0;

    public static boolean isVive(Player p) {
        if (p == null) return false;
        if (vivePlayers.containsKey(p.getUniqueId())) {
            return vivePlayers.get(p.getUniqueId()).isVR();
        }
        return false;
    }

    public static boolean isCompanion(Player p) {
        if (p == null) return false;
        if (vivePlayers.containsKey(p.getUniqueId())) {
            return !vivePlayers.get(p.getUniqueId()).isVR();
        }
        return false;
    }

    public static boolean isSeated(Player player) {
        if (vivePlayers.containsKey(player.getUniqueId())) {
            return vivePlayers.get(player.getUniqueId()).isSeated();
        }
        return false;
    }

    public static boolean isStanding(Player player) {
        if (vivePlayers.containsKey(player.getUniqueId())) {
            return !vivePlayers.get(player.getUniqueId()).isSeated() && vivePlayers.get(player.getUniqueId()).isVR();
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        super.onEnable();
        me = this;

        if (getConfig().getBoolean("general.vive-crafting", true)) {
            {
                ItemStack is = new ItemStack(Material.LEATHER_BOOTS);
                ItemMeta meta = is.getItemMeta();
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                ((LeatherArmorMeta) meta).setColor(Color.fromRGB(9233775));
                is.setItemMeta(meta);
                is = CompatibilityAPI.getCompatibility().setLocalizedName(is, "vivecraft.item.jumpboots", "Jump Boots");
                ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "jump_boots"), is);
                recipe.shape("B", "S");
                recipe.setIngredient('B', Material.LEATHER_BOOTS);
                recipe.setIngredient('S', Material.SLIME_BLOCK);
                Bukkit.addRecipe(recipe);
            }
            {
                ItemStack is = new ItemStack(Material.SHEARS);
                ItemMeta meta = is.getItemMeta();
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                is.setItemMeta(meta);
                is = CompatibilityAPI.getCompatibility().setLocalizedName(is, "vivecraft.item.climbclaws", "Climb Claws");
                ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "climb_claws"), is);
                recipe.shape("E E", "S S");
                recipe.setIngredient('E', Material.SPIDER_EYE);
                recipe.setIngredient('S', Material.SHEARS);
                Bukkit.addRecipe(recipe);
            }
        }
        try {
            Metrics metrics = new Metrics(this, bStatsId);
            metrics.addCustomChart(new AdvancedPie("vrplayers", new Callable<Map<String, Integer>>() {
                @Override
                public Map<String, Integer> call() throws Exception {
                    int out = 0;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        //counts standing or seated players using VR
                        if (isVive(p)) out++;
                    }
                    Map<String, Integer> valueMap = new HashMap<>();
                    valueMap.put("VR", out);
                    valueMap.put("NonVR", vivePlayers.size() - out);
                    valueMap.put("Vanilla", Bukkit.getOnlinePlayers().size() - vivePlayers.size());
                    return valueMap;
                }
            }));
        } catch (Exception e) {
            getLogger().warning("Could not start bStats metrics");
        }

        // Config Part
        config.options().copyDefaults(true);
        saveDefaultConfig();
        saveConfig();
        saveResource("config-instructions.yml", true);
        ConfigurationSection sec = config.getConfigurationSection("climbey");

        if (sec != null) {
            List<String> temp = sec.getStringList("blocklist");
            //make an attempt to validate these on the server for debugging.
            for (String string : temp) {
                // todo add validation
                blocklist.add(string);
            }
        }
        // end Config part

        getCommand("vive").setExecutor(new ViveCommand(this));
        getCommand("vse").setExecutor(new ViveCommand(this));
        getCommand("vive").setTabCompleter(new ConstructTabCompleter());
        getCommand("vse").setTabCompleter(new ConstructTabCompleter());

        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, new VivecraftNetworkListener(this));
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);

        getServer().getPluginManager().registerEvents(new CreatureSpawnListener(), this);
        getServer().getPluginManager().registerEvents(new VivecraftCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new VivecraftItemListener(), this);

        Headshot.init(this);

        if (getConfig().getBoolean("setSpigotConfig.enabled")) {
            //SpigotConfig.movedWronglyThreshold = getConfig().getDouble("setSpigotConfig.movedWronglyThreshold");
            //SpigotConfig.movedTooQuicklyMultiplier = getConfig().getDouble("setSpigotConfig.movedTooQuickly");
        }

        debug = (getConfig().getBoolean("general.debug", false));

        sendPosDataTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                sendPosData();
            }
        }, 20, 1);


        CheckAllEntities();

        vault = true;
        if (getServer().getPluginManager().getPlugin("Vault") == null || getServer().getPluginManager().getPlugin("Vault").isEnabled() == false) {
            getLogger().severe("Vault not found, permissions groups will not be set");
            vault = false;
        }
    }

    public void CheckAllEntities(){
        List<World> wrl = this.getServer().getWorlds();
        for(World world: wrl){
            for(Entity e: world.getLivingEntities()){
                EditEntity(e);
            }
        }
    }

    public void EditEntity(Entity entity){
        if (entity instanceof Creeper creeper) {
            double newRadius = VSE.me.getConfig().getDouble("CreeperRadius.radius", 3.0);
            CompatibilityAPI.getCompatibility().injectCreeper(creeper, newRadius);
        }
        else if (entity instanceof Enderman enderman) {
            CompatibilityAPI.getCompatibility().injectEnderman(enderman);
        }
    }

    public void sendVRActiveUpdate(VivePlayer v) {
        if(v==null) return;
        var payload = v.getVRPacket();
        for (VivePlayer sendTo : vivePlayers.values()) {

            if (sendTo == null || sendTo.player == null || !sendTo.player.isOnline())
                continue; // dunno y but just in case.

            if (v == sendTo ||v.player == null || !v.player.isOnline()){
                continue;
            }

            sendTo.player.sendPluginMessage(this, CHANNEL, payload);
        }
    }

    public void sendPosData() {

        for (VivePlayer sendTo : vivePlayers.values()) {

            if (sendTo == null || sendTo.player == null || !sendTo.player.isOnline())
                continue; // dunno y but just in case.

            for (VivePlayer v : vivePlayers.values()) {

                if (v == sendTo || v == null || v.player == null || !v.player.isOnline() || v.player.getWorld() != sendTo.player.getWorld() || v.hmdData == null || v.controller0data == null || v.controller1data == null) {
                    continue;
                }

                double d = sendTo.player.getLocation().distanceSquared(v.player.getLocation());

                if (d < 256 * 256) {
                    sendTo.player.sendPluginMessage(this, CHANNEL, v.getUberPacket());
                }
            }
        }
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTask(sendPosDataTask);
        super.onDisable();
    }

    public void setPermissionsGroup(Player p) {
        if (!vault) return;
        if (!getConfig().getBoolean("permissions.enabled")) return;

        Map<String, Boolean> groups = new HashMap<String, Boolean>();

        boolean isvive = isVive(p);
        boolean iscompanion = isCompanion(p);

        String g_vive = getConfig().getString("permissions.vivegroup");
        String g_classic = getConfig().getString("permissions.non-vivegroup");
        if (g_vive != null && !g_vive.trim().isEmpty())
            groups.put(g_vive, isvive);
        if (g_classic != null && !g_classic.trim().isEmpty())
            groups.put(g_classic, iscompanion);

        String g_freemove = getConfig().getString("permissions.freemovegroup");
        if (g_freemove != null && !g_freemove.trim().isEmpty()) {
            if (isvive) {
                groups.put(g_freemove, !vivePlayers.get(p.getUniqueId()).isTeleportMode);
            } else {
                groups.put(g_freemove, false);
            }
        }

        updatePlayerPermissionGroup(p, groups);

    }

    public void updatePlayerPermissionGroup(Player p, Map<String, Boolean> groups) {
        try {

            RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
            Permission perm = rsp.getProvider();

            if (perm == null) {
                getLogger().info("Permissions error: Registered permissions provider is null!");
                return;
            }
            if (!perm.hasGroupSupport()) {
                getLogger().info("Permissions error: Permission plugin does not support groups.");
                return;
            }

            for (Map.Entry<String, Boolean> entry : groups.entrySet()) {
                if (entry.getValue()) {

                    if (!perm.playerInGroup(null, p, entry.getKey())) {
                        if (debug)
                            getLogger().info("Adding " + p.getName() + " to " + entry.getKey());
                        boolean ret = perm.playerAddGroup(null, p, entry.getKey());
                        if (!ret)
                            getLogger().info("Failed adding " + p.getName() + " to " + entry.getKey() + ". Group may not exist.");

                    }
                } else {
                    if (perm.playerInGroup(null, p, entry.getKey())) {
                        if (debug)
                            getLogger().info("Removing " + p.getName() + " from " + entry.getKey());
                        boolean ret = perm.playerRemoveGroup(null, p, entry.getKey());
                        if (!ret)
                            getLogger().info("Failed removing " + p.getName() + " from " + entry.getKey() + ". Group may not exist.");
                    }
                }
            }

        } catch (Exception e) {
            getLogger().severe("Could not set player permission group: " + e.getMessage());
        }
    }

    boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void broadcastConfigString(String node, String playername) {
        String message = this.getConfig().getString(node);
        if (message == null || message.isEmpty()) return;
        String[] formats = message.replace("&player", playername).split("\\n");
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (String line : formats)
                ViveCommand.sendMessage(line, p);
        }
    }

    public void sendWelcomeMessage(Player p) {
        if (!getConfig().getBoolean("welcomemsg.enabled")) return;

        VivePlayer vp = VSE.vivePlayers.get(p.getUniqueId());

        if (vp == null) {
            broadcastConfigString("welcomemsg.welcomeVanilla", p.getDisplayName());
        } else {
            if (vp.isSeated())
                broadcastConfigString("welcomemsg.welcomeSeated", p.getDisplayName());
            else if (!vp.isVR())
                broadcastConfigString("welcomemsg.welcomenonVR", p.getDisplayName());
            else
                broadcastConfigString("welcomemsg.welcomeVR", p.getDisplayName());
        }
    }
}

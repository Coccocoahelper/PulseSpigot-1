package org.bukkit.craftbukkit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.eatthepath.uuid.FastUUID;
import net.minecraft.server.*;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.Warning.WarningState;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.conversations.Conversable;
import org.bukkit.craftbukkit.command.VanillaCommandWrapper;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.generator.CraftChunkData;
import org.bukkit.craftbukkit.help.SimpleHelpMap;
import org.bukkit.craftbukkit.inventory.CraftFurnaceRecipe;
import org.bukkit.craftbukkit.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftItemFactory;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.inventory.CraftShapedRecipe;
import org.bukkit.craftbukkit.inventory.CraftShapelessRecipe;
import org.bukkit.craftbukkit.inventory.RecipeIterator;
import org.bukkit.craftbukkit.map.CraftMapView;
import org.bukkit.craftbukkit.metadata.EntityMetadataStore;
import org.bukkit.craftbukkit.metadata.PlayerMetadataStore;
import org.bukkit.craftbukkit.metadata.WorldMetadataStore;
import org.bukkit.craftbukkit.potion.CraftPotionBrewer;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager;
import org.bukkit.craftbukkit.util.CraftIconCache;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.util.DatFileFilter;
import org.bukkit.craftbukkit.util.Versioning;
import org.bukkit.craftbukkit.util.permissions.CraftDefaultPermissions;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.help.HelpMap;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoadOrder;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.SimpleServicesManager;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.bukkit.scheduler.BukkitWorker;
import org.bukkit.util.StringUtil;
import org.bukkit.util.permissions.DefaultPermissions;
import xyz.krypton.spigot.PulseSpigot;
import xyz.krypton.spigot.async.AsyncPriority;
import xyz.krypton.spigot.config.BukkitConfig;
import xyz.krypton.spigot.config.PaperConfig;
import xyz.krypton.spigot.config.Config;
import xyz.krypton.spigot.config.TacoConfig;
import xyz.krypton.spigot.config.PulseConfig;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.apache.commons.lang.Validate;

import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.SQLitePlatform;
import com.avaje.ebeaninternal.server.lib.sql.TransactionIsolation;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import net.md_5.bungee.api.chat.BaseComponent;
import xyz.krypton.spigot.event.command.UnknownCommandEvent;

public final class CraftServer implements Server {
    private static final Player[] EMPTY_PLAYER_ARRAY = new Player[0];
    private final String serverName = "PulseSpigot"; // PulseSpigot - PulseSpigot
    private final String serverVersion;
    private final String bukkitVersion = Versioning.getBukkitVersion();
    private final Logger logger = Logger.getLogger("Minecraft");
    private final ServicesManager servicesManager = new SimpleServicesManager();
    private final CraftScheduler scheduler = new CraftScheduler();
    private final SimpleCommandMap commandMap = new SimpleCommandMap(this);
    private final SimpleHelpMap helpMap = new SimpleHelpMap(this);
    private final StandardMessenger messenger = new StandardMessenger();
    private final SimplePluginManager pluginManager = new SimplePluginManager(this, commandMap); // Paper
    protected final MinecraftServer console;
    protected final DedicatedPlayerList playerList;
    private final Map<String, World> worlds = new LinkedHashMap<String, World>();
    @Deprecated private YamlConfiguration configuration; // PulseSpigot - deprecate
    private YamlConfiguration commandsConfiguration;
    private final Yaml yaml = new Yaml(new SafeConstructor());
    private final com.github.benmanes.caffeine.cache.Cache<UUID, OfflinePlayer> offlinePlayers = com.github.benmanes.caffeine.cache.Caffeine.newBuilder().softValues().build(); // PulseSpigot - use caffeine
    private final EntityMetadataStore entityMetadata = new EntityMetadataStore();
    private final PlayerMetadataStore playerMetadata = new PlayerMetadataStore();
    private final WorldMetadataStore worldMetadata = new WorldMetadataStore();
    // PulseSpigot start
    public final PulseSpigot pulseSpigot;
    // PulseSpigot - implement modern config system
    public final PulseConfig pulseConfig;
    public final BukkitConfig bukkitConfig;
    public final Config spigotConfig;
    public final PaperConfig paperConfig;
    public final TacoConfig tacoConfig;
    // PulseSpigot end
    private int monsterSpawn = -1;
    private int animalSpawn = -1;
    private int waterAnimalSpawn = -1;
    private int ambientSpawn = -1;
    public int chunkGCPeriod = -1;
    public int chunkGCLoadThresh = 0;
    private File container;
    private WarningState warningState = WarningState.DEFAULT;
    private final BooleanWrapper online = new BooleanWrapper();
    public CraftScoreboardManager scoreboardManager;
    public boolean playerCommandState;
    private boolean printSaveWarning;
    private CraftIconCache icon;
    private boolean overrideAllCommandBlockCommands = false;
    private final Pattern validUserPattern = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");
    private final UUID invalidUserUUID = UUID.nameUUIDFromBytes("InvalidUsername".getBytes(Charsets.UTF_8));
    private final List<CraftPlayer> playerView;
    public int reloadCount;

    private final class BooleanWrapper {
        private boolean value = true;
    }

    static {
        ConfigurationSerialization.registerClass(CraftOfflinePlayer.class);
        CraftItemFactory.instance();
    }

    public CraftServer(MinecraftServer console, PlayerList playerList) {
        this.console = console;
        this.playerList = (DedicatedPlayerList) playerList;
        this.playerView = Collections.unmodifiableList(Lists.transform(playerList.players, new Function<EntityPlayer, CraftPlayer>() {
            @Override
            public CraftPlayer apply(EntityPlayer player) {
                return player.getBukkitEntity();
            }
        }));
        this.serverVersion = CraftServer.class.getPackage().getImplementationVersion();
        online.value = console.getPropertyManager().getBoolean("online-mode", true);

        Bukkit.setServer(this);

        // Register all the Enchantments and PotionTypes now so we can stop new registration immediately after
        Enchantment.DAMAGE_ALL.getClass();
        org.bukkit.enchantments.Enchantment.stopAcceptingRegistrations();

        Potion.setPotionBrewer(new CraftPotionBrewer());
        MobEffectList.BLINDNESS.getClass();
        PotionEffectType.stopAcceptingRegistrations();
        // Ugly hack :(

        if (!Main.useConsole) {
            getLogger().info("Console input is disabled due to --noconsole command argument");
        }

        // PulseSpigot start
        this.pulseSpigot = new PulseSpigot(this.console);
        this.pulseSpigot.init();

        this.pulseConfig = this.pulseSpigot.getPulseSpigotConfig();
        this.bukkitConfig = this.pulseSpigot.getBukkitConfig();
        this.spigotConfig = this.pulseSpigot.getSpigotConfig();
        this.paperConfig = this.pulseSpigot.getPaperConfig();
        this.tacoConfig = this.pulseSpigot.getTacoConfig();

        /* // PulseSpigot - we don't need this anymore, we have our own config system
        configuration = YamlConfiguration.loadConfiguration(getConfigFile());
        configuration.options().copyDefaults(true);
        configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("configurations/bukkit.yml"), Charsets.UTF_8)));
        ConfigurationSection legacyAlias = null;
        if (!configuration.isString("aliases")) {
            legacyAlias = configuration.getConfigurationSection("aliases");
            configuration.set("aliases", "now-in-commands.yml");
        }
        saveConfig();
        if (getCommandsConfigFile().isFile()) {
            legacyAlias = null;
        }*/
        this.configuration = this.pulseSpigot.getBukkitConfiguration();
        commandsConfiguration = YamlConfiguration.loadConfiguration(getCommandsConfigFile());
        commandsConfiguration.options().copyDefaults(true);
        commandsConfiguration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("configurations/commands.yml"), Charsets.UTF_8)));
        saveCommandsConfig();

        /* // PulseSpigot - we don't support migration from old config
        // Migrate aliases from old file and add previously implicit $1- to pass all arguments
        if (legacyAlias != null) {
            ConfigurationSection aliases = commandsConfiguration.createSection("aliases");
            for (String key : legacyAlias.getKeys(false)) {
                ArrayList<String> commands = new ArrayList<String>();

                if (legacyAlias.isList(key)) {
                    for (String command : legacyAlias.getStringList(key)) {
                        commands.add(command + " $1-");
                    }
                } else {
                    commands.add(legacyAlias.getString(key) + " $1-");
                }

                aliases.set(key, commands);
            }
        }

        saveCommandsConfig(); */
        overrideAllCommandBlockCommands = commandsConfiguration.getStringList("command-block-overrides").contains("*");
        ((SimplePluginManager) pluginManager).useTimings(this.bukkitConfig.settings.pluginProfiling);
        monsterSpawn = this.bukkitConfig.spawnLimits.monsters;
        animalSpawn = this.bukkitConfig.spawnLimits.animals;
        waterAnimalSpawn = this.bukkitConfig.spawnLimits.waterAnimals;
        ambientSpawn = this.bukkitConfig.spawnLimits.ambient;
        console.autosavePeriod = this.bukkitConfig.ticksPer.autosave;
        warningState = this.bukkitConfig.settings.deprecatedVerbose;
        chunkGCPeriod = this.bukkitConfig.chunkGC.periodInTicks;
        chunkGCLoadThresh = this.bukkitConfig.chunkGC.loadThreshold;
        // PulseSpigot end
        loadIcon();

        // Spigot Start - Moved to old location of new DedicatedPlayerList in DedicatedServer
        // loadPlugins();
        // enablePlugins(PluginLoadOrder.STARTUP);
        // Spigot End
    }

    public boolean getCommandBlockOverride(String command) {
        return overrideAllCommandBlockCommands || commandsConfiguration.getStringList("command-block-overrides").contains(command);
    }

    private File getConfigFile() {
        return (File) console.options.valueOf("bukkit-settings");
    }

    private File getCommandsConfigFile() {
        return (File) console.options.valueOf("commands-settings");
    }

    private void saveConfig() {
        // PulseSpigot start - Implement modern config system
        /*try {
            configuration.save(getConfigFile());
        } catch (IOException ex) {
            Logger.getLogger(CraftServer.class.getName()).log(Level.SEVERE, "Could not save " + getConfigFile(), ex);
        }*/
        this.bukkitConfig.save();
        // PulseSpigot end
    }

    private void saveCommandsConfig() {
        try {
            commandsConfiguration.save(getCommandsConfigFile());
        } catch (IOException ex) {
            Logger.getLogger(CraftServer.class.getName()).log(Level.SEVERE, "Could not save " + getCommandsConfigFile(), ex);
        }
    }

    public void loadPlugins() {
        pluginManager.registerInterface(JavaPluginLoader.class);

        File pluginFolder = this.getPluginsFolder(); // Paper

        // Paper start
        if (true || pluginFolder.exists()) {
            if (!pluginFolder.exists()) {
                pluginFolder.mkdirs();
            }
             // PulseSpigot start
            List<File> extraJars = this.extraPluginJars();
            if (!Boolean.getBoolean("PulseSpigot.IReallyDontWantSpark") || !Boolean.getBoolean("Purpur.IReallyDontWantSpark")) {
                try {
                    File file = new File("cache", "spark.jar");
                    file.getParentFile().mkdirs();

                    boolean shouldDownload = true;
                    if (file.exists()) {
                        String fileSha1 = String.format("%040x", new java.math.BigInteger(1, java.security.MessageDigest.getInstance("SHA-1").digest(java.nio.file.Files.readAllBytes(file.toPath()))));
                        String sparkSha1;
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(new java.net.URL("https://sparkapi.lucko.me/download/bukkit/sha1").openStream()))) {
                            sparkSha1 = reader.lines().collect(Collectors.joining(""));
                        }

                        if (fileSha1.equals(sparkSha1)) {
                            shouldDownload = false;
                        }
                    }

                    if (shouldDownload) {
                        java.nio.file.Files.copy(new java.net.URL("https://sparkapi.lucko.me/download/bukkit").openStream(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }

                    extraJars.add(file);
                } catch (Exception e) {
                    getLogger().severe("Purpur: Failed to download and install spark plugin");
                    e.printStackTrace();
                }
            }
            Plugin[] plugins = this.pluginManager.loadPlugins(pluginFolder, extraJars);
            // PulseSpigot end
            // Paper end
            for (Plugin plugin : plugins) {
                try {
                    String message = String.format("Loading %s", plugin.getDescription().getFullName());
                    plugin.getLogger().info(message);
                    plugin.onLoad();
                } catch (Throwable ex) {
                    Logger.getLogger(CraftServer.class.getName()).log(Level.SEVERE, ex.getMessage() + " initializing " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
                }
            }
        } else {
            pluginFolder.mkdir();
        }
    }

    // Paper start
    @Override
    public File getPluginsFolder() {
        return (File) console.options.valueOf("plugins");
    }

    private List<File> extraPluginJars() {
        @SuppressWarnings("unchecked")
        final List<File> jars = (List<File>) this.console.options.valuesOf("add-plugin");
        final List<File> list = new ArrayList<>();
        for (final File file : jars) {
            if (!file.exists()) {
                net.minecraft.server.MinecraftServer.LOGGER.warn("File '{}' specified through 'add-plugin' argument does not exist, cannot load a plugin from it!", file.getAbsolutePath());
                continue;
            }
            if (!file.isFile()) {
                net.minecraft.server.MinecraftServer.LOGGER.warn("File '{}' specified through 'add-plugin' argument is not a file, cannot load a plugin from it!", file.getAbsolutePath());
                continue;
            }
            if (!file.getName().endsWith(".jar")) {
                net.minecraft.server.MinecraftServer.LOGGER.warn("File '{}' specified through 'add-plugin' argument is not a jar file, cannot load a plugin from it!", file.getAbsolutePath());
                continue;
            }
            list.add(file);
        }
        return list;
    }
    // Paper end

    public void enablePlugins(PluginLoadOrder type) {
        if (type == PluginLoadOrder.STARTUP) {
            helpMap.clear();
            helpMap.initializeGeneralTopics();
        }

        Plugin[] plugins = pluginManager.getPlugins();

        for (Plugin plugin : plugins) {
            if ((!plugin.isEnabled()) && (plugin.getDescription().getLoad() == type)) {
                loadPlugin(plugin);
            }
        }

        if (type == PluginLoadOrder.POSTWORLD) {
            // Spigot start - Allow vanilla commands to be forced to be the main command
            setVanillaCommands(true);
            commandMap.setFallbackCommands();
            setVanillaCommands(false);
            // Spigot end
            commandMap.registerServerAliases();
            loadCustomPermissions();
            DefaultPermissions.registerCorePermissions();
            CraftDefaultPermissions.registerCorePermissions();
            helpMap.initializeCommands();
            co.aikar.timings.Timings.reset(); // Spigot
        }
    }

    public void disablePlugins() {
        pluginManager.disablePlugins();
    }

    private void setVanillaCommands(boolean first) { // Spigot
        Map<String, ICommand> commands = new CommandDispatcher().getCommands();
        for (ICommand cmd : commands.values()) {
            // Spigot start
            VanillaCommandWrapper wrapper = new VanillaCommandWrapper((CommandAbstract) cmd, LocaleI18n.get(cmd.getUsage(null)));
            if (this.spigotConfig.commands.replaceCommands.contains( wrapper.getName() ) ) { // PulseSpigot
                if (first) {
                    commandMap.register("minecraft", wrapper);
                }
            } else if (!first) {
                commandMap.register("minecraft", wrapper);
            }
            // Spigot end
        }
    }

    private void loadPlugin(Plugin plugin) {
        try {
            pluginManager.enablePlugin(plugin);

            List<Permission> perms = plugin.getDescription().getPermissions();

            for (Permission perm : perms) {
                try {
                    pluginManager.addPermission(perm);
                } catch (IllegalArgumentException ex) {
                    getLogger().log(Level.WARNING, "Plugin " + plugin.getDescription().getFullName() + " tried to register permission '" + perm.getName() + "' but it's already registered", ex);
                }
            }
        } catch (Throwable ex) {
            Logger.getLogger(CraftServer.class.getName()).log(Level.SEVERE, ex.getMessage() + " loading " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
        }
    }

    @Override
    public String getName() {
        return serverName;
    }

    @Override
    public String getVersion() {
        return serverVersion + " (MC: " + console.getVersion() + ")";
    }

    @Override
    public String getBukkitVersion() {
        return bukkitVersion;
    }

    @Override
    @Deprecated
    @SuppressWarnings("unchecked")
    public Player[] _INVALID_getOnlinePlayers() {
        return getOnlinePlayers().toArray(EMPTY_PLAYER_ARRAY);
    }

    @Override
    public List<CraftPlayer> getOnlinePlayers() {
        return this.playerView;
    }

    @Override
    @Deprecated
    public Player getPlayer(final String name) {
        Validate.notNull(name, "Name cannot be null");

        Player found = getPlayerExact(name);
        // Try for an exact match first.
        if (found != null) {
            return found;
        }

        String lowerName = name.toLowerCase();
        int delta = Integer.MAX_VALUE;
        for (Player player : getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(lowerName)) {
                int curDelta = Math.abs(player.getName().length() - lowerName.length());
                if (curDelta < delta) {
                    found = player;
                    delta = curDelta;
                }
                if (curDelta == 0) break;
            }
        }
        return found;
    }

    @Override
    @Deprecated
    public Player getPlayerExact(String name) {
        Validate.notNull(name, "Name cannot be null");

        EntityPlayer player = playerList.getPlayer(name);
        return (player != null) ? player.getBukkitEntity() : null;
    }

    @Override
    public Player getPlayer(UUID id) {
        EntityPlayer player = playerList.a(id);

        if (player != null) {
            return player.getBukkitEntity();
        }

        return null;
    }

    @Override
    public int broadcastMessage(String message) {
        return broadcast(message, BROADCAST_CHANNEL_USERS);
    }

    public Player getPlayer(final EntityPlayer entity) {
        return entity.getBukkitEntity();
    }

    @Override
    @Deprecated
    public List<Player> matchPlayer(String partialName) {
        Validate.notNull(partialName, "PartialName cannot be null");

        List<Player> matchedPlayers = new ArrayList<Player>();

        for (Player iterPlayer : this.getOnlinePlayers()) {
            String iterPlayerName = iterPlayer.getName();

            if (partialName.equalsIgnoreCase(iterPlayerName)) {
                // Exact match
                matchedPlayers.clear();
                matchedPlayers.add(iterPlayer);
                break;
            }
            if (iterPlayerName.toLowerCase().contains(partialName.toLowerCase())) {
                // Partial match
                matchedPlayers.add(iterPlayer);
            }
        }

        return matchedPlayers;
    }

    // PulseSpigot start
    @Override
    public Entity getEntity(UUID uuid) {
        Validate.notNull(uuid, "UUID cannot be null");
        for (WorldServer world : this.getServer().worldServer) {
            net.minecraft.server.Entity entity = world.getEntity(uuid);
            if (entity != null) {
                return entity.getBukkitEntity();
            }
        }
        return null;
    }
    // PulseSpigot end

    @Override
    public int getMaxPlayers() {
        return playerList.getMaxPlayers();
    }

    @Override
    public void setMaxPlayers(int maxPlayers) {
        this.playerList.maxPlayers = maxPlayers;
    }

    // NOTE: These are dependent on the corrisponding call in MinecraftServer
    // so if that changes this will need to as well
    @Override
    public int getPort() {
        return this.getConfigInt("server-port", 25565);
    }

    @Override
    public int getViewDistance() {
        return this.getConfigInt("view-distance", 10);
    }

    @Override
    public String getIp() {
        return this.getConfigString("server-ip", "");
    }

    @Override
    public String getServerName() {
        return this.getConfigString("server-name", "Unknown Server");
    }

    @Override
    public String getServerId() {
        return this.getConfigString("server-id", "unnamed");
    }

    @Override
    public String getWorldType() {
        return this.getConfigString("level-type", "DEFAULT");
    }

    @Override
    public boolean getGenerateStructures() {
        return this.getConfigBoolean("generate-structures", true);
    }

    @Override
    public boolean getAllowEnd() {
        return this.bukkitConfig.settings.allowEnd; // PulseSpigot
    }

    @Override
    public boolean getAllowNether() {
        return this.getConfigBoolean("allow-nether", true);
    }

    public boolean getWarnOnOverload() {
        return this.bukkitConfig.settings.warnOnOverload; // PulseSpigot
    }

    public boolean getQueryPlugins() {
        return this.bukkitConfig.settings.queryPlugins; // PulseSpigot
    }

    @Override
    public boolean hasWhitelist() {
        return this.getConfigBoolean("white-list", false);
    }

    // NOTE: Temporary calls through to server.properies until its replaced
    private String getConfigString(String variable, String defaultValue) {
        return this.console.getPropertyManager().getString(variable, defaultValue);
    }

    private int getConfigInt(String variable, int defaultValue) {
        return this.console.getPropertyManager().getInt(variable, defaultValue);
    }

    private boolean getConfigBoolean(String variable, boolean defaultValue) {
        return this.console.getPropertyManager().getBoolean(variable, defaultValue);
    }

    // End Temporary calls

    @Override
    public String getUpdateFolder() {
        return this.bukkitConfig.settings.updateFolder; // PulseSpigot
    }

    @Override
    public File getUpdateFolderFile() {
        return new File((File) console.options.valueOf("plugins"), this.getUpdateFolder()); // PulseSpigot
    }

    @Override
    public long getConnectionThrottle() {
        // Spigot Start - Automatically set connection throttle for bungee configurations
        if (this.spigotConfig.settings.bungeecord) { // PulseSpigot
            return -1;
        } else {
            return this.bukkitConfig.settings.connectionThrottle; // PulseSpigot
        }
        // Spigot End
    }

    @Override
    public int getTicksPerAnimalSpawns() {
        return this.bukkitConfig.ticksPer.animalSpawns; // PulseSpigot
    }

    @Override
    public int getTicksPerMonsterSpawns() {
        return this.bukkitConfig.ticksPer.monsterSpawn; // PulseSpigot
    }

    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public CraftScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public ServicesManager getServicesManager() {
        return servicesManager;
    }

    @Override
    public List<World> getWorlds() {
        return new ArrayList<World>(worlds.values());
    }

    public DedicatedPlayerList getHandle() {
        return playerList;
    }

    // NOTE: Should only be called from DedicatedServer.ah()
    public boolean dispatchServerCommand(CommandSender sender, ServerCommand serverCommand) {
        if (sender instanceof Conversable) {
            Conversable conversable = (Conversable)sender;

            if (conversable.isConversing()) {
                conversable.acceptConversationInput(serverCommand.command);
                return true;
            }
        }
        try {
            this.playerCommandState = true;
            return dispatchCommand(sender, serverCommand.command);
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Unexpected exception while parsing console command \"" + serverCommand.command + '"', ex);
            return false;
        } finally {
            this.playerCommandState = false;
        }
    }

    @Override
    public boolean dispatchCommand(CommandSender sender, String commandLine) {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(commandLine, "CommandLine cannot be null");

        // PaperSpigot Start
        if (!Bukkit.isPrimaryThread()) {
            final CommandSender fSender = sender;
            final String fCommandLine = commandLine;
            Bukkit.getLogger().log(Level.SEVERE, "Command Dispatched Async: " + commandLine);
            Bukkit.getLogger().log(Level.SEVERE, "Please notify author of plugin causing this execution to fix this bug! see: http://bit.ly/1oSiM6C", new Throwable());
            org.bukkit.craftbukkit.util.Waitable<Boolean> wait = new org.bukkit.craftbukkit.util.Waitable<Boolean>() {
                @Override
                protected Boolean evaluate() {
                    return dispatchCommand(fSender, fCommandLine);
                }
            };
            net.minecraft.server.MinecraftServer.getServer().processQueue.add(wait);
            try {
                return wait.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // This is proper habit for java. If we aren't handling it, pass it on!
            } catch (Exception e) {
                throw new RuntimeException("Exception processing dispatch command", e.getCause());
            }
        }
        // PaperSpigot End

        if (commandMap.dispatch(sender, commandLine)) {
            return true;
        }

        // PulseSpigot start
        UnknownCommandEvent event = new UnknownCommandEvent(sender, commandLine);
        if (!event.callEvent()) return false;
        sender.sendMessage(java.text.MessageFormat.format(this.spigotConfig.messages.unknownCommand, event.getCommand()));
        // PulseSpigot end

        return false;
    }

    @Override
    public void reload() {
        reloadCount++;
        this.pulseSpigot.reloadConfigs(); // PulseSpigot
        this.configuration = this.pulseSpigot.getBukkitConfiguration(); // PulseSpigot
        commandsConfiguration = YamlConfiguration.loadConfiguration(getCommandsConfigFile());
        PropertyManager config = new PropertyManager(console.options);

        ((DedicatedServer) console).propertyManager = config;

        boolean animals = config.getBoolean("spawn-animals", console.getSpawnAnimals());
        boolean monsters = config.getBoolean("spawn-monsters", console.worlds.get(0).getDifficulty() != EnumDifficulty.PEACEFUL);
        EnumDifficulty difficulty = EnumDifficulty.getById(config.getInt("difficulty", console.worlds.get(0).getDifficulty().ordinal()));

        online.value = config.getBoolean("online-mode", console.getOnlineMode());
        console.setSpawnAnimals(config.getBoolean("spawn-animals", console.getSpawnAnimals()));
        console.setPVP(config.getBoolean("pvp", console.getPVP()));
        console.setAllowFlight(config.getBoolean("allow-flight", console.getAllowFlight()));
        console.setMotd(config.getString("motd", console.getMotd()));
        // PulseSpigot start - implement modern config system
        monsterSpawn = this.bukkitConfig.spawnLimits.monsters;
        animalSpawn = this.bukkitConfig.spawnLimits.animals;
        waterAnimalSpawn = this.bukkitConfig.spawnLimits.waterAnimals;
        ambientSpawn = this.bukkitConfig.spawnLimits.ambient;
        warningState = this.bukkitConfig.settings.deprecatedVerbose;
        printSaveWarning = false;
        console.autosavePeriod = this.bukkitConfig.ticksPer.autosave;
        chunkGCPeriod = this.bukkitConfig.chunkGC.periodInTicks;
        chunkGCLoadThresh = this.bukkitConfig.chunkGC.loadThreshold;
        // PulseSpigot end
        loadIcon();

        try {
            playerList.getIPBans().load();
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to load banned-ips.json, " + ex.getMessage());
        }
        try {
            playerList.getProfileBans().load();
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to load banned-players.json, " + ex.getMessage());
        }

        // PulseSpigot start - implement modern config system
        /*org.spigotmc.SpigotConfig.init((File) console.options.valueOf("spigot-settings")); // Spigot
        org.github.paperspigot.PaperSpigotConfig.init((File) console.options.valueOf("paper-settings")); // PaperSpigot*/
        // PulseSpigot end
        for (WorldServer world : console.worlds) {
            world.worldData.setDifficulty(difficulty);
            world.setSpawnFlags(monsters, animals);
            if (this.getTicksPerAnimalSpawns() < 0) {
                world.ticksPerAnimalSpawns = 400;
            } else {
                world.ticksPerAnimalSpawns = this.getTicksPerAnimalSpawns();
            }

            if (this.getTicksPerMonsterSpawns() < 0) {
                world.ticksPerMonsterSpawns = 1;
            } else {
                world.ticksPerMonsterSpawns = this.getTicksPerMonsterSpawns();
            }
            // PulseSpigot start - implement modern config system
            /*world.spigotConfig.init(); // Spigot
            world.paperSpigotConfig.init();*/ // PaperSpigot
            // PulseSpigot end
        }

        pluginManager.clearPlugins();
        commandMap.clearCommands();
        resetRecipes();
        // PulseSpigot start
        //org.spigotmc.SpigotConfig.registerCommands(); // Spigot
        //org.github.paperspigot.PaperSpigotConfig.registerCommands(); // PaperSpigot
        // PulseSpigot end

        overrideAllCommandBlockCommands = commandsConfiguration.getStringList("command-block-overrides").contains("*");

        int pollCount = 0;

        // Wait for at most 2.5 seconds for plugins to close their threads
        while (pollCount < 50 && getScheduler().getActiveWorkers().size() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {}
            pollCount++;
        }

        List<BukkitWorker> overdueWorkers = getScheduler().getActiveWorkers();
        for (BukkitWorker worker : overdueWorkers) {
            Plugin plugin = worker.getOwner();
            String author = "<NoAuthorGiven>";
            if (plugin.getDescription().getAuthors().size() > 0) {
                author = plugin.getDescription().getAuthors().get(0);
            }
            getLogger().log(Level.SEVERE, String.format(
                "Nag author: '%s' of '%s' about the following: %s",
                author,
                plugin.getDescription().getName(),
                "This plugin is not properly shutting down its async tasks when it is being reloaded.  This may cause conflicts with the newly loaded version of the plugin"
            ));
        }
        loadPlugins();
        enablePlugins(PluginLoadOrder.STARTUP);
        enablePlugins(PluginLoadOrder.POSTWORLD);
        getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.RELOAD)); // PulseSpigot
    }

    private void loadIcon() {
        icon = new CraftIconCache(null);
        try {
            final File file = new File(new File("."), "server-icon.png");
            if (file.isFile()) {
                icon = loadServerIcon0(file);
            }
        } catch (Exception ex) {
            getLogger().log(Level.WARNING, "Couldn't load server icon", ex);
        }
    }

    @SuppressWarnings({ "unchecked", "finally" })
    private void loadCustomPermissions() {
        File file = new File(this.bukkitConfig.settings.permissionsFile); // PulseSpigot
        FileInputStream stream;

        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            try {
                file.createNewFile();
            } finally {
                return;
            }
        }

        Map<String, Map<String, Object>> perms;

        try {
            perms = (Map<String, Map<String, Object>>) yaml.load(stream);
        } catch (MarkedYAMLException ex) {
            getLogger().log(Level.WARNING, "Server permissions file " + file + " is not valid YAML: " + ex.toString());
            return;
        } catch (Throwable ex) {
            getLogger().log(Level.WARNING, "Server permissions file " + file + " is not valid YAML.", ex);
            return;
        } finally {
            try {
                stream.close();
            } catch (IOException ex) {}
        }

        if (perms == null) {
            getLogger().log(Level.INFO, "Server permissions file " + file + " is empty, ignoring it");
            return;
        }

        List<Permission> permsList = Permission.loadPermissions(perms, "Permission node '%s' in " + file + " is invalid", Permission.DEFAULT_PERMISSION);

        for (Permission perm : permsList) {
            try {
                pluginManager.addPermission(perm);
            } catch (IllegalArgumentException ex) {
                getLogger().log(Level.SEVERE, "Permission in " + file + " was already defined", ex);
            }
        }
    }

    @Override
    public String toString() {
        return "CraftServer{" + "serverName=" + serverName + ",serverVersion=" + serverVersion + ",minecraftVersion=" + console.getVersion() + '}';
    }

    public World createWorld(String name, World.Environment environment) {
        return WorldCreator.name(name).environment(environment).createWorld();
    }

    public World createWorld(String name, World.Environment environment, long seed) {
        return WorldCreator.name(name).environment(environment).seed(seed).createWorld();
    }

    public World createWorld(String name, Environment environment, ChunkGenerator generator) {
        return WorldCreator.name(name).environment(environment).generator(generator).createWorld();
    }

    public World createWorld(String name, Environment environment, long seed, ChunkGenerator generator) {
        return WorldCreator.name(name).environment(environment).seed(seed).generator(generator).createWorld();
    }

    @Override
    public World createWorld(WorldCreator creator) {
        Validate.notNull(creator, "Creator may not be null");

        String name = creator.name();
        ChunkGenerator generator = creator.generator();
        File folder = new File(getWorldContainer(), name);
        World world = getWorld(name);
        WorldType type = WorldType.getType(creator.type().getName());
        boolean generateStructures = creator.generateStructures();

        if (world != null) {
            return world;
        }

        if ((folder.exists()) && (!folder.isDirectory())) {
            throw new IllegalArgumentException("File exists with the name '" + name + "' and isn't a folder");
        }

        if (generator == null) {
            generator = getGenerator(name);
        }

        Convertable converter = new WorldLoaderServer(getWorldContainer());
        if (converter.isConvertable(name)) {
            getLogger().info("Converting world '" + name + "'");
            converter.convert(name, new IProgressUpdate() {
                private long b = System.currentTimeMillis();

                public void a(String s) {}

                public void a(int i) {
                    if (System.currentTimeMillis() - this.b >= 1000L) {
                        this.b = System.currentTimeMillis();
                        MinecraftServer.LOGGER.info("Converting... " + i + "%");
                    }

                }

                public void c(String s) {}
            });
        }

        int dimension = CraftWorld.CUSTOM_DIMENSION_OFFSET + console.worlds.size();
        boolean used = false;
        do {
            for (WorldServer server : console.worlds) {
                used = server.dimension == dimension;
                if (used) {
                    dimension++;
                    break;
                }
            }
        } while(used);
        boolean hardcore = false;

        IDataManager sdm = new ServerNBTManager(getWorldContainer(), name, true);
        WorldData worlddata = sdm.getWorldData();
        if (worlddata == null) {
            WorldSettings worldSettings = new WorldSettings(creator.seed(), WorldSettings.EnumGamemode.getById(getDefaultGameMode().getValue()), generateStructures, hardcore, type);
            worldSettings.setGeneratorSettings(creator.generatorSettings());
            worlddata = new WorldData(worldSettings, name);
        }
        worlddata.checkName(name); // CraftBukkit - Migration did not rewrite the level.dat; This forces 1.8 to take the last loaded world as respawn (in this case the end)
        WorldServer internal = (WorldServer) new WorldServer(console, sdm, worlddata, dimension, console.methodProfiler, creator.environment(), generator).b();

        if (!(worlds.containsKey(name.toLowerCase()))) {
            return null;
        }

        internal.scoreboard = getScoreboardManager().getMainScoreboard().getHandle();

        internal.tracker = new EntityTracker(internal);
        internal.addIWorldAccess(new WorldManager(console, internal));
        internal.worldData.setDifficulty(EnumDifficulty.EASY);
        internal.setSpawnFlags(true, true);
        console.worlds.add(internal);

        if (generator != null) {
            internal.getWorld().getPopulators().addAll(generator.getDefaultPopulators(internal.getWorld()));
        }

        pluginManager.callEvent(new WorldInitEvent(internal.getWorld()));
        System.out.print("Preparing start region for level " + (console.worlds.size() - 1) + " (Seed: " + internal.getSeed() + ")");

        if (internal.getWorld().getKeepSpawnInMemory()) {
            short short1 = 196;
            long i = System.currentTimeMillis();
            // Paper start
            for (ChunkCoordIntPair coords : internal.chunkProviderServer.getSpiralOutChunks(internal.getSpawn(), short1 >> 4)) {{
                    int j = coords.x;
                    int k = coords.z;
            // Paper end
                    long l = System.currentTimeMillis();

                    if (l < i) {
                        i = l;
                    }

                    if (l > i + 1000L) {
                        int i1 = (short1 * 2 + 1) * (short1 * 2 + 1);
                        int j1 = (j + short1) * (short1 * 2 + 1) + k + 1;

                        System.out.println("Preparing spawn area for " + name + ", " + (j1 * 100 / i1) + "%");
                        i = l;
                    }

                    BlockPosition chunkcoordinates = internal.getSpawn();
                    internal.chunkProviderServer.getChunkAt(chunkcoordinates.getX() + j >> 4, chunkcoordinates.getZ() + k >> 4, true, true, AsyncPriority.URGENT, null); // PulseSpigot - use new methods
                }
            }
        }
        pluginManager.callEvent(new WorldLoadEvent(internal.getWorld()));
        return internal.getWorld();
    }

    @Override
    public boolean unloadWorld(String name, boolean save) {
        return unloadWorld(getWorld(name), save);
    }

    @Override
    public boolean unloadWorld(World world, boolean save) {
        if (world == null) {
            return false;
        }

        WorldServer handle = ((CraftWorld) world).getHandle();

        if (!(console.worlds.contains(handle))) {
            return false;
        }

        if (!(handle.dimension > 1)) {
            return false;
        }

        if (handle.players.size() > 0) {
            return false;
        }

        WorldUnloadEvent e = new WorldUnloadEvent(handle.getWorld());
        pluginManager.callEvent(e);

        if (e.isCancelled()) {
            return false;
        }

        if (save) {
            try {
                handle.save(true, null);
                handle.saveLevel();
            } catch (ExceptionWorldConflict ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        // FlamePaper start - Fix chunk memory leak
        } else {
            ChunkProviderServer chunkProviderServer = handle.chunkProviderServer;
            // PulseSpigot start - Add instanceof check
            if (chunkProviderServer.chunkLoader instanceof ChunkRegionLoader) {
                ChunkRegionLoader regionLoader = (ChunkRegionLoader) chunkProviderServer.chunkLoader;
                regionLoader.b.clear();
                regionLoader.c.clear();
            }
            // PulseSpigot end

            try {
                FileIOThread.a().b();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            chunkProviderServer.unloadChunks(true);
            chunkProviderServer.chunkLoader = null;
            chunkProviderServer.chunkProvider = null;
            chunkProviderServer.chunks.clear();
        }
        // FlamePaper end

        worlds.remove(world.getName().toLowerCase());
        console.worlds.remove(console.worlds.indexOf(handle));

        // KigPaper start - fix memory leak
        CraftingManager craftingManager = CraftingManager.getInstance();
        CraftInventoryView lastView = (CraftInventoryView) craftingManager.lastCraftView;
        if (lastView != null && lastView.getHandle() instanceof ContainerWorkbench && ((ContainerWorkbench) lastView.getHandle()).g == handle) {
            craftingManager.lastCraftView = null;
        }
        // KigPaper end

        File parentFolder = world.getWorldFolder().getAbsoluteFile();

        // Synchronized because access to RegionFileCache.a is guarded by this lock.
        synchronized (RegionFileCache.class) {
            // RegionFileCache.a should be RegionFileCache.cache
            Iterator<Map.Entry<File, RegionFile>> i = RegionFileCache.a.entrySet().iterator();
            while(i.hasNext()) {
                Map.Entry<File, RegionFile> entry = i.next();
                File child = entry.getKey().getAbsoluteFile();
                while (child != null) {
                    if (child.equals(parentFolder)) {
                        i.remove();
                        try {
                            entry.getValue().c(); // Should be RegionFile.close();
                        } catch (IOException ex) {
                            getLogger().log(Level.SEVERE, null, ex);
                        }
                        break;
                    }
                    child = child.getParentFile();
                }
            }
        }

        return true;
    }

    public MinecraftServer getServer() {
        return console;
    }

    @Override
    public World getWorld(String name) {
        Validate.notNull(name, "Name cannot be null");

        return worlds.get(name.toLowerCase());
    }

    @Override
    public World getWorld(UUID uid) {
        for (World world : worlds.values()) {
            if (world.getUID().equals(uid)) {
                return world;
            }
        }
        return null;
    }

    public void addWorld(World world) {
        // Check if a World already exists with the UID.
        if (getWorld(world.getUID()) != null) {
            System.out.println("World " + world.getName() + " is a duplicate of another world and has been prevented from loading. Please delete the uid.dat file from " + world.getName() + "'s world directory if you want to be able to load the duplicate world.");
            return;
        }
        worlds.put(world.getName().toLowerCase(), world);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    // Paper start - JLine update
    /*
    public ConsoleReader getReader() {
        return console.reader;
    }
     */
    // Paper end

    @Override
    public PluginCommand getPluginCommand(String name) {
        Command command = commandMap.getCommand(name);

        if (command instanceof PluginCommand) {
            return (PluginCommand) command;
        } else {
            return null;
        }
    }

    @Override
    public void savePlayers() {
        checkSaveState();
        playerList.savePlayers();
    }

    @Override
    public void configureDbConfig(ServerConfig config) {
        Validate.notNull(config, "Config cannot be null");

        // PulseSpigot start - implement modern config system
        BukkitConfig.Database dbConfig = this.bukkitConfig.database;

        DataSourceConfig ds = new DataSourceConfig();
        ds.setDriver(dbConfig.driver);
        ds.setUrl(dbConfig.url);
        ds.setUsername(dbConfig.username);
        ds.setPassword(dbConfig.password);
        ds.setIsolationLevel(TransactionIsolation.getLevel(dbConfig.isolation));
        // PulseSpigot end

        if (ds.getDriver().contains("sqlite")) {
            config.setDatabasePlatform(new SQLitePlatform());
            config.getDatabasePlatform().getDbDdlSyntax().setIdentity("");
        }

        config.setDataSourceConfig(ds);
    }

    @Override
    public boolean addRecipe(Recipe recipe) {
        CraftRecipe toAdd;
        if (recipe instanceof CraftRecipe) {
            toAdd = (CraftRecipe) recipe;
        } else {
            if (recipe instanceof ShapedRecipe) {
                toAdd = CraftShapedRecipe.fromBukkitRecipe((ShapedRecipe) recipe);
            } else if (recipe instanceof ShapelessRecipe) {
                toAdd = CraftShapelessRecipe.fromBukkitRecipe((ShapelessRecipe) recipe);
            } else if (recipe instanceof FurnaceRecipe) {
                toAdd = CraftFurnaceRecipe.fromBukkitRecipe((FurnaceRecipe) recipe);
            } else {
                return false;
            }
        }
        toAdd.addToCraftingManager();
        CraftingManager.getInstance().sort();
        return true;
    }

    @Override
    public List<Recipe> getRecipesFor(ItemStack result) {
        Validate.notNull(result, "Result cannot be null");

        List<Recipe> results = new ArrayList<Recipe>();
        Iterator<Recipe> iter = recipeIterator();
        while (iter.hasNext()) {
            Recipe recipe = iter.next();
            ItemStack stack = recipe.getResult();
            if (stack.getType() != result.getType()) {
                continue;
            }
            if (result.getDurability() == -1 || result.getDurability() == stack.getDurability()) {
                results.add(recipe);
            }
        }
        return results;
    }

    @Override
    public Iterator<Recipe> recipeIterator() {
        return new RecipeIterator();
    }

    @Override
    public void clearRecipes() {
        CraftingManager.getInstance().recipes.clear();
        RecipesFurnace.getInstance().recipes.clear();
        RecipesFurnace.getInstance().customRecipes.clear();
    }

    @Override
    public void resetRecipes() {
        CraftingManager.getInstance().recipes = new CraftingManager().recipes;
        RecipesFurnace.getInstance().recipes = new RecipesFurnace().recipes;
        RecipesFurnace.getInstance().customRecipes.clear();
    }

    @Override
    public Map<String, String[]> getCommandAliases() {
        ConfigurationSection section = commandsConfiguration.getConfigurationSection("aliases");
        Map<String, String[]> result = new LinkedHashMap<String, String[]>();

        if (section != null) {
            for (String key : section.getKeys(false)) {
                List<String> commands;

                if (section.isList(key)) {
                    commands = section.getStringList(key);
                } else {
                    commands = ImmutableList.of(section.getString(key));
                }

                result.put(key, commands.toArray(new String[commands.size()]));
            }
        }

        return result;
    }

    public void removeBukkitSpawnRadius() {
        // PulseSpigot start - implement modern config system
        /*configuration.set("settings.spawn-radius", null);
        saveConfig();*/
        throw new UnsupportedOperationException("Not supported");
        // PulseSpigot end
    }

    public int getBukkitSpawnRadius() {
        return -1; // PulseSpigot
    }

    @Override
    public String getShutdownMessage() {
        return this.bukkitConfig.settings.shutdownMessage;
    }

    @Override
    public int getSpawnRadius() {
        return ((DedicatedServer) console).propertyManager.getInt("spawn-protection", 16);
    }

    @Override
    public void setSpawnRadius(int value) {
        // PulseSpigot start - implement modern config system
        PropertyManager propertyManager = ((DedicatedServer) console).propertyManager;
        propertyManager.setProperty("spawn-protection", value);
        propertyManager.savePropertiesFile();
        // PulseSpigot end
    }

    @Override
    public boolean getOnlineMode() {
        return online.value;
    }

    @Override
    public boolean getAllowFlight() {
        return console.getAllowFlight();
    }

    @Override
    public boolean isHardcore() {
        return console.isHardcore();
    }

    @Override
    public boolean useExactLoginLocation() {
        return this.bukkitConfig.settings.useExactLoginLocation;
    }

    public ChunkGenerator getGenerator(String world) {
        // PulseSpigot start - implement modern config system
        //ConfigurationSection section = configuration.getConfigurationSection("worlds");
        ChunkGenerator result = null;
        BukkitConfig.WorldConfig worldConfig = this.bukkitConfig.worlds.get(world);
        String name = null;
        if (worldConfig != null) {
            name = worldConfig.getGenerator();
        }


        /*if (section != null) {
            section = section.getConfigurationSection(world);

            if (section != null) {
                String name = section.getString("generator");*/

                if ((name != null) && (!name.equals(""))) {
                    String[] split = name.split(":", 2);
                    String id = (split.length > 1) ? split[1] : null;
                    Plugin plugin = pluginManager.getPlugin(split[0]);

                    if (plugin == null) {
                        getLogger().severe("Could not set generator for default world '" + world + "': Plugin '" + split[0] + "' does not exist");
                    } else if (!plugin.isEnabled()) {
                        getLogger().severe("Could not set generator for default world '" + world + "': Plugin '" + plugin.getDescription().getFullName() + "' is not enabled yet (is it load:STARTUP?)");
                    } else {
                        try {
                            result = plugin.getDefaultWorldGenerator(world, id);
                            if (result == null) {
                                getLogger().severe("Could not set generator for default world '" + world + "': Plugin '" + plugin.getDescription().getFullName() + "' lacks a default world generator");
                            }
                        } catch (Throwable t) {
                            plugin.getLogger().log(Level.SEVERE, "Could not set generator for default world '" + world + "': Plugin '" + plugin.getDescription().getFullName(), t);
                        }
                    }
                }
        /*    }
        }*/
        // PulseSpigot end

        return result;
    }

    @Override
    @Deprecated
    public CraftMapView getMap(short id) {
        PersistentCollection collection = console.worlds.get(0).worldMaps;
        WorldMap worldmap = (WorldMap) collection.get(WorldMap.class, "map_" + id);
        if (worldmap == null) {
            return null;
        }
        return worldmap.mapView;
    }

    @Override
    public CraftMapView createMap(World world) {
        Validate.notNull(world, "World cannot be null");

        net.minecraft.server.ItemStack stack = new net.minecraft.server.ItemStack(Items.MAP, 1, -1);
        WorldMap worldmap = Items.FILLED_MAP.getSavedMap(stack, ((CraftWorld) world).getHandle());
        return worldmap.mapView;
    }

    @Override
    public void shutdown() {
        console.safeShutdown();
    }

    @Override
    public int broadcast(String message, String permission) {
        int count = 0;
        Set<Permissible> permissibles = getPluginManager().getPermissionSubscriptions(permission);

        for (Permissible permissible : permissibles) {
            if (permissible instanceof CommandSender && permissible.hasPermission(permission)) {
                CommandSender user = (CommandSender) permissible;
                user.sendMessage(message);
                count++;
            }
        }

        return count;
    }

    // Paper start
    @Override
    public void broadcast(BaseComponent component) {
        for (Player player : getOnlinePlayers()) {
            player.sendMessage(component);
        }
    }

    @Override
    public void broadcast(BaseComponent... components) {
        for (Player player : getOnlinePlayers()) {
            player.sendMessage(components);
        }
    }
    // Paper end

    @Override
    @Deprecated
    public OfflinePlayer getOfflinePlayer(String name) {
        Validate.notNull(name, "Name cannot be null");
        com.google.common.base.Preconditions.checkArgument( !org.apache.commons.lang.StringUtils.isBlank( name ), "Name cannot be blank" ); // Spigot

        OfflinePlayer result = getPlayerExact(name);
        if (result == null) {
            // Spigot Start
            GameProfile profile = null;
            // Only fetch an online UUID in online mode
            if ( this.pulseSpigot.isProxyOnlineMode() ) // PulseSpigot - add proxy online mode setting
            {
                profile = MinecraftServer.getServer().getUserCache().getProfile( name );
            }
            // Spigot end
            if (profile == null) {
                // Make an OfflinePlayer using an offline mode UUID since the name has no profile
                result = getOfflinePlayer(new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8)), name));
            } else {
                // Use the GameProfile even when we get a UUID so we ensure we still have a name
                result = getOfflinePlayer(profile);
            }
        } else {
            offlinePlayers.invalidate(result.getUniqueId()); // PulseSpigot - use caffeine
        }

        return result;
    }

    @Override
    public OfflinePlayer getOfflinePlayer(UUID id) {
        Validate.notNull(id, "UUID cannot be null");

        OfflinePlayer result = getPlayer(id);
        if (result == null) {
            result = offlinePlayers.getIfPresent(id); // PulseSpigot - use caffeine
            if (result == null) {
                result = new CraftOfflinePlayer(this, new GameProfile(id, null));
                offlinePlayers.put(id, result);
            }
        } else {
            offlinePlayers.invalidate(id); // PulseSpigot - use caffeine
        }

        return result;
    }

    public OfflinePlayer getOfflinePlayer(GameProfile profile) {
        OfflinePlayer player = new CraftOfflinePlayer(this, profile);
        offlinePlayers.put(profile.getId(), player);
        return player;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getIPBans() {
        return new HashSet<String>(Arrays.asList(playerList.getIPBans().getEntries()));
    }

    @Override
    public void banIP(String address) {
        Validate.notNull(address, "Address cannot be null.");

        this.getBanList(org.bukkit.BanList.Type.IP).addBan(address, null, null, null);
    }

    @Override
    public void unbanIP(String address) {
        Validate.notNull(address, "Address cannot be null.");

        this.getBanList(org.bukkit.BanList.Type.IP).pardon(address);
    }

    @Override
    public Set<OfflinePlayer> getBannedPlayers() {
        Set<OfflinePlayer> result = new HashSet<OfflinePlayer>();

        for (JsonListEntry entry : playerList.getProfileBans().getValues()) {
            result.add(getOfflinePlayer((GameProfile) entry.getKey()));
        }        

        return result;
    }

    @Override
    public BanList getBanList(BanList.Type type) {
        Validate.notNull(type, "Type cannot be null");

        switch(type){
        case IP:
            return new CraftIpBanList(playerList.getIPBans());
        case NAME:
        default:
            return new CraftProfileBanList(playerList.getProfileBans());
        }
    }

    @Override
    public void setWhitelist(boolean value) {
        playerList.setHasWhitelist(value);
        console.getPropertyManager().setProperty("white-list", value);
    }

    @Override
    public Set<OfflinePlayer> getWhitelistedPlayers() {
        Set<OfflinePlayer> result = new LinkedHashSet<OfflinePlayer>();

        for (JsonListEntry entry : playerList.getWhitelist().getValues()) {
            result.add(getOfflinePlayer((GameProfile) entry.getKey()));
        }

        return result;
    }

    @Override
    public Set<OfflinePlayer> getOperators() {
        Set<OfflinePlayer> result = new HashSet<OfflinePlayer>();

        for (JsonListEntry entry : playerList.getOPs().getValues()) {
            result.add(getOfflinePlayer((GameProfile) entry.getKey()));
        }

        return result;
    }

    @Override
    public void reloadWhitelist() {
        playerList.reloadWhitelist();
    }

    @Override
    public GameMode getDefaultGameMode() {
        return GameMode.getByValue(console.worlds.get(0).getWorldData().getGameType().getId());
    }

    @Override
    public void setDefaultGameMode(GameMode mode) {
        Validate.notNull(mode, "Mode cannot be null");

        for (World world : getWorlds()) {
            ((CraftWorld) world).getHandle().worldData.setGameType(WorldSettings.EnumGamemode.getById(mode.getValue()));
        }
    }

    @Override
    public ConsoleCommandSender getConsoleSender() {
        return console.console;
    }

    public EntityMetadataStore getEntityMetadata() {
        return entityMetadata;
    }

    public PlayerMetadataStore getPlayerMetadata() {
        return playerMetadata;
    }

    public WorldMetadataStore getWorldMetadata() {
        return worldMetadata;
    }

    @Override
    public File getWorldContainer() {
        if (this.getServer().universe != null) {
            return this.getServer().universe;
        }

        if (container == null) {
            container = new File(this.bukkitConfig.settings.worldContainer); // PulseSpigot
        }

        return container;
    }

    @Override
    public OfflinePlayer[] getOfflinePlayers() {
        WorldNBTStorage storage = (WorldNBTStorage) console.worlds.get(0).getDataManager();
        String[] files = storage.getPlayerDir().list(new DatFileFilter());
        Set<OfflinePlayer> players = new HashSet<OfflinePlayer>();

        for (String file : files) {
            try {
                players.add(getOfflinePlayer(FastUUID.parseUUID(file.substring(0, file.length() - 4))));
            } catch (IllegalArgumentException ex) {
                // Who knows what is in this directory, just ignore invalid files
            }
        }

        players.addAll(getOnlinePlayers());

        return players.toArray(new OfflinePlayer[players.size()]);
    }

    @Override
    public Messenger getMessenger() {
        return messenger;
    }

    @Override
    public void sendPluginMessage(Plugin source, String channel, byte[] message) {
        StandardMessenger.validatePluginMessage(getMessenger(), source, channel, message);

        for (Player player : getOnlinePlayers()) {
            player.sendPluginMessage(source, channel, message);
        }
    }

    @Override
    public Set<String> getListeningPluginChannels() {
        Set<String> result = new HashSet<String>();

        for (Player player : getOnlinePlayers()) {
            result.addAll(player.getListeningPluginChannels());
        }

        return result;
    }

    @Override
    public Inventory createInventory(InventoryHolder owner, InventoryType type) {
        // TODO: Create the appropriate type, rather than Custom?
        return new CraftInventoryCustom(owner, type);
    }

    @Override
    public Inventory createInventory(InventoryHolder owner, InventoryType type, String title) {
        return new CraftInventoryCustom(owner, type, title);
    }

    @Override
    public Inventory createInventory(InventoryHolder owner, int size) throws IllegalArgumentException {
        Validate.isTrue(size % 9 == 0, "Chests must have a size that is a multiple of 9!");
        return new CraftInventoryCustom(owner, size);
    }

    @Override
    public Inventory createInventory(InventoryHolder owner, int size, String title) throws IllegalArgumentException {
        Validate.isTrue(size % 9 == 0, "Chests must have a size that is a multiple of 9!");
        return new CraftInventoryCustom(owner, size, title);
    }

    @Override
    public HelpMap getHelpMap() {
        return helpMap;
    }

    @Override // Paper - add override
    public SimpleCommandMap getCommandMap() {
        return commandMap;
    }

    @Override
    public int getMonsterSpawnLimit() {
        return monsterSpawn;
    }

    @Override
    public int getAnimalSpawnLimit() {
        return animalSpawn;
    }

    @Override
    public int getWaterAnimalSpawnLimit() {
        return waterAnimalSpawn;
    }

    @Override
    public int getAmbientSpawnLimit() {
        return ambientSpawn;
    }

    @Override
    public boolean isPrimaryThread() {
        return Thread.currentThread().equals(console.primaryThread);
    }

    @Override
    public String getMotd() {
        return console.getMotd();
    }

    @Override
    public WarningState getWarningState() {
        return warningState;
    }

    public List<String> tabComplete(net.minecraft.server.ICommandListener sender, String message) {
        return tabComplete(sender, message, null); // PaperSpigot - location tab-completes. Original code here moved below
    }

    // PaperSpigot start - add BlockPosition support
    /*
        this code is copied, except for the noted change, from the original tabComplete(net.minecraft.server.ICommandListener sender, String message) method
     */
    public List<String> tabComplete(net.minecraft.server.ICommandListener sender, String message, BlockPosition blockPosition) {
        if (!(sender instanceof EntityPlayer)) {
            return ImmutableList.of();
        }

        // PulseSpigot start - Backport modern tab completion API
        Player player = ((EntityPlayer) sender).getBukkitEntity();

        boolean isCommand = message.startsWith("/");
        List<String> offers = isCommand
                ? this.tabCompleteCommand(player, message, blockPosition)
                : this.tabCompleteChat(player, message);
        Location location = MCUtil.toLocation(((CraftWorld) player.getWorld()).getHandle(), blockPosition);

        TabCompleteEvent tabEvent = new TabCompleteEvent(player, message, offers, isCommand, location);
        return tabEvent.callEvent() ? tabEvent.getCompletions() : ImmutableList.of();
        // PulseSpigot end
    }
    // PaperSpigot end

    public List<String> tabCompleteCommand(Player player, String message) {
        return tabCompleteCommand(player, message, null); // PaperSpigot - location tab-completes. Original code here moved below
    }

    // PaperSpigot start - add BlockPosition support
    /*
        this code is copied, except for the noted change, from the original tabCompleteCommand(Player player, String message) method
     */
    public List<String> tabCompleteCommand(Player player, String message, BlockPosition blockPosition) {
        // Spigot Start
        if ( (this.spigotConfig.commands.tabComplete < 0 || message.length() <= this.spigotConfig.commands.tabComplete) && !message.contains( " " ) ) // PulseSpigot
        {
            return ImmutableList.of();
        }
        // Spigot End

        List<String> completions = null;
        try {
            // send location info if present
            // completions = getCommandMap().tabComplete(player, message.substring(1));
            if (blockPosition == null || !((CraftWorld) player.getWorld()).getHandle().paperConfigPulseSpigot.allowBlockLocationTabCompletion) { // PulseSpigot
                completions = getCommandMap().tabComplete(player, message.substring(1));
            } else {
                completions = getCommandMap().tabComplete(player, message.substring(1), new Location(player.getWorld(), blockPosition.getX(), blockPosition.getY(), blockPosition.getZ()));
            }
        } catch (CommandException ex) {
            player.sendMessage(ChatColor.RED + "An internal error occurred while attempting to tab-complete this command");
            getLogger().log(Level.SEVERE, "Exception when " + player.getName() + " attempted to tab complete " + message, ex);
        }

        return completions == null ? ImmutableList.<String>of() : completions;
    }
    // PaperSpigot end

    public List<String> tabCompleteChat(Player player, String message) {
        List<String> completions = new ArrayList<String>();
        PlayerChatTabCompleteEvent event = new PlayerChatTabCompleteEvent(player, message, completions);
        String token = event.getLastToken();
        for (Player p : getOnlinePlayers()) {
            if (player.canSee(p) && StringUtil.startsWithIgnoreCase(p.getName(), token)) {
                completions.add(p.getName());
            }
        }
        pluginManager.callEvent(event);

        Iterator<?> it = completions.iterator();
        while (it.hasNext()) {
            Object current = it.next();
            if (!(current instanceof String)) {
                // Sanity
                it.remove();
            }
        }
        Collections.sort(completions, String.CASE_INSENSITIVE_ORDER);
        return completions;
    }

    @Override
    public CraftItemFactory getItemFactory() {
        return CraftItemFactory.instance();
    }

    @Override
    public CraftScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public void checkSaveState() {
        if (this.playerCommandState || this.printSaveWarning || this.console.autosavePeriod <= 0) {
            return;
        }
        this.printSaveWarning = true;
        getLogger().log(Level.WARNING, "A manual (plugin-induced) save has been detected while server is configured to auto-save. This may affect performance.", warningState == WarningState.ON ? new Throwable() : null);
    }

    @Override
    public CraftIconCache getServerIcon() {
        return icon;
    }

    @Override
    public CraftIconCache loadServerIcon(File file) throws Exception {
        Validate.notNull(file, "File cannot be null");
        if (!file.isFile()) {
            throw new IllegalArgumentException(file + " is not a file");
        }
        return loadServerIcon0(file);
    }

    static CraftIconCache loadServerIcon0(File file) throws Exception {
        return loadServerIcon0(ImageIO.read(file));
    }

    @Override
    public CraftIconCache loadServerIcon(BufferedImage image) throws Exception {
        Validate.notNull(image, "Image cannot be null");
        return loadServerIcon0(image);
    }

    static CraftIconCache loadServerIcon0(BufferedImage image) throws Exception {
        ByteBuf bytebuf = Unpooled.buffer();

        Validate.isTrue(image.getWidth() == 64, "Must be 64 pixels wide");
        Validate.isTrue(image.getHeight() == 64, "Must be 64 pixels high");
        ImageIO.write(image, "PNG", new ByteBufOutputStream(bytebuf));
        ByteBuf bytebuf1 = Base64.encode(bytebuf);

        return new CraftIconCache("data:image/png;base64," + bytebuf1.toString(Charsets.UTF_8));
    }

    @Override
    public void setIdleTimeout(int threshold) {
        console.setIdleTimeout(threshold);
    }

    @Override
    public int getIdleTimeout() {
        return console.getIdleTimeout();
    }

    @Override
    public ChunkGenerator.ChunkData createChunkData(World world) {
        return new CraftChunkData(world);
    }

    @Deprecated
    @Override
    public UnsafeValues getUnsafe() {
        return CraftMagicNumbers.INSTANCE;
    }

    // PandaSpigot start - PlayerProfile API
    @Override
    public com.destroystokyo.paper.profile.PlayerProfile createProfile(UUID uuid) {
        return createProfile(uuid, null);
    }

    @Override
    public com.destroystokyo.paper.profile.PlayerProfile createProfile(String name) {
        return createProfile(null, name);
    }

    @Override
    public com.destroystokyo.paper.profile.PlayerProfile createProfile(UUID uuid, String name) {
        Player player = uuid != null ? Bukkit.getPlayer(uuid) : (name != null ? Bukkit.getPlayerExact(name) : null);
        if (player != null) {
            return new com.destroystokyo.paper.profile.CraftPlayerProfile((CraftPlayer)player);
        }
        return new com.destroystokyo.paper.profile.CraftPlayerProfile(uuid, name);
    }
    // PandaSpigot end

    private final Spigot spigot = new Spigot()
    {

        // PaperSpigot start - Add getTPS (Further improve tick loop)
        @Override
        public double[] getTPS() {
            return new double[] {
                    MinecraftServer.getServer().tps1.getAverage(),
                    MinecraftServer.getServer().tps5.getAverage(),
                    MinecraftServer.getServer().tps15.getAverage()
            };
        }
        // PaperSpigot end

        @Deprecated
        @Override
        public YamlConfiguration getConfig()
        {
            return getBukkitConfig();
        }

        @Override
        public YamlConfiguration getBukkitConfig()
        {
            return configuration;
        }

        @Override
        public YamlConfiguration getSpigotConfig()
        {
            return pulseSpigot.getSpigotConfiguration(); // PulseSpigot
        }

        @Override
        public YamlConfiguration getPaperSpigotConfig()
        {
            return pulseSpigot.getPaperConfiguration(); // PulseSpigot
        }

        // TacoSpigot start
        @Override
        public YamlConfiguration getTacoSpigotConfig()
        {
            return pulseSpigot.getTacoConfiguration();
        }
        // TacoSpigot end

        // PulseSpigot start
        @Override
        public YamlConfiguration getPulseSpigotConfig() {
            return pulseSpigot.getPulseSpigotConfiguration();
        }
        // PulseSpigot end

        @Override
        public void restart() {
            org.spigotmc.RestartCommand.restart();
        }

        @Override
        public void broadcast(BaseComponent component) {
            for (Player player : getOnlinePlayers()) {
                player.spigot().sendMessage(component);
            }
        }

        @Override
        public void broadcast(BaseComponent... components) {
            for (Player player : getOnlinePlayers()) {
                player.spigot().sendMessage(components);
            }
        }
    };

    public Spigot spigot()
    {
        return spigot;
    }

    private final Pulse pulse = new Pulse() {

        @Override
        public Configuration getConfiguration() {
            return pulseConfig;
        }

    };

    public Pulse pulse() {
        return pulse;
    }
}

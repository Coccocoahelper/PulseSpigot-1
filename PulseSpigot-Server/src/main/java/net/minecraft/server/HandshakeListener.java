package net.minecraft.server;

import xyz.krypton.spigot.config.Config;
// CraftBukkit start
import java.net.InetAddress;
// CraftBukkit end

public class HandshakeListener implements PacketHandshakingInListener {

    private static final com.google.gson.Gson gson = new com.google.gson.Gson(); // Spigot
    // CraftBukkit start - add fields
    private static final java.util.Map<InetAddress, Long> throttleTracker = new it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap<>(); // Nacho - Use fastutil map
    private static int throttleCounter = 0;
    // CraftBukkit end

    private final MinecraftServer a;
    private final NetworkManager b;

    public HandshakeListener(MinecraftServer minecraftserver, NetworkManager networkmanager) {
        this.a = minecraftserver;
        this.b = networkmanager;
    }

    public void a(PacketHandshakingInSetProtocol packethandshakinginsetprotocol) {
        switch (HandshakeListener.SyntheticClass_1.a[packethandshakinginsetprotocol.a().ordinal()]) {
        case 1:
            this.b.a(EnumProtocol.LOGIN);
            ChatComponentText chatcomponenttext;

            // CraftBukkit start - Connection throttle
            try {
                long currentTime = System.currentTimeMillis();
                long connectionThrottle = MinecraftServer.getServer().server.getConnectionThrottle();
                InetAddress address = ((java.net.InetSocketAddress) this.b.getSocketAddress()).getAddress();

                synchronized (throttleTracker) {
                    if (throttleTracker.containsKey(address) && !"127.0.0.1".equals(address.getHostAddress()) && currentTime - throttleTracker.get(address) < connectionThrottle) {
                        throttleTracker.put(address, currentTime);
                        chatcomponenttext = new ChatComponentText("Connection throttled! Please wait before reconnecting.");
                        this.b.handle(new PacketLoginOutDisconnect(chatcomponenttext));
                        this.b.close(chatcomponenttext);
                        return;
                    }

                    throttleTracker.put(address, currentTime);
                    throttleCounter++;
                    if (throttleCounter > 200) {
                        throttleCounter = 0;

                        // Cleanup stale entries
                        java.util.Iterator iter = throttleTracker.entrySet().iterator();
                        while (iter.hasNext()) {
                            java.util.Map.Entry<InetAddress, Long> entry = (java.util.Map.Entry) iter.next();
                            if (entry.getValue() > connectionThrottle) {
                                iter.remove();
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                org.apache.logging.log4j.LogManager.getLogger().debug("Failed to check connection throttle", t);
            }
            // CraftBukkit end

            if (packethandshakinginsetprotocol.b() > 47) {
                chatcomponenttext = new ChatComponentText( java.text.MessageFormat.format( Config.get().messages.outdatedServer.replaceAll("'", "''"), "1.8.8" ) ); // Spigot
                this.b.handle(new PacketLoginOutDisconnect(chatcomponenttext));
                this.b.close(chatcomponenttext);
            } else if (packethandshakinginsetprotocol.b() < 47) {
                chatcomponenttext = new ChatComponentText( java.text.MessageFormat.format( Config.get().messages.outdatedClient.replaceAll("'", "''"), "1.8.8" ) ); // Spigot
                this.b.handle(new PacketLoginOutDisconnect(chatcomponenttext));
                this.b.close(chatcomponenttext);
            } else {
                this.b.a((PacketListener) (new LoginListener(this.a, this.b)));
                // PandaSpigot start - handshake event
                boolean proxyLogicEnabled = Config.get().settings.bungeecord;
                boolean handledByEvent = false;
                // Try and handle the handshake through the event
                if (com.destroystokyo.paper.event.player.PlayerHandshakeEvent.getHandlerList().getRegisteredListeners().length != 0) { // Hello? Can you hear me?
                    java.net.SocketAddress socketAddress = this.b.l;
                    String hostnameOfRemote = socketAddress instanceof java.net.InetSocketAddress ? ((java.net.InetSocketAddress) socketAddress).getHostString() : InetAddress.getLoopbackAddress().getHostAddress();
                    com.destroystokyo.paper.event.player.PlayerHandshakeEvent event = new com.destroystokyo.paper.event.player.PlayerHandshakeEvent(packethandshakinginsetprotocol.hostname, hostnameOfRemote, !proxyLogicEnabled);
                    org.bukkit.Bukkit.getPluginManager().callEvent(event);
                    if (!event.isCancelled()) {
                        // If we've failed somehow, let the client know so and go no further.
                        if (event.isFailed()) {
                            chatcomponenttext = new ChatComponentText(event.getFailMessage());
                            this.b.handle(new PacketLoginOutDisconnect(chatcomponenttext));
                            this.b.close(chatcomponenttext);
                            return;
                        }

                        packethandshakinginsetprotocol.hostname = event.getServerHostname();
                        if (event.getSocketAddressHostname() != null) this.b.l = new java.net.InetSocketAddress(event.getSocketAddressHostname(), socketAddress instanceof java.net.InetSocketAddress ? ((java.net.InetSocketAddress) socketAddress).getPort() : 0);
                        this.b.spoofedUUID = event.getUniqueId();
                        this.b.spoofedProfile = gson.fromJson(event.getPropertiesJson(), com.mojang.authlib.properties.Property[].class);
                        handledByEvent = true; // Hooray, we did it!
                    }
                }
                // Don't try and handle default logic if it's been handled by the event.
                if (!handledByEvent && proxyLogicEnabled) {
                // PandaSpigot end
                // Spigot Start
                //if (SpigotConfig.get().settings.bungeecord) { // PandaSpigot - comment out, we check above!
                    String[] split = packethandshakinginsetprotocol.hostname.split("\00");
                    if ( split.length == 3 || split.length == 4 ) {
                        packethandshakinginsetprotocol.hostname = split[0];
                        b.l = new java.net.InetSocketAddress(split[1], ((java.net.InetSocketAddress) b.getSocketAddress()).getPort());
                        b.spoofedUUID = com.mojang.util.UUIDTypeAdapter.fromString( split[2] );
                    } else
                    {
                        chatcomponenttext = new ChatComponentText("If you wish to use IP forwarding, please enable it in your BungeeCord config as well!");
                        this.b.handle(new PacketLoginOutDisconnect(chatcomponenttext));
                        this.b.close(chatcomponenttext);
                        return;
                    }
                    if ( split.length == 4 )
                    {
                        b.spoofedProfile = gson.fromJson(split[3], com.mojang.authlib.properties.Property[].class);
                    }
                }
                // Spigot End
                ((LoginListener) this.b.getPacketListener()).hostname = packethandshakinginsetprotocol.hostname + ":" + packethandshakinginsetprotocol.port; // CraftBukkit - set hostname
            }
            break;

        case 2:
            this.b.a(EnumProtocol.STATUS);
            this.b.a((PacketListener) (new PacketStatusListener(this.a, this.b)));
            break;

        default:
            throw new UnsupportedOperationException("Invalid intention " + packethandshakinginsetprotocol.a());
        }

        // Paper start - NetworkClient implementation
        this.b.protocolVersion = packethandshakinginsetprotocol.b();
        this.b.virtualHost = com.destroystokyo.paper.network.PaperNetworkClient.prepareVirtualHost(packethandshakinginsetprotocol.hostname, packethandshakinginsetprotocol.port);
        // Paper end
    }

    public void a(IChatBaseComponent ichatbasecomponent) {}

    static class SyntheticClass_1 {

        static final int[] a = new int[EnumProtocol.values().length];

        static {
            try {
                HandshakeListener.SyntheticClass_1.a[EnumProtocol.LOGIN.ordinal()] = 1;
            } catch (NoSuchFieldError nosuchfielderror) {
                ;
            }

            try {
                HandshakeListener.SyntheticClass_1.a[EnumProtocol.STATUS.ordinal()] = 2;
            } catch (NoSuchFieldError nosuchfielderror1) {
                ;
            }

        }
    }
}

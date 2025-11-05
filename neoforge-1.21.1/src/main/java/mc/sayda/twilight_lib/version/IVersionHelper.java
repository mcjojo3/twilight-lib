package mc.sayda.twilight_lib.version;

/**
 * Version abstraction interface for Minecraft version-specific operations.
 * Implementations exist in each forge-X.Y.Z module to handle API differences.
 */
public interface IVersionHelper {

    /**
     * Register mod capabilities (IEffects, ITrails, IAddons, IMorph)
     * Called during mod initialization
     */
    void registerCapabilities();

    /**
     * Register network packets for client-server synchronization
     * Called during mod initialization
     */
    void registerNetworkPackets();

    /**
     * Register particle providers for custom particles
     * Called during client setup
     */
    void registerParticleProviders();

    /**
     * Register entity renderers for custom entities
     * Called during client setup
     */
    void registerEntityRenderers();

    /**
     * Register event handlers (tick events, player events, etc.)
     * Called during mod initialization
     */
    void registerEventHandlers();

    /**
     * Send a packet to a specific player
     * @param player The player to send to (can be ServerPlayer or other player types)
     * @param packet The packet object to send
     */
    void sendPacketToPlayer(Object player, Object packet);

    /**
     * Get the current Minecraft version string
     * @return Version string (e.g., "1.20.1")
     */
    String getMinecraftVersion();
}

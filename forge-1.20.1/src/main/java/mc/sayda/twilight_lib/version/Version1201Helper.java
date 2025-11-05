package mc.sayda.twilight_lib.version;

/**
 * Version helper implementation for Minecraft 1.20.1 (Forge 47.3.0)
 * Handles version-specific API calls and registration.
 */
public class Version1201Helper implements IVersionHelper {

    @Override
    public void registerCapabilities() {
        // Capabilities are registered via RegisterCapabilitiesEvent in TwilightLib
        // This method can be used if we need version-specific capability registration
    }

    @Override
    public void registerNetworkPackets() {
        // Network packets are registered in NetworkHandler
        // This method can be used if packet registration API changes
    }

    @Override
    public void registerParticleProviders() {
        // Particle providers are registered in ClientModEvents
        // This method can be used if particle registration API changes
    }

    @Override
    public void registerEntityRenderers() {
        // Entity renderers are registered in ClientModEvents
        // This method can be used if renderer registration API changes
    }

    @Override
    public void registerEventHandlers() {
        // Event handlers are registered via @SubscribeEvent annotations
        // This method can be used if event registration API changes
    }

    @Override
    public void sendPacketToPlayer(Object player, Object packet) {
        // Network packet sending is handled in NetworkHandler
        // This method can be implemented if packet sending API changes
        throw new UnsupportedOperationException("Not yet implemented - use NetworkHandler directly for now");
    }

    @Override
    public String getMinecraftVersion() {
        return "1.20.1";
    }
}

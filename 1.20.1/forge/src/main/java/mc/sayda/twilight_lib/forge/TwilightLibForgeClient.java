package mc.sayda.twilight_lib.forge;

/**
 * Forge client-side event subscriber.
 * 
 * Note: TwilightLibClient.init() is called from the TwilightLibForge
 * constructor
 * (not here) because Architectury's model layer registration APIs must be
 * called
 * before Forge's EntityRenderersEvent.RegisterLayerDefinitions fires.
 * 
 * This class is kept as a placeholder for any future Forge-specific client
 * events
 * that require @SubscribeEvent handling on the MOD bus with Dist.CLIENT.
 */
public class TwilightLibForgeClient {
    // Client init is handled in TwilightLibForge constructor
}

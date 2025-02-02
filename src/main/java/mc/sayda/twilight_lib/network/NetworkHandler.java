package mc.sayda.twilight_lib.network;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("twilight_lib", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void registerMessages() {
        int id = 0;
        // Register our SyncPlayerModelPacket with id=0
        CHANNEL.registerMessage(id++,
                SyncPlayerModelPacket.class,
                SyncPlayerModelPacket::encode,
                SyncPlayerModelPacket::decode,
                SyncPlayerModelPacket::handle);
    }
}

package mc.sayda.twilight_lib.fabric;

import mc.sayda.twilight_lib.client.TwilightLibClient;
import net.fabricmc.api.ClientModInitializer;

public class TwilightLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TwilightLibClient.init();
    }
}

package io.github.betterclient.maxima.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.NetworkPhase;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class FakeNetworkHandler extends ServerPlayNetworkHandler {
    public FakeNetworkHandler(ServerPlayerEntity spe) {
        super(MinecraftClient.getInstance().getServer(), new FakeClientConnection(), spe, new ConnectedClientData(spe.getGameProfile(), 1, spe.getClientOptions(), false));
    }

    @Override
    public NetworkPhase getPhase() {
        return super.getPhase();
    }
}

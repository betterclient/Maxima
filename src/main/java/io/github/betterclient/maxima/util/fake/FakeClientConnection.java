package io.github.betterclient.maxima.util.fake;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;

public class FakeClientConnection extends ClientConnection {
    public FakeClientConnection() {
        super(NetworkSide.SERVERBOUND);
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush) {

    }
}

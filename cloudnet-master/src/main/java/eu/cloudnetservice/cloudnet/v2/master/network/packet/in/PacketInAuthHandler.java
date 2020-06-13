package eu.cloudnetservice.cloudnet.v2.master.network.packet.in;

import eu.cloudnetservice.cloudnet.v2.lib.network.auth.Auth;
import eu.cloudnetservice.cloudnet.v2.lib.network.auth.AuthLoginResult;
import eu.cloudnetservice.cloudnet.v2.lib.network.auth.AuthType;
import eu.cloudnetservice.cloudnet.v2.lib.network.auth.packetio.PacketOutAuthResult;
import eu.cloudnetservice.cloudnet.v2.lib.network.protocol.packet.Packet;
import eu.cloudnetservice.cloudnet.v2.lib.network.protocol.packet.PacketInHandler;
import eu.cloudnetservice.cloudnet.v2.lib.network.protocol.packet.PacketSender;
import eu.cloudnetservice.cloudnet.v2.lib.service.ServiceId;
import eu.cloudnetservice.cloudnet.v2.lib.utility.document.Document;
import eu.cloudnetservice.cloudnet.v2.master.CloudNet;
import eu.cloudnetservice.cloudnet.v2.master.network.CloudNetClient;
import eu.cloudnetservice.cloudnet.v2.master.network.CloudNetClientAuth;
import eu.cloudnetservice.cloudnet.v2.master.network.components.MinecraftServer;
import eu.cloudnetservice.cloudnet.v2.master.network.components.ProxyServer;
import eu.cloudnetservice.cloudnet.v2.master.network.components.Wrapper;
import io.netty.channel.Channel;

/**
 * Created by Tareko on 25.07.2017.
 */
public final class PacketInAuthHandler implements PacketInHandler {

    public void handleInput(Packet packet, PacketSender packetSender) {
        Auth auth = packet.getData().getObject("auth", Auth.TYPE);
        handleAuth(auth.getType(), auth.getAuthData(), packetSender);
    }

    public static void handleAuth(AuthType authType, Document authData, PacketSender packetSender) {
        if (!(packetSender instanceof CloudNetClientAuth)) {
            return;
        }
        CloudNetClientAuth client = (CloudNetClientAuth) packetSender;
        switch (authType) {
            case CLOUD_NET: {
                String key = authData.getString("key");
                String id = authData.getString("id");

                if (CloudNet.getInstance().getWrappers().containsKey(id)) {
                    Wrapper wrapper = CloudNet.getInstance().getWrappers().get(id);
                    String wrapperKey = CloudNet.getInstance().getConfig().getWrapperKey();
                    if (wrapperKey != null && wrapper.getChannel() == null && wrapperKey.equals(key)) {
                        Channel channel = client.getChannel();
                        channel.pipeline().remove("client");
                        client.getChannel().writeAndFlush(new PacketOutAuthResult(new AuthLoginResult(true))).syncUninterruptibly();
                        channel.pipeline().addLast(new CloudNetClient(wrapper, channel));
                        return;
                    } else {
                        client.getChannel().writeAndFlush(new PacketOutAuthResult(new AuthLoginResult(false))).syncUninterruptibly();
                        if (wrapperKey != null) {
                            CloudNet.getLogger().info(
                                "Authentication failed [Invalid WrapperKey or Wrapper is already connected!]");
                        } else {
                            CloudNet.getLogger().info(
                                "Authentication failed [WrapperKey not found, please copy a wrapper key to this instance]");
                        }
                    }
                } else {
                    client.getChannel().writeAndFlush(new PacketOutAuthResult(new AuthLoginResult(false))).syncUninterruptibly();
                    client.getChannel().close().syncUninterruptibly();
                }
            }
            return;
            case GAMESERVER_OR_BUNGEE: {
                ServiceId serviceId = authData.getObject("serviceId", ServiceId.class);
                if (CloudNet.getInstance().getWrappers().containsKey(serviceId.getWrapperId())) {
                    Wrapper wrapper = CloudNet.getInstance().getWrappers().get(serviceId.getWrapperId());
                    if (wrapper.getServers().containsKey(serviceId.getServerId())) {
                        MinecraftServer minecraftServer = wrapper.getServers().get(serviceId.getServerId());
                        if (minecraftServer.getChannel() == null && minecraftServer.getServerInfo().getServiceId().getUniqueId().equals(
                            serviceId.getUniqueId())) {
                            Channel channel = client.getChannel();
                            channel.pipeline().remove("client");
                            channel.pipeline().addLast(new CloudNetClient(minecraftServer, channel));
                        }
                    } else if (wrapper.getProxies().containsKey(serviceId.getServerId())) {
                        ProxyServer proxyServer = wrapper.getProxies().get(serviceId.getServerId());
                        if (proxyServer.getChannel() == null && proxyServer.getProxyInfo().getServiceId().getUniqueId().equals(
                            serviceId.getUniqueId())) {
                            Channel channel = client.getChannel();
                            channel.pipeline().remove("client");
                            channel.pipeline().addLast(new CloudNetClient(proxyServer, channel));
                        }
                    } else {
                        client.getChannel().close().syncUninterruptibly();
                    }
                } else {
                    client.getChannel().close().syncUninterruptibly();
                }
            }
        }
    }
}
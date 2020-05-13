package eu.cloudnetservice.v2.wrapper.network.packet.out;

import eu.cloudnetservice.v2.lib.network.protocol.packet.Packet;
import eu.cloudnetservice.v2.lib.network.protocol.packet.PacketRC;
import eu.cloudnetservice.v2.lib.service.ServiceId;
import eu.cloudnetservice.v2.lib.utility.document.Document;

/**
 * Created by Tareko on 25.08.2017.
 */
public class PacketOutDisableScreen extends Packet {

    public PacketOutDisableScreen(ServiceId serviceId) {
        super(PacketRC.CN_WRAPPER + 10, new Document("serviceId", serviceId));
    }
}

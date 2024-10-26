package com.gastonlagaf.stun.client;

import com.gastonlagaf.stun.model.Message;
import com.gastonlagaf.stun.model.NatBehaviour;
import com.gastonlagaf.turn.client.TurnClient;
import com.gastonlagaf.udp.client.UdpClient;

import java.io.Closeable;
import java.net.InetSocketAddress;

public interface StunClient extends UdpClient<Message>, Closeable {

    TurnClient initializeTurnSession();

    InetSocketAddress getReflexiveAddress();

    NatBehaviour checkMappingBehaviour();

    NatBehaviour checkFilteringBehaviour();

}

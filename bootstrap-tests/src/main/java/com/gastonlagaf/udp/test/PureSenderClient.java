package com.gastonlagaf.udp.test;

import com.gastonlagaf.udp.client.stun.StunClientProtocol;
import com.gastonlagaf.udp.client.stun.client.StunClient;
import com.gastonlagaf.udp.discovery.InternetDiscovery;
import com.gastonlagaf.udp.client.model.ClientProperties;
import com.gastonlagaf.udp.test.protocol.PureProtocol;
import com.gastonlagaf.udp.turn.model.NatBehaviour;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class PureSenderClient {

    public static void main(String[] args) throws Exception {
        InetAddress hostIp = InternetDiscovery.getAddress();
        ClientProperties clientProperties = new ClientProperties(
                new InetSocketAddress(hostIp, 40004),
                new InetSocketAddress(hostIp, 40001),
                new InetSocketAddress("45.129.186.80", 3478),
                new InetSocketAddress("45.129.186.80", 3478),
                5000L
        );

        InetSocketAddress reflexiveAddress;
        try (StunClientProtocol stunClientProtocol = new StunClientProtocol(clientProperties)) {
            stunClientProtocol.start(new InetSocketAddress(hostIp, 40004));
            reflexiveAddress = ((StunClient)stunClientProtocol.getClient()).getReflexiveAddress();
            System.out.println(reflexiveAddress);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PureProtocol pureProtocol = new PureProtocol(NatBehaviour.NO_NAT, clientProperties, true);
        pureProtocol.start(clientProperties.getHostAddress());

        InetSocketAddress socketAddress = new InetSocketAddress(reflexiveAddress.getHostName(), 40001);
        for (int i = 0; i < 100; i++) {
            String message = "Ping " + i;
            pureProtocol.getClient().send(socketAddress, message);
            Thread.sleep(1000L);
        }

        pureProtocol.close();
    }

}

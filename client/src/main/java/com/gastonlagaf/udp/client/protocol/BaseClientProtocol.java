package com.gastonlagaf.udp.client.protocol;

import com.gastonlagaf.udp.client.BaseUdpClient;
import com.gastonlagaf.udp.client.PendingMessages;
import com.gastonlagaf.udp.client.UdpClient;
import com.gastonlagaf.udp.discovery.InternetDiscovery;
import com.gastonlagaf.udp.client.model.ClientProperties;
import com.gastonlagaf.udp.client.stun.StunClientProtocol;
import com.gastonlagaf.udp.client.stun.client.StunClient;
import com.gastonlagaf.udp.client.turn.proxy.TurnProxy;
import com.gastonlagaf.udp.protocol.ClientProtocol;
import com.gastonlagaf.udp.socket.UdpSockets;
import com.gastonlagaf.udp.turn.model.NatBehaviour;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract class BaseClientProtocol<T> implements ClientProtocol<T> {

    private static final Set<NatBehaviour> TURN_REQUIRED_NAT_BEHAVIOURS = Set.of(
            NatBehaviour.ADDRESS_DEPENDENT, NatBehaviour.ADDRESS_AND_PORT_DEPENDENT
    );

    protected final ClientProperties clientProperties;

    protected final PendingMessages<T> pendingMessages;

    protected final NatBehaviour natBehaviour;

    protected final Integer threadCount;

    protected UdpSockets<T> sockets;

    protected UdpClient<T> client;

    public BaseClientProtocol(Integer threads) {
        this(null, null, threads);
    }

    public BaseClientProtocol(NatBehaviour natBehaviour, ClientProperties clientProperties, Integer threads) {
        this.clientProperties = Optional.ofNullable(clientProperties).orElseGet(this::getClientProperties);
        this.natBehaviour = Optional.ofNullable(natBehaviour).orElseGet(() -> getNatBehaviour(this.clientProperties));
        this.threadCount = threads;

        this.pendingMessages = new PendingMessages<>(this.clientProperties.getSocketTimeout());
    }

    protected abstract String getCorrelationId(T message);

    protected abstract UdpClient<T> createUdpClient(UdpClient<T> udpClient);

    @Override
    public CompletableFuture<T> awaitResult(T message) {
        String correlationId = getCorrelationId(message);
        return pendingMessages.put(correlationId);
    }

    @Override
    public UdpClient<T> getClient() {
        return client;
    }

    private ClientProperties getClientProperties() {
        InputStream propertiesStream = BaseClientProtocol.class.getClassLoader().getResourceAsStream("udp-client.properties");
        Properties properties = new Properties();
        try {
            properties.load(propertiesStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        InetAddress hostIp = InternetDiscovery.getAddress();
        return new ClientProperties(hostIp, properties);
    }

    private NatBehaviour getNatBehaviour(ClientProperties clientProperties) {
        try (StunClientProtocol stunClientProtocol = new StunClientProtocol(clientProperties)) {
            stunClientProtocol.start(clientProperties.getHostAddress());
            StunClient stunClient = (StunClient) stunClientProtocol.getClient();
            return stunClient.checkMappingBehaviour();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(InetSocketAddress... addresses) {
        UdpClient<T> client;
        if (!TURN_REQUIRED_NAT_BEHAVIOURS.contains(this.natBehaviour)) {
            this.sockets = new UdpSockets<>(threadCount);
            client = new BaseUdpClient<>(sockets, this, this.clientProperties.getHostAddress());
            sockets.start(this);
            for (InetSocketAddress address : addresses) {
                sockets.getRegistry().register(address);
            }
        } else {
            this.sockets = null;
            client = new TurnProxy<>(this.clientProperties, this);
        }
        this.client = createUdpClient(client);

        if (client instanceof TurnProxy<T> turnProxy) {
            turnProxy.start(clientProperties.getHostAddress());
        }
    }

    @Override
    public void close() throws IOException {
        client.close();

        Optional.ofNullable(sockets).ifPresent(it -> {
            try {
                it.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}

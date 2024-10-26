package com.gastonlagaf.stun.server.handler.impl;

import com.gastonlagaf.stun.exception.StunProtocolException;
import com.gastonlagaf.stun.integrity.IntegrityVerifier;
import com.gastonlagaf.stun.model.*;
import com.gastonlagaf.stun.server.handler.StunMessageHandler;
import com.gastonlagaf.stun.server.model.ContexedMessage;
import com.gastonlagaf.stun.server.model.StunResponse;
import com.gastonlagaf.stun.server.turn.TurnSession;
import com.gastonlagaf.stun.server.turn.TurnSessions;
import com.gastonlagaf.udp.ChannelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class AllocateMessageHandler implements StunMessageHandler {

    private final Map<String, TurnSessions> turnSessionsMap;

    private final IntegrityVerifier integrityVerifier;

    private final ChannelRegistry channelRegistry;

    @Override
    public MessageType getMessageType() {
        return MessageType.ALLOCATE;
    }

    @Override
    public StunResponse handle(InetSocketAddress serverAddress, InetSocketAddress clientAddress, ContexedMessage message) {
        if (null == turnSessionsMap) {
            throw new StunProtocolException("Turn is not activated", ErrorCode.TRY_ALTERNATE.getCode());
        }
        if (null != message.getTurnSession()) {
            throw new StunProtocolException("Cannot allocate inside turn session", ErrorCode.TRY_ALTERNATE.getCode());
        }

        Optional.ofNullable(integrityVerifier).ifPresent(it -> it.check(message));

        validateRequestedTransport(message);

        TurnSessions turnSessions = turnSessionsMap.get(serverAddress.getAddress().getHostAddress());

        Integer port = turnSessions.allocatePort();
        InetSocketAddress turnServerAddress = new InetSocketAddress(serverAddress.getHostName(), port);
        SelectionKey key = channelRegistry.register(turnServerAddress.getHostName(), turnServerAddress.getPort());
        TurnSession turnSession = new TurnSession(clientAddress, turnServerAddress, key);
        turnSessions.put(turnSession, Protocol.UDP);

        log.info("Initialized turn session on {}", turnSession.getServerAddress().toString());

        Map<Integer, MessageAttribute> attributes = prepareAttributes(clientAddress, turnServerAddress);
        MessageHeader messageHeader = new MessageHeader(MessageType.ALLOCATE.getCode(), 0, message.getHeader().getTransactionId());
        Message responseMessage = new Message(messageHeader, attributes);

        return new StunResponse(serverAddress, clientAddress, responseMessage);
    }

    private void validateRequestedTransport(Message message) {
        RequestedTransportAttribute requestedTransportAttribute = message.getAttributes().get(KnownAttributeName.REQUESTED_TRANSPORT);
        if (null == requestedTransportAttribute) {
            throw new StunProtocolException("Unknown requested transport", ErrorCode.UNSUPPORTED_TRANSPORT_PROTOCOL.getCode());
        } else if (!Protocol.UDP.equals(requestedTransportAttribute.getValue())) {
            throw new StunProtocolException("Only udp connection is supported", ErrorCode.UNSUPPORTED_TRANSPORT_PROTOCOL.getCode());
        }
    }

    private Map<Integer, MessageAttribute> prepareAttributes(InetSocketAddress clientAddress, InetSocketAddress turnServerAddress) {
        AddressAttribute relayedAddressAttribute = new AddressAttribute(KnownAttributeName.XOR_RELAYED_ADDRESS, turnServerAddress);
        AddressAttribute mappedAddress = new AddressAttribute(KnownAttributeName.XOR_MAPPED_ADDRESS, clientAddress);
        LifetimeAttribute lifetimeAttribute = new LifetimeAttribute((int) TimeUnit.MINUTES.toSeconds(TurnSessions.DEFAULT_SESSION_DURATION_MINUTES));

        return Map.of(
                relayedAddressAttribute.getType(), relayedAddressAttribute,
                mappedAddress.getType(), mappedAddress,
                lifetimeAttribute.getType(), lifetimeAttribute
        );
    }

}

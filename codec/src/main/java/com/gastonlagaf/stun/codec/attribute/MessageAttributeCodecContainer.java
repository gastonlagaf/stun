package com.gastonlagaf.stun.codec.attribute;

import com.gastonlagaf.stun.codec.attribute.impl.*;
import com.gastonlagaf.stun.model.KnownAttributeName;
import com.gastonlagaf.stun.model.PasswordAlgorithm;
import com.gastonlagaf.stun.user.model.UserDetails;

import java.util.Map;

public class MessageAttributeCodecContainer {

    private final MessageAttributeCodec defaultCodec;

    private final Map<Integer, MessageAttributeCodec> codecMap;

    public MessageAttributeCodecContainer() {
        this.defaultCodec = new DefaultAttributeCodec();
        this.codecMap = Map.ofEntries(
                Map.entry(KnownAttributeName.MAPPED_ADDRESS.getCode(), new MappedAddressAttributeCodec()),
                Map.entry(KnownAttributeName.XOR_MAPPED_ADDRESS.getCode(), new MappedAddressAttributeCodec()),
                Map.entry(KnownAttributeName.RESPONSE_ORIGIN.getCode(), new MappedAddressAttributeCodec()),
                Map.entry(KnownAttributeName.OTHER_ADDRESS.getCode(), new MappedAddressAttributeCodec()),
                Map.entry(KnownAttributeName.CHANGE_REQUEST.getCode(), new ChangeRequestAttributeCodec()),
                Map.entry(KnownAttributeName.ERROR_CODE.getCode(), new ErrorCodeAttributeCodec()),
                Map.entry(KnownAttributeName.CHANNEL_NUMBER.getCode(), new ChannelNumberAttributeCodec()),
                Map.entry(KnownAttributeName.LIFETIME.getCode(), new LifetimeAttributeCodec()),
                Map.entry(KnownAttributeName.XOR_PEER_ADDRESS.getCode(), new MappedAddressAttributeCodec()),
                Map.entry(KnownAttributeName.XOR_RELAYED_ADDRESS.getCode(), new MappedAddressAttributeCodec()),
                Map.entry(KnownAttributeName.REQUESTED_ADDRESS_FAMILY.getCode(), new AddressFamilyAttributeCodec()),
                Map.entry(KnownAttributeName.EVEN_PORT.getCode(), new EvenPortAttributeCodec()),
                Map.entry(KnownAttributeName.REQUESTED_TRANSPORT.getCode(), new RequestedTransportAttributeCodec()),
                Map.entry(KnownAttributeName.DONT_FRAGMENT.getCode(), new DontFragmentAttributeCodec()),
                Map.entry(KnownAttributeName.ADDITIONAL_ADDRESS_FAMILY.getCode(), new AddressFamilyAttributeCodec()),
                Map.entry(KnownAttributeName.ADDRESS_ERROR_CODE.getCode(), new AddressErrorCodeAttributeCodec()),
                Map.entry(KnownAttributeName.MESSAGE_INTEGRITY.getCode(), new MessageIntegrityAttributeCodec()),
                Map.entry(KnownAttributeName.MESSAGE_INTEGRITY_SHA256.getCode(), new MessageIntegrityAttributeCodec()),
                Map.entry(KnownAttributeName.PASSWORD_ALGORITHM.getCode(), new PasswordAlgorithmAttributeCodec())
        );
    }

    public MessageAttributeCodec get(Integer type) {
        return codecMap.getOrDefault(type, defaultCodec);
    }

}
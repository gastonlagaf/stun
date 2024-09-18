package com.gastonlagaf.stun.codec.attribute.impl;

import com.gastonlagaf.stun.codec.attribute.BaseMessageAttributeCodec;
import com.gastonlagaf.stun.codec.util.CodecUtils;
import com.gastonlagaf.stun.model.MessageHeader;
import com.gastonlagaf.stun.model.PasswordAlgorithm;
import com.gastonlagaf.stun.model.PasswordAlgorithmAttribute;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

public class PasswordAlgorithmAttributeCodec extends BaseMessageAttributeCodec<PasswordAlgorithmAttribute> {

    @Override
    protected Class<PasswordAlgorithmAttribute> getType() {
        return PasswordAlgorithmAttribute.class;
    }

    @Override
    protected PasswordAlgorithmAttribute decodeValue(MessageHeader messageHeader, ByteBuffer byteBuffer, Integer type, Integer length) {
        int passwordAlgorithmCode = CodecUtils.readShort(byteBuffer);
        PasswordAlgorithm passwordAlgorithm = PasswordAlgorithm.ofCode(passwordAlgorithmCode);

        int parametersLength = CodecUtils.readShort(byteBuffer);
        byteBuffer.position(byteBuffer.position() + parametersLength);

        return new PasswordAlgorithmAttribute(type, length, passwordAlgorithm);
    }

    @Override
    protected byte[] encodeValue(MessageHeader messageHeader, PasswordAlgorithmAttribute messageAttribute) {
        ByteBuffer buffer = ByteBuffer.allocate(PasswordAlgorithmAttribute.DEFAULT_VALUE_LENGTH);
        buffer.put(CodecUtils.shortToByteArray(messageAttribute.getValue().getCode().shortValue()));
        IntStream.range(0, Short.BYTES).forEach(it -> buffer.put(PADDING));
        return buffer.array();
    }

}

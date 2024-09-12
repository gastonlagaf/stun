package com.gastonlagaf.stun.integrity.impl;

import com.gastonlagaf.stun.exception.StunProtocolException;
import com.gastonlagaf.stun.integrity.IntegrityVerifier;
import com.gastonlagaf.stun.integrity.model.IntegrityVerificationDetails;
import com.gastonlagaf.stun.integrity.utils.IntegrityUtils;
import com.gastonlagaf.stun.model.*;
import com.gastonlagaf.stun.user.UserProvider;
import com.gastonlagaf.stun.user.model.UserDetails;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.Optional;

@RequiredArgsConstructor
public class DefaultIntegrityVerifier implements IntegrityVerifier {

    private final String realm;

    private final UserProvider userProvider;

    @Override
    @SneakyThrows
    public void check(Message message) {
        IntegrityVerificationDetails details = extractDetails(message);

        UserDetails userDetails = details.getUserDetails();
        byte[] key = IntegrityUtils.constructKey(
                details.getPasswordAlgorithm(), userDetails.getUsername(), realm, userDetails.getPassword()
        );

        byte[] reconstructedHash = IntegrityUtils.constructHash(
                details.getTargetAttribute().getPrecedingBytes(), key, details.getTargetAttribute().getIsSha256()
        );

        if (!Arrays.equals(details.getTargetAttribute().getValue(), reconstructedHash)) {
            throw new StunProtocolException("Message integrity mismatch", ErrorCode.UNAUTHENTICATED.getCode());
        }
    }

    private IntegrityVerificationDetails extractDetails(Message message) {
        MessageIntegrityAttribute targetAttribute = (MessageIntegrityAttribute) message.getAttributes().get(KnownAttributeName.MESSAGE_INTEGRITY_SHA256.getCode());
        if (null == targetAttribute) {
            throw new StunProtocolException("No auth details provided", ErrorCode.UNAUTHENTICATED.getCode());
        }

        DefaultMessageAttribute usernameAttribute = (DefaultMessageAttribute) message.getAttributes().get(KnownAttributeName.USERNAME.getCode());
        if (null == usernameAttribute) {
            throw new StunProtocolException("Username not provided", ErrorCode.UNAUTHENTICATED.getCode());
        }
        String username = new String(usernameAttribute.getValue());
        UserDetails userDetails = userProvider.find(realm, username)
                .orElseThrow(() -> new StunProtocolException("User not found", ErrorCode.UNAUTHENTICATED.getCode()));

        PasswordAlgorithmAttribute passwordAlgorithmAttribute = (PasswordAlgorithmAttribute) message.getAttributes().get(KnownAttributeName.PASSWORD_ALGORITHM.getCode());
        PasswordAlgorithm passwordAlgorithm = Optional.ofNullable(passwordAlgorithmAttribute)
                .map(PasswordAlgorithmAttribute::getValue)
                .orElse(PasswordAlgorithm.MD5);

        return new IntegrityVerificationDetails(targetAttribute, userDetails, passwordAlgorithm);
    }

}

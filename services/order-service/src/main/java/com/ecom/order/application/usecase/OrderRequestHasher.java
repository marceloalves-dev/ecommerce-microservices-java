package com.ecom.order.application.usecase;

import com.ecom.order.application.port.in.CreateOrderUseCase.CreateOrderCommand;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.UUID;

/** Hash canônico do conteúdo relevante para a idempotência. */
final class OrderRequestHasher {

    private OrderRequestHasher() {
    }

    static String hash(CreateOrderCommand command) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, command.customerId().toString());
            command.items().stream()
                    .sorted(Comparator.comparing(CreateOrderCommand.Line::sku)
                            .thenComparingInt(CreateOrderCommand.Line::quantity))
                    .forEach(line -> {
                        update(digest, line.sku().trim());
                        update(digest, Integer.toString(line.quantity()));
                    });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}

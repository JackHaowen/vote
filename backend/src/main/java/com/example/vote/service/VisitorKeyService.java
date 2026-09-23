package com.example.vote.service;

import com.example.vote.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class VisitorKeyService {
    private final byte[] secret;
    public VisitorKeyService(@Value("${app.visitor-hmac-secret}") String secret) { this.secret = secret.getBytes(StandardCharsets.UTF_8); }
    public String from(UUID visitorId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(visitorId.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "VISITOR_KEY_ERROR", "匿名标识处理失败");
        }
    }
}

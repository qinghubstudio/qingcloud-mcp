package com.qingcloud.mcp.xhs.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * REST controller for Chrome extension signature communication.
 * Extension pushes signatures, MCP service consumes them.
 */
@RestController
@RequestMapping("/api/signature")
@CrossOrigin(origins = "*")
public class SignatureController {

    private static final Logger logger = LoggerFactory.getLogger(SignatureController.class);

    // Store pending signature requests
    private final Map<String, SignatureRequest> pendingRequests = new ConcurrentHashMap<>();

    // Store received signatures from extension
    private final Map<String, Map<String, String>> receivedSignatures = new ConcurrentHashMap<>();

    /**
     * Request signature from extension.
     * Called by internal services.
     */
    public Map<String, String> requestSignature(String url, Object data, long timeoutMs) {
        String requestId = generateRequestId();

        SignatureRequest request = new SignatureRequest(url, data);
        pendingRequests.put(requestId, request);

        logger.debug("Requesting signature for URL: {}, requestId: {}", url, requestId);

        try {
            // Wait for extension to provide signature
            if (request.latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                Map<String, String> signature = receivedSignatures.remove(requestId);
                if (signature != null) {
                    logger.debug("Received signature for requestId: {}", requestId);
                    logger.info("Extension signature received for requestId {}: {}", requestId, signature);
                    return signature;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pendingRequests.remove(requestId);
        }

        logger.warn("Signature request timeout for requestId: {}", requestId);
        return Map.of();
    }

    /**
     * Get pending signature request.
     * Called by Chrome extension to get what needs to be signed.
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingRequest() {
        if (pendingRequests.isEmpty()) {
            // Optional: log heartbeat occasionally
            return ResponseEntity.ok(Map.of("requestId", ""));
        }

        for (Map.Entry<String, SignatureRequest> entry : pendingRequests.entrySet()) {
            SignatureRequest request = entry.getValue();
            if (!request.processing) {
                request.processing = true;
                logger.info("Sending sign request {} to extension for URL: {}", entry.getKey(), request.url);
                return ResponseEntity.ok(Map.of(
                        "requestId", entry.getKey(),
                        "url", request.url,
                        "data", request.data != null ? request.data : ""));
            }
        }
        return ResponseEntity.ok(Map.of("requestId", ""));
    }

    /**
     * Receive signature from Chrome extension.
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, String>> submitSignature(
            @RequestBody Map<String, Object> body) {

        String requestId = (String) body.get("requestId");
        @SuppressWarnings("unchecked")
        Map<String, Object> rawSignature = (Map<String, Object>) body.get("signature");

        logger.debug("Received signature submission for requestId: {}", requestId);

        SignatureRequest request = pendingRequests.get(requestId);
        if (request != null) {
            // Convert all values to String (x-t might be Long)
            Map<String, String> signature = new java.util.HashMap<>();
            if (rawSignature != null) {
                for (Map.Entry<String, Object> entry : rawSignature.entrySet()) {
                    signature.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            receivedSignatures.put(requestId, signature);
            logger.info("Received signature from extension for {}: {}", requestId, signature.keySet());
            request.latch.countDown();
            return ResponseEntity.ok(Map.of("status", "ok"));
        }

        return ResponseEntity.ok(Map.of("status", "expired"));
    }

    /**
     * Health check for extension.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        logger.debug("Health check from extension, pending requests: {}", pendingRequests.size());
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "pendingRequests", pendingRequests.size()));
    }

    private String generateRequestId() {
        return System.currentTimeMillis() + "-" + Math.random();
    }

    private static class SignatureRequest {
        final String url;
        final Object data;
        final CountDownLatch latch = new CountDownLatch(1);
        volatile boolean processing = false;

        SignatureRequest(String url, Object data) {
            this.url = url;
            this.data = data;
        }
    }
}

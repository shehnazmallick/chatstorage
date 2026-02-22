package com.example.chatstorage.security;

import com.example.chatstorage.config.AppProperties;
import com.example.chatstorage.exception.RateLimitExceededException;
import com.example.chatstorage.exception.RateLimitServiceUnavailableException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisRateLimiterService {

    private static final String KEY_PREFIX = "rate_limit:";

    private final ClientResources clientResources;
    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, byte[]> redisConnection;
    private final ProxyManager<String> proxyManager;
    private final AppProperties appProperties;

    public RedisRateLimiterService(
            AppProperties appProperties,
            @Value("${spring.data.redis.host:localhost}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort,
            @Value("${spring.data.redis.password:}") String redisPassword,
            @Value("${spring.data.redis.ssl.enabled:false}") boolean redisSslEnabled,
            @Value("${spring.data.redis.timeout:2s}") Duration redisTimeout
    ) {
        this.appProperties = appProperties;
        this.clientResources = DefaultClientResources.create();
        this.redisClient = RedisClient.create(clientResources, buildRedisUri(redisHost, redisPort, redisPassword, redisSslEnabled, redisTimeout));
        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
        this.redisConnection = redisClient.connect(codec);
        this.proxyManager = LettuceBasedProxyManager.builderFor(redisConnection).build();
    }

    public void acquirePermission(String clientFingerprint) {
        BucketConfiguration config = buildBucketConfiguration();
        try {
            String key = KEY_PREFIX + clientFingerprint;
            Bucket bucket = proxyManager.builder().build(key, () -> config);
            if (!bucket.tryConsume(1)) {
                throw new RateLimitExceededException("Rate limit exceeded. Try again later.");
            }
        } catch (RuntimeException exception) {
            if (appProperties.getRateLimit().isFailOpenWhenRedisDown()) {
                return;
            }
            throw new RateLimitServiceUnavailableException("Rate limiting backend is unavailable", exception);
        }
    }

    private BucketConfiguration buildBucketConfiguration() {
        long capacity = Math.max(1, appProperties.getRateLimit().getRequestsPerMinute());
        long windowSeconds = Math.max(1, appProperties.getRateLimit().getWindowSeconds());
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, Duration.ofSeconds(windowSeconds)));
        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }

    private RedisURI buildRedisUri(String host, int port, String password, boolean sslEnabled, Duration timeout) {
        RedisURI.Builder builder = RedisURI.Builder.redis(host, port)
                .withSsl(sslEnabled)
                .withTimeout(timeout);

        if (password != null && !password.isBlank()) {
            builder.withPassword(password.toCharArray());
        }
        return builder.build();
    }

    @PreDestroy
    public void shutdown() {
        redisConnection.close();
        redisClient.shutdown();
        clientResources.shutdown();
    }
}

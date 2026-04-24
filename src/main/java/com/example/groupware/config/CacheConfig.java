package com.example.groupware.config;

import com.example.groupware.announcement.dto.AnnouncementDetailResponse;
import com.example.groupware.announcement.dto.AnnouncementListResponse;
import com.example.groupware.common.response.RestPage;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * 캐시별로 타입을 명시한 전용 Serializer를 사용.
 * GenericJackson2JsonRedisSerializer + activateDefaultTyping 조합은
 * Java record(final 클래스)에 @class 타입 정보를 붙이지 않아 역직렬화 실패 유발.
 * Jackson2JsonRedisSerializer<T>는 타입을 미리 알고 있어서 @class 없이도 정상 동작.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        // announcement:detail — AnnouncementDetailResponse 전용
        Jackson2JsonRedisSerializer<AnnouncementDetailResponse> detailSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, AnnouncementDetailResponse.class);

        // announcement:list — RestPage<AnnouncementListResponse> 전용
        JavaType listPageType = objectMapper.getTypeFactory()
                .constructParametricType(RestPage.class, AnnouncementListResponse.class);
        Jackson2JsonRedisSerializer<?> listSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, listPageType);

        RedisCacheConfiguration detailConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(detailSerializer))
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues();

        RedisCacheConfiguration listConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(listSerializer))
                .entryTtl(Duration.ofSeconds(30))
                .disableCachingNullValues();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "announcement:list",   listConfig,
                "announcement:detail", detailConfig
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}

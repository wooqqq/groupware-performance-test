package com.example.groupware.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * [v1 - 레거시 방식]
 *
 * @EnableCaching은 선언되어 있으나 서비스 레이어에 @Cacheable이 없으므로
 * 실제 캐싱은 동작하지 않는다.
 * v2/optimized 브랜치에서 Redis Cache-Aside 패턴이 적용된다.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}

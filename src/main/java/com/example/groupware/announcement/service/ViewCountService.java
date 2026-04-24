package com.example.groupware.announcement.service;

import com.example.groupware.announcement.repository.AnnouncementRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * [v2 - 개선 방식]
 *
 * 조회수 처리 방식 비교
 *
 * v1 (레거시): 요청마다 DB UPDATE
 *   - 1000명 동시 접속 → 동일 row에 UPDATE 1000번 → row lock 경쟁 → 응답 지연
 *
 * v2 (개선): Redis INCR + 배치 DB 동기화
 *   - 요청: Redis INCR (메모리 연산, ~0.1ms) → lock 없음
 *   - 30초마다: 누적된 count를 DB에 일괄 반영 → UPDATE 쿼리 수 최소화
 */
@Slf4j
@Service
public class ViewCountService {

    private static final String KEY_PREFIX = "announcement:viewcount:";

    private final StringRedisTemplate redisTemplate;
    private final AnnouncementRepository announcementRepository;
    private final Counter syncCounter;

    public ViewCountService(StringRedisTemplate redisTemplate,
                            AnnouncementRepository announcementRepository,
                            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.announcementRepository = announcementRepository;
        // Grafana에서 조회수 동기화 횟수를 추적하기 위한 커스텀 메트릭
        this.syncCounter = Counter.builder("announcement.viewcount.synced.total")
                .description("Total view counts synced from Redis to DB")
                .register(meterRegistry);
    }

    // Redis INCR: 원자적 연산 → 동시성 문제 없이 카운트 증가
    public void increment(Long announcementId) {
        redisTemplate.opsForValue().increment(KEY_PREFIX + announcementId);
    }

    // 30초마다 Redis → DB 배치 동기화
    // v1의 1000번 UPDATE가 여기서는 공문 수(N)번 UPDATE로 줄어듦
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void syncViewCountsToDB() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        long totalSynced = 0;
        for (String key : keys) {
            // GETDEL: 원자적으로 값을 가져오고 삭제 → 중복 반영 방지
            String value = redisTemplate.opsForValue().getAndDelete(key);
            if (value == null) continue;

            Long id = Long.parseLong(key.replace(KEY_PREFIX, ""));
            long count = Long.parseLong(value);

            announcementRepository.incrementViewCount(id, count);
            totalSynced += count;
        }

        if (totalSynced > 0) {
            syncCounter.increment(totalSynced);
            log.info("조회수 DB 동기화 완료: {}건", totalSynced);
        }
    }
}

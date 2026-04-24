package com.example.groupware.announcement.service;

import com.example.groupware.announcement.dto.AnnouncementCreateRequest;
import com.example.groupware.announcement.dto.AnnouncementDetailResponse;
import com.example.groupware.announcement.dto.AnnouncementListResponse;
import com.example.groupware.announcement.entity.Announcement;
import com.example.groupware.announcement.entity.AnnouncementType;
import com.example.groupware.announcement.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [v2 - 개선 방식]
 *
 * 캐싱 전략 비교
 *
 * v1 (레거시): 매 요청마다 DB SELECT
 *   - 1000명 동시 접속 → DB 커넥션 풀(기본 10개) 고갈 → 대기 큐 쌓임 → Connection timeout
 *
 * v2 (개선): Redis Cache-Aside 패턴
 *   - 첫 요청: Redis miss → DB SELECT → Redis 저장 (TTL 5분)
 *   - 이후 요청: Redis hit → DB 접근 없음 → 커넥션 풀 여유 → 응답 빠름
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final ViewCountService viewCountService;

    // 목록 캐시 TTL 30초: 새 공문 게시 후 최대 30초 내 반영
    @Cacheable(value = "announcement:list", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<AnnouncementListResponse> getList(Pageable pageable) {
        return announcementRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AnnouncementListResponse::from);
    }

    @Cacheable(value = "announcement:list", key = "'type_' + #type + '_' + #pageable.pageNumber")
    public Page<AnnouncementListResponse> getListByType(AnnouncementType type, Pageable pageable) {
        return announcementRepository.findAllByTypeOrderByCreatedAtDesc(type, pageable)
                .map(AnnouncementListResponse::from);
    }

    // 상세 캐시 TTL 5분: 인사발령 공문은 게시 후 수정이 거의 없음
    @Cacheable(value = "announcement:detail", key = "#id")
    public AnnouncementDetailResponse getDetail(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공문입니다: " + id));
        return AnnouncementDetailResponse.from(announcement);
    }

    // 조회수 증가는 캐시된 응답 반환과 분리 → 빠른 응답 보장
    public void incrementViewCount(Long id) {
        viewCountService.increment(id);
    }

    @Transactional
    @CacheEvict(value = {"announcement:list", "announcement:detail"}, allEntries = true)
    public AnnouncementDetailResponse create(AnnouncementCreateRequest request) {
        Announcement saved = announcementRepository.save(request.toEntity());
        return AnnouncementDetailResponse.from(saved);
    }
}

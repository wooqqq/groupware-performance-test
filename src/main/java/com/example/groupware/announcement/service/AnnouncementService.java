package com.example.groupware.announcement.service;

import com.example.groupware.announcement.dto.AnnouncementCreateRequest;
import com.example.groupware.announcement.dto.AnnouncementDetailResponse;
import com.example.groupware.announcement.dto.AnnouncementListResponse;
import com.example.groupware.announcement.entity.Announcement;
import com.example.groupware.announcement.entity.AnnouncementType;
import com.example.groupware.announcement.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [v1 - 레거시 방식]
 *
 * 캐싱 없이 모든 요청이 DB에 직접 도달한다.
 * → 1000명 동시 접속 시 1000번의 SELECT가 DB로 전달
 * → HikariCP 커넥션 풀(기본 10개) 고갈 → 대기 큐 쌓임 → Connection timeout
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final ViewCountService viewCountService;

    public Page<AnnouncementListResponse> getList(Pageable pageable) {
        return announcementRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AnnouncementListResponse::from);
    }

    public Page<AnnouncementListResponse> getListByType(AnnouncementType type, Pageable pageable) {
        return announcementRepository.findAllByTypeOrderByCreatedAtDesc(type, pageable)
                .map(AnnouncementListResponse::from);
    }

    public AnnouncementDetailResponse getDetail(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공문입니다: " + id));
        return AnnouncementDetailResponse.from(announcement);
    }

    @Transactional
    public void incrementViewCount(Long id) {
        viewCountService.increment(id);
    }

    @Transactional
    public AnnouncementDetailResponse create(AnnouncementCreateRequest request) {
        Announcement saved = announcementRepository.save(request.toEntity());
        return AnnouncementDetailResponse.from(saved);
    }
}

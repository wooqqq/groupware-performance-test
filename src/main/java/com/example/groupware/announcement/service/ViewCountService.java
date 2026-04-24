package com.example.groupware.announcement.service;

import com.example.groupware.announcement.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [v1 - 레거시 방식]
 *
 * 조회수 증가를 매 요청마다 DB UPDATE로 처리한다.
 * → 1000명이 동시에 같은 공문을 조회하면 동일 row에 UPDATE 1000번 발생
 * → row lock 경쟁 → 응답 지연 → 서비스 다운
 *
 * 실무 레거시(BrdViewLogService)에서 실제로 발생하는 문제와 동일한 패턴.
 */
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final AnnouncementRepository announcementRepository;

    @Transactional
    public void increment(Long announcementId) {
        announcementRepository.incrementViewCount(announcementId, 1L);
    }
}

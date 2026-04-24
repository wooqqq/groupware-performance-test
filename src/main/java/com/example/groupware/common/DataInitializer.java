package com.example.groupware.common;

import com.example.groupware.announcement.entity.Announcement;
import com.example.groupware.announcement.entity.AnnouncementType;
import com.example.groupware.announcement.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// nGrinder 부하 테스트용 샘플 데이터 생성
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AnnouncementRepository announcementRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (announcementRepository.count() > 0) {
            return;
        }

        List<Announcement> announcements = new ArrayList<>();

        // 인사발령 공문 (트래픽 폭증 시나리오의 주체)
        for (int i = 1; i <= 20; i++) {
            announcements.add(Announcement.builder()
                    .title(String.format("2024년 하반기 인사발령 공문 제%d호", i))
                    .content("""
                            다음과 같이 인사발령을 시행하오니 해당 사제들은 착임하시기 바랍니다.

                            1. 전보
                               - 홍길동 신부: 명동성당 → 절두산순교성지
                               - 김철수 신부: 혜화동성당 → 약현성당

                            2. 발령일자: 2024년 9월 1일

                            서울대교구 사무처장
                            """)
                    .author("서울대교구 사무처")
                    .type(AnnouncementType.PERSONNEL)
                    .createdAt(LocalDateTime.now().minusDays(i))
                    .build());
        }

        // 일반 공지사항
        for (int i = 1; i <= 30; i++) {
            announcements.add(Announcement.builder()
                    .title(String.format("일반 공지사항 제%d호", i))
                    .content("공지사항 내용입니다. 해당 내용을 숙지하시기 바랍니다.")
                    .author("서울대교구 사무처")
                    .type(AnnouncementType.GENERAL)
                    .createdAt(LocalDateTime.now().minusHours(i))
                    .build());
        }

        announcementRepository.saveAll(announcements);
        log.info("샘플 데이터 {}건 생성 완료", announcements.size());
    }
}
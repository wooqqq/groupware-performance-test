package com.example.groupware.announcement.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "announcements",
    indexes = {
        // 목록 조회 시 type + 날짜 내림차순이 가장 빈번한 쿼리 패턴
        @Index(name = "idx_type_created_at", columnList = "type, created_at DESC"),
        @Index(name = "idx_created_at", columnList = "created_at DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false, length = 100)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementType type;

    // Redis 배치 동기화 대상. DB에 직접 UPDATE하지 않음
    @Column(nullable = false)
    @Builder.Default
    private long viewCount = 0L;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void applyViewCount(long count) {
        this.viewCount += count;
    }
}

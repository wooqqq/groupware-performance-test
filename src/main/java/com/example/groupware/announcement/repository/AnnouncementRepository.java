package com.example.groupware.announcement.repository;

import com.example.groupware.announcement.entity.Announcement;
import com.example.groupware.announcement.entity.AnnouncementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Page<Announcement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Announcement> findAllByTypeOrderByCreatedAtDesc(AnnouncementType type, Pageable pageable);

    // 레거시는 매 조회마다 UPDATE → 1000명 동시 접속 시 row lock 경쟁 발생
    // 개선: Redis INCR 누적 후 배치로 한 번에 반영
    @Modifying
    @Query("UPDATE Announcement a SET a.viewCount = a.viewCount + :count WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id, @Param("count") long count);
}

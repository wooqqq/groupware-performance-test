package com.example.groupware.announcement.dto;

import com.example.groupware.announcement.entity.Announcement;
import com.example.groupware.announcement.entity.AnnouncementType;

import java.io.Serializable;
import java.time.LocalDateTime;

public record AnnouncementDetailResponse(
        Long id,
        String title,
        String content,
        String author,
        AnnouncementType type,
        long viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {

    public static AnnouncementDetailResponse from(Announcement announcement) {
        return new AnnouncementDetailResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAuthor(),
                announcement.getType(),
                announcement.getViewCount(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}

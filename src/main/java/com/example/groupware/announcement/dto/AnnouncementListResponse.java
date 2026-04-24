package com.example.groupware.announcement.dto;

import com.example.groupware.announcement.entity.Announcement;
import com.example.groupware.announcement.entity.AnnouncementType;
import java.io.Serializable;
import java.time.LocalDateTime;

public record AnnouncementListResponse(
        Long id,
        String title,
        String author,
        AnnouncementType type,
        long viewCount,
        LocalDateTime createdAt
) implements Serializable {

    public static AnnouncementListResponse from(Announcement announcement) {
        return new AnnouncementListResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getAuthor(),
                announcement.getType(),
                announcement.getViewCount(),
                announcement.getCreatedAt()
        );
    }
}

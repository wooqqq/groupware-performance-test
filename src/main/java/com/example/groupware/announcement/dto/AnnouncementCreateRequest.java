package com.example.groupware.announcement.dto;

import com.example.groupware.announcement.entity.Announcement;
import com.example.groupware.announcement.entity.AnnouncementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AnnouncementCreateRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotBlank String author,
        @NotNull AnnouncementType type
) {
    public Announcement toEntity() {
        return Announcement.builder()
                .title(title)
                .content(content)
                .author(author)
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();
    }
}

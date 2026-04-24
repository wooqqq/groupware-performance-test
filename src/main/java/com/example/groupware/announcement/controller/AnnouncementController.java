package com.example.groupware.announcement.controller;

import com.example.groupware.announcement.dto.AnnouncementCreateRequest;
import com.example.groupware.announcement.dto.AnnouncementDetailResponse;
import com.example.groupware.announcement.dto.AnnouncementListResponse;
import com.example.groupware.announcement.entity.AnnouncementType;
import com.example.groupware.announcement.service.AnnouncementService;
import com.example.groupware.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AnnouncementListResponse>>> getList(
            @RequestParam(required = false) AnnouncementType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AnnouncementListResponse> result = (type != null)
                ? announcementService.getListByType(type, pageable)
                : announcementService.getList(pageable);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementDetailResponse>> getDetail(@PathVariable Long id) {
        AnnouncementDetailResponse response = announcementService.getDetail(id); // 캐시 적용
        announcementService.incrementViewCount(id);                              // Redis INCR (비동기적)
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AnnouncementDetailResponse>> create(
            @Valid @RequestBody AnnouncementCreateRequest request
    ) {
        AnnouncementDetailResponse response = announcementService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}

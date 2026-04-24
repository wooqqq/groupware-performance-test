package com.example.groupware.announcement;

import com.example.groupware.announcement.dto.AnnouncementCreateRequest;
import com.example.groupware.announcement.dto.AnnouncementDetailResponse;
import com.example.groupware.announcement.entity.Announcement;
import com.example.groupware.announcement.entity.AnnouncementType;
import com.example.groupware.announcement.repository.AnnouncementRepository;
import com.example.groupware.announcement.service.AnnouncementService;
import com.example.groupware.announcement.service.ViewCountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @InjectMocks
    private AnnouncementService announcementService;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private ViewCountService viewCountService;

    @Test
    @DisplayName("공문 상세 조회 시 ViewCountService.increment가 호출된다")
    void incrementViewCountOnDetail() {
        Announcement announcement = Announcement.builder()
                .title("인사발령 공문")
                .content("내용")
                .author("서울교구")
                .type(AnnouncementType.PERSONNEL)
                .createdAt(LocalDateTime.now())
                .build();

        given(announcementRepository.findById(1L)).willReturn(Optional.of(announcement));

        announcementService.getDetail(1L);
        announcementService.incrementViewCount(1L);

        verify(viewCountService).increment(1L);
    }

    @Test
    @DisplayName("존재하지 않는 공문 조회 시 예외가 발생한다")
    void throwExceptionWhenNotFound() {
        given(announcementRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.getDetail(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 공문입니다");
    }

    @Test
    @DisplayName("공문 생성이 정상적으로 동작한다")
    void createAnnouncement() {
        AnnouncementCreateRequest request = new AnnouncementCreateRequest(
                "인사발령 공문", "내용", "서울교구", AnnouncementType.PERSONNEL
        );

        Announcement saved = Announcement.builder()
                .title(request.title())
                .content(request.content())
                .author(request.author())
                .type(request.type())
                .createdAt(LocalDateTime.now())
                .build();

        given(announcementRepository.save(any())).willReturn(saved);

        AnnouncementDetailResponse response = announcementService.create(request);

        assertThat(response.title()).isEqualTo("인사발령 공문");
        assertThat(response.type()).isEqualTo(AnnouncementType.PERSONNEL);
    }
}

CREATE TABLE IF NOT EXISTS announcements
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(500)                        NOT NULL,
    content    TEXT                                NOT NULL,
    author     VARCHAR(100)                        NOT NULL,
    type       VARCHAR(20)                         NOT NULL,
    view_count BIGINT    DEFAULT 0                 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 목록 조회 최적화 인덱스
CREATE INDEX idx_type_created_at ON announcements (type, created_at DESC);
CREATE INDEX idx_created_at ON announcements (created_at DESC);

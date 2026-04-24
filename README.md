# 그룹웨어 트래픽 과부하 개선 — 포트폴리오 프로젝트

> **"인사발령 공문이 올라오면 서버가 죽는다"** — 실무에서 겪은 문제를 재현하고, 수치로 개선을 증명한 프로젝트입니다.

## 배경

그룹웨어를 운영하면서 반복적으로 겪은 문제가 있습니다.

인사발령 공문이 게시되는 순간, 전 직원 ~1000명이 동시에 접속합니다.  
그 결과: **API 응답 지연 → DB 커넥션 고갈 → 서비스 다운.**

실무 코드는 건드릴 수 없어서, 동일한 병목 패턴을 토이 프로젝트로 재현하고 직접 개선했습니다.

> 실무 환경은 Java 8 / Spring MVC / MyBatis 기반이나, 이 프로젝트는 Java 17 / Spring Boot 3 / JPA로 구성했습니다.  
> 기술 스택은 다르지만, **병목의 원인과 해결 방식은 동일**합니다.

---

## 성능 개선 결과

> nGrinder 1000 vUser / 5분 부하 테스트 기준

| 지표 | v1 (레거시) | v2 (개선) | 개선율 |
|------|------------|----------|--------|
| TPS | 측정 예정 | 측정 예정 | - |
| P95 응답시간 | 측정 예정 | 측정 예정 | - |
| 에러율 | 측정 예정 | 측정 예정 | - |
| DB 커넥션 사용량 | 측정 예정 | 측정 예정 | - |

---

## 문제 분석

### 병목 1 — DB SELECT 폭증 (커넥션 풀 고갈)

```
[v1] 1000명 동시 접속
  → 각 요청마다 DB SELECT
  → 커넥션 풀(20개) 고갈
  → 나머지 980개 요청이 대기 큐에서 타임아웃
```

### 병목 2 — 조회수 UPDATE row lock 경쟁

```
[v1] 1000명이 같은 공문 조회
  → 동일한 row에 UPDATE 1000번
  → row lock 경쟁
  → 응답 지연 → 서비스 다운
```

---

## 해결 방법

### 해결 1 — Redis Cache-Aside (캐싱)

```
[v2] 1000명 동시 접속
  → 첫 번째 요청만 DB SELECT → Redis에 저장
  → 나머지 999개 요청은 Redis에서 응답 (DB 접근 없음)
  
캐시 TTL: 목록 30초 / 상세 5분
```

### 해결 2 — Redis INCR + 배치 DB 동기화 (조회수)

```
[v2] 1000명이 같은 공문 조회
  → Redis INCR (메모리 연산, ~0.1ms, lock 없음)
  → 30초마다 누적된 값을 DB에 일괄 반영 (UPDATE 1번)
  
1000번 UPDATE → N번 UPDATE (N = 공문 수)
```

---

## 브랜치 구조

```
main              ← v1 baseline (레거시 방식, 캐시 없음)
v2/optimized      ← v2 개선 (Redis 캐싱 + 배치 동기화)
```

v1과 v2의 핵심 차이는 `AnnouncementService`, `ViewCountService`, `CacheConfig` 3개 파일입니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| DB | MySQL 8.0 + HikariCP (풀 사이즈 20) |
| Cache | Redis 7 |
| ORM | Spring Data JPA |
| Migration | Flyway |
| Monitoring | Prometheus + Grafana |
| Load Test | nGrinder |

---

## 실행 방법

### 1. 인프라 시작

```bash
docker-compose up -d
```

MySQL(3306), Redis(6379), Prometheus(9090), Grafana(3000) 가 실행됩니다.

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

서버 시작 시 공문 데이터 100건이 자동으로 생성됩니다.

### 3. API 확인

```bash
# 공문 목록 조회
GET http://localhost:8080/api/announcements

# 공문 상세 조회
GET http://localhost:8080/api/announcements/{id}
```

### 4. nGrinder 부하 테스트

1. [nGrinder](https://github.com/naver/ngrinder) 설치 및 실행
2. `ngrinder/AnnouncementLoadTest.groovy` 스크립트 업로드
3. 가상 유저: 1000 / 실행 시간: 5분으로 테스트 실행
4. Grafana(`http://localhost:3000`)에서 실시간 메트릭 확인

### 5. Grafana 모니터링 확인

- admin / admin123 으로 로그인
- Prometheus 데이터소스 자동 설정됨
- 주요 메트릭:
  - `http_server_requests_seconds` — P95 응답시간
  - `hikaricp_connections_active` — DB 커넥션 사용량
  - `announcement_viewcount_synced_total` — 배치 동기화 횟수 (v2 전용)

---

## 핵심 코드 위치

| 파일 | 설명 |
|------|------|
| `service/AnnouncementService.java` | v1 vs v2 캐싱 적용 차이 |
| `service/ViewCountService.java` | v1 DB UPDATE vs v2 Redis INCR + 배치 |
| `config/CacheConfig.java` | Redis 캐시 TTL 설정 |
| `ngrinder/AnnouncementLoadTest.groovy` | 부하 테스트 시나리오 |

/**
 * nGrinder 부하 테스트 시나리오
 *
 * 목표: 인사발령 공문 게시 시 ~1000명 동시 접속 상황 재현
 *
 * 실행 방법:
 *   1. nGrinder 웹 UI → Script → Create → 내용 전체 교체 후 Save
 *   2. Performance Test → Create Test → 스크립트 선택
 *   3. Vuser: 100~500, Duration: 5분으로 테스트 실행
 *   4. Grafana(http://localhost:3000)에서 실시간 메트릭 확인
 *
 * 측정 지표:
 *   - TPS (초당 처리량)
 *   - 평균 응답시간 / P95 응답시간
 *   - 에러율
 *   → 개선 전/후 수치 비교로 성능 개선 효과를 수치화
 */

import static net.grinder.script.Grinder.grinder
import net.grinder.script.GTest
import net.grinder.plugin.http.HTTPRequest
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GrinderRunner)
class AnnouncementLoadTest {

    public static GTest listTest
    public static GTest detailTest
    public static HTTPRequest request

    // host.docker.internal: Docker 컨테이너에서 호스트 PC(Spring Boot)에 접근하는 주소
    private static final String BASE_URL = "http://host.docker.internal:8080"

    @BeforeProcess
    public static void beforeProcess() {
        listTest   = new GTest(1, "공문 목록 조회")
        detailTest = new GTest(2, "공문 상세 조회 (인사발령)")
        request    = new HTTPRequest()
        grinder.logger.info("테스트 초기화 완료")
    }

    @BeforeThread
    public void beforeThread() {
        listTest.record(this, "testList")
        detailTest.record(this, "testDetail")
        grinder.statistics.delayReports = true
    }

    // 시나리오 1: 공문 목록 조회 (Redis 캐시 적중 확인)
    @Test
    public void testList() {
        def response = request.GET("${BASE_URL}/api/announcements?page=0&size=20")
        if (response.statusCode != 200) {
            grinder.logger.error("목록 조회 실패: ${response.statusCode}")
        }
    }

    // 시나리오 2: 인사발령 공문 상세 조회 (조회수 Redis INCR 확인)
    @Test
    public void testDetail() {
        // 스레드 번호 기반으로 10개 공문을 분산 조회 → 특정 공문 집중 접속 재현
        int announcementId = (grinder.threadNumber % 10) + 1
        def response = request.GET("${BASE_URL}/api/announcements/${announcementId}")
        if (response.statusCode != 200) {
            grinder.logger.error("상세 조회 실패: ${response.statusCode}")
        }
    }
}

/**
 * nGrinder 부하 테스트 시나리오
 *
 * 목표: 인사발령 공문 게시 시 ~1000명 동시 접속 상황 재현
 *
 * 실행 방법:
 *   1. nGrinder (https://github.com/naver/ngrinder) 설치 및 실행
 *   2. 이 스크립트를 nGrinder 스크립트 메뉴에 업로드
 *   3. 테스트 생성: 가상 유저 수 = 500~1000, 실행 시간 = 5분
 *   4. Grafana에서 실시간 메트릭 확인
 *
 * 측정 지표:
 *   - TPS (초당 처리량)
 *   - 평균 응답시간 / P95 응답시간
 *   - 에러율
 *   → 개선 전/후 수치 비교로 성능 개선 효과를 수치화
 */

import static net.grinder.script.Grinder.grinder
import static org.junit.Assert.assertThat
import static org.hamcrest.Matchers.is
import net.grinder.script.GTest
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.ngrinder.http.HTTPRequest
import org.ngrinder.http.HTTPResponse
import org.ngrinder.http.HTTPPluginControl

@RunWith(GrinderRunner)
class AnnouncementLoadTest {

    public static GTest listTest
    public static GTest detailTest
    public static HTTPRequest request

    @BeforeProcess
    public static void beforeProcess() {
        HTTPPluginControl.getConnectionDefaults().timeout = 6000
        listTest   = new GTest(1, "공문 목록 조회")
        detailTest = new GTest(2, "공문 상세 조회 (인사발령)")
        request    = new HTTPRequest()
    }

    @BeforeThread
    public void beforeThread() {
        listTest.record(this, "testList")
        detailTest.record(this, "testDetail")
        grinder.statistics.delayReports = true
    }

    @Before
    public void before() {
        HTTPPluginControl.getThreadControl().endPage()
    }

    // 시나리오 1: 공문 목록 조회 (Redis 캐시 적중 확인)
    @Test
    public void testList() {
        HTTPResponse response = request.GET("http://localhost:8080/api/announcements?page=0&size=20")
        assertThat(response.statusCode, is(200))
    }

    // 시나리오 2: 인사발령 공문 상세 조회 (조회수 Redis INCR 확인)
    @Test
    public void testDetail() {
        // 스레드 번호 기반으로 10개 공문을 분산 조회 → 특정 공문 집중 접속 재현
        int announcementId = (grinder.threadNumber % 10) + 1
        HTTPResponse response = request.GET("http://localhost:8080/api/announcements/${announcementId}")
        assertThat(response.statusCode, is(200))
    }
}

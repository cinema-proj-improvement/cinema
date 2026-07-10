# 좌석 선점 스트레스 테스트 결과 (시나리오 B)

- 작성일: 2026-07-10
- 대상 브랜치: `refactor/seat-lock-redis`
- 테스트 스크립트: `k6/scenarios/b-seat-hold-stress.js`
- 테스트 환경: dev ECS Fargate(1 vCPU) + RDS MySQL `db.t4g.micro`(2 vCPU) + Redis
- 관련 문서: [`spike-test-results.md`](./spike-test-results.md) (시나리오 A, 정합성 검증)

> 이 문서는 1차 조사 결과다. **헬스체크 아키텍처를 고치기 전** 상태에서 얻은 결과이며,
> 수정 후 재검증한 결과는 별도로 추가될 예정이다.

## 시나리오 개요

시나리오 A(spike)가 "순간 폭증에도 정합성이 깨지지 않는가"를 봤다면, B는 "요청률을 계단식으로
올렸을 때 어디서부터, 어떻게 무너지는가"를 본다. 좌석 40석을 hold → 즉시 cancel-hold로 반납하는
루프로 지속적인 락 경합을 유지하면서 rps를 단계적으로 올렸다. 로그인/CSRF는 시나리오 A와
동일하게 `setup()`에서 미리 끝내고, 측정 구간은 순수하게 홀드+취소 사이클만 반복한다.

---

## 1차 실행 — 5~100rps

`5 → 15 → 30 → 60 → 100` rps, 각 단계 20초 유지 (총 ~2분 40초).

### 결과 — 완전히 통과

```
stress_hold_success............: 5274   32.673903/s
stress_hold_conflict...........: 1030   6.381138/s
stress_hold_unexpected_error...: 0      0/s
stress_hold_duration...........: avg=107.84ms min=26.6ms med=96.73ms max=561.94ms p(90)=187.42ms p(95)=217.03ms

http_req_failed.................: 8.88%  1030 out of 11590
iterations......................: 6304   39.055041/s
vus..............................: min=0 max=34 (preAllocatedVUs=50, maxVUs=500)

running (2m41.4s), 6304 complete and 0 interrupted iterations
```

- 이터레이션 수(6304)가 설계한 rps 램프 곡선의 이론치(≈6300)와 거의 정확히 일치 — k6가 목표
  rps를 못 따라가서 밀린 게 아니라, 서버가 그 rps를 실제로 다 받아냈다는 뜻.
- 100rps까지도 지연시간 증가 추세조차 안 보임. **이 구간까지는 문제없음.**

---

## 2차 실행 — 100~1500rps (한계점 발견)

1차에서 100rps가 멀쩡했으므로 상한을 `100 → 200 → 400 → 800 → 1500` rps로 올려서
(각 단계 30초 유지, 총 ~3분 20초) 재시도했다.

### 결과 — 한계 도달, 테스트 중도 중단

```
stress_hold_success............: 3275   17.229695/s
stress_hold_conflict...........: 37113  195.250587/s
stress_hold_unexpected_error...: 817    4.298217/s   <- threshold(count<500) 초과로 abortOnFail 발동
stress_hold_duration...........: avg=7.55s min=4.98ms med=10.81s max=14.81s p(90)=12.1s p(95)=12.28s

http_req_failed.................: 85.32% 37934 out of 44458
dropped_iterations..............: 72336  380.55793/s   <- k6가 쏘려던 요청의 상당수를 아예 못 쏨
vus..............................: max=3000 (maxVUs 상한까지 전부 사용)

running (3m10.1s), 41168 complete / 3000 interrupted iterations
ERRO: thresholds on metrics 'stress_hold_unexpected_error' were crossed;
      stopping test prematurely
```

테스트 도중 **기존 ECS task가 종료되고 새 task로 교체**되는 것을 실시간으로 관찰함
(`status=0` 에러 다수 — 연결 자체가 끊긴 것).

---

## 원인 조사

### 1) CloudWatch 지표

| 시각(KST) | ECS CPU | ECS Memory |
|---|---|---|
| 평소 | ~1% | ~24.5%(baseline) |
| 15:25~15:28 (부하 구간) | 100%까지 상승, 지속 | 32.4% → 39.6%까지 계속 상승 |
| 15:29 (부하 종료 직후) | 2.6%로 즉시 회복 | **38%대에서 회복 안 됨** |

CPU는 부하가 끝나자마자 즉시 정상화됐지만 메모리는 안 돌아왔다 — 정리 안 되고 쌓이는 리소스가
있다는 신호였는데, 실제 원인은 아래에서 보듯 메모리 누수가 아니라 **큐에 쌓인 대기 요청/스레드**였다.

### 2) ECS 콘솔 — Stopped reason (사용자 직접 확인)

```
다음 시간에 작업이 중지됨: 2026-07-10T06:34:55.781Z
Task failed ELB health checks in
  (target-group arn:aws:elasticloadbalancing:...:targetgroup/cinema-app-tg/...)
종료 코드: 143
```

**종료 코드 143 = 128 + 15(SIGTERM).** OOM Kill(SIGKILL=137)이 아니라 **ECS가 헬스체크 실패를
근거로 정상 종료 신호를 보낸 것.** ALB가 이 target을 unhealthy로 판정 → ECS가 SIGTERM으로
task를 내리고 교체.

### 3) CloudWatch Logs (`/ecs/cinema-task`) — 죽은 task의 마지막 로그

- **ERROR 레벨 로그 0건.** WARN 38건은 전부 이미 아는 "좌석 이미 선점됨" 비즈니스 경고뿐.
- `GET /health 200 1ms` — **마지막까지(06:28:30) 정상적으로 빠르게 응답.** 이후 5분 넘게 헬스체크
  로그가 아예 없음(체크가 실패해서 로그가 남은 게 아니라, 아예 처리가 안 됨).
- 06:34:20 — `HikariPool-1 - Shutdown initiated...` → `Shutdown completed.` **완전히 정상적인
  Spring Boot 종료 훅**이 끝까지 실행됨. 앱이 강제로 죽은 게 아니라 스스로 깔끔하게 종료했다는 뜻.

### 4) 코드 확인 — 헬스체크가 왜 굶었는가

```java
// global/health/HealthCheckController.java
@RestController
public class HealthCheckController {
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
```

Spring Boot Actuator가 아니라 **일반 `@RestController`**다(`build.gradle`에 actuator 의존성도
없음). 즉 `/health`도 `/reservations/holds` 같은 다른 API와 **완전히 동일한 포트(8080), 동일한
내장 Tomcat 스레드풀**을 공유한다. 실제 로그에서도 헬스체크 요청이 `http-nio-8080-exec-*`라는,
비즈니스 요청과 똑같은 이름의 스레드로 처리된 것이 확인됨.

---

## 결론 — 정확한 인과관계

1. 부하가 증가하면서(200→400→800rps 구간 어딘가) Tomcat 요청 큐에 처리 못 한 요청이 쌓이기 시작
2. `/health` 요청도 예외 없이 같은 큐에서 순서를 기다려야 했고, 큐가 충분히 밀리자
   ALB 헬스체크 타임아웃 안에 응답을 못 받는 상황 발생
3. ALB가 연속 실패 임계치를 넘기자 target을 unhealthy로 판정
4. ECS가 SIGTERM으로 task를 정상 종료(코드 143) → 새 task로 교체

**앱 자체는 한 번도 에러를 낸 적이 없다.** 메모리 부족으로 뻗은 것도, 코드가 예외를 던진 것도
아니다. "너무 바빠서 헬스체크에 응답할 스레드 자리조차 못 잡았을 뿐"인데, 오케스트레이터는 이걸
"죽었다"고 오판해서 멀쩡히 살아있던 프로세스를 강제 재시작시켰다.

**이건 좌석락 코드(`0e8a4b8`)의 결함이 아니다.** 원인은 헬스체크 엔드포인트가 Actuator의 별도
management 포트를 쓰지 않고 메인 트래픽과 스레드/큐를 공유하는 **인프라·구성상의 결함**이다.

또한 이 결함 때문에 **지금까지 얻은 "몇 rps부터 깨지는가" 하는 정밀한 숫자는 의미가 제한적이다.**
그 숫자는 "이 헬스체크 구조가 그대로인 상태에서 오판성 강제종료가 시작되는 지점"이지, 시스템의
진짜 처리 용량 한계가 아니기 때문이다.

## 다음 단계 (예정)

1. `application.yml`에 `management.server.port` 분리 설정 추가 (헬스체크 전용 커넥터로 분리)
2. ALB/ECS 헬스체크 대상을 새 포트로 변경
3. 재배포 후 동일한 100~1500rps 스윕을 재실행 → 이번엔 헬스체크 기아 없이 **진짜 처리 용량 한계**를
   측정
4. 1차(수정 전) vs 2차(수정 후) 결과를 종합해서 트러블슈팅 문서로 정리

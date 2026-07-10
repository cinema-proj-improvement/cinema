# 헬스체크 Actuator 전환 + 관리 포트 분리 계획

## 배경

`k6/stress-test-results.md` 시나리오 B(스트레스 테스트)에서, 부하가 몰렸을 때 dev ECS task가
ALB 헬스체크 실패로 강제 종료되는 것을 확인했다(종료 코드 143, SIGTERM). 원인은 코드 버그가
아니라 **헬스체크(`GET /health`)가 일반 `@RestController`로 구현되어 있어, 다른 API 요청과
동일한 내장 Tomcat 스레드풀/큐를 공유**하기 때문이었다. 부하로 요청 큐가 밀리면 헬스체크
요청도 같이 밀려서 ALB 타임아웃 안에 응답을 못 하고, ALB가 target을 unhealthy로 판정 →
ECS가 정상 동작 중이던 task를 오판성으로 죽이고 재기동하는 결과로 이어졌다.

해결책은 헬스체크를 Spring Boot Actuator로 옮기고, **메인 애플리케이션 포트(8080)와 완전히
분리된 관리 포트**에서 서비스하는 것이다. 이러면 메인 트래픽이 아무리 밀려도 헬스체크는
별도 스레드풀에서 영향받지 않고 응답한다.

## 인프라 구조 (전제)

```
Internet → ALB(public subnet) → Target Group → ECS Task(private subnet, awsvpc)
```

- ECS 클러스터: `cinema`, 서비스: `cinema-app-service`
- 로드밸런서: `app/cinema-alb/...`, 타겟 그룹: `cinema-app-tg`
- 현재 트래픽/헬스체크 모두 컨테이너 포트 8080 사용

## 1단계 — 코드 변경

### ① `build.gradle`
```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

### ② `application.yml` (또는 `application-dev.yml`)
```yaml
management:
  server:
    port: 8081          # 메인 서버 포트(8080)와 분리된 별도 내장 커넥터
  endpoints:
    web:
      exposure:
        include: health  # health만 노출. env/beans/heapdump 등은 절대 포함 금지(정보 유출 위험)
  endpoint:
    health:
      show-details: never   # 기본값 - "UP"만 반환, 내부 상세정보 노출 안 함
```

- `management.server.port`를 메인 포트와 다르게 지정하면, Spring Boot가 완전히 별도의 내장
  Tomcat 커넥터(자체 스레드풀)를 그 포트로 띄운다. 엔드포인트 경로는 기본값 `/actuator/health`.

### ③ 기존 `HealthCheckController`(`GET /health`) 정리
- Actuator의 `/actuator/health`가 동일 역할을 대체하므로, ALB 전환이 끝나면 제거를 검토.
  (전환 검증 전까지는 남겨둬도 무방 — 어차피 이번 문제의 원인이었던 "스레드풀 공유"만 없어지면 됨)

### ④ Spring Security 영향 확인
- 관리 포트가 메인 포트와 분리되면 별도 자식 컨텍스트로 뜨기 때문에, 기존 `SecurityConfig`의
  `SecurityFilterChain`이 이 포트에는 보통 적용되지 않는다. **배포 후 인증 없이
  `/actuator/health`가 200을 반환하는지 반드시 직접 확인.** 막혀 있으면 관리 컨텍스트 전용
  시큐리티 설정 추가 필요.

## 2단계 — AWS 인프라 변경

### ① ECS Task Definition — 컨테이너 포트 매핑 추가
- Fargate(`awsvpc` 네트워크 모드)는 컨테이너가 노출할 포트를 태스크 정의에 명시해야 ENI에서
  접근 가능하다. 기존 8080 매핑에 **8081 포트 매핑 추가** 필요.
- 리포에 IaC(Terraform 등)가 없으므로 콘솔에서 새 태스크 정의 리비전 등록, 또는
  `aws ecs register-task-definition`으로 직접 처리.
- GitHub Actions 배포 파이프라인(`dev-cicd.yml`)은 "현재 태스크 정의를 가져와 이미지 태그만
  교체"하는 방식이라, 이 포트 매핑은 한 번 반영하면 이후 배포에서도 계속 유지된다.

### ② 보안 그룹 — ALB → ECS 8081 인바운드 허용
- ECS task 보안그룹에 ALB 보안그룹으로부터 **8081 포트 인바운드 허용** 규칙 추가
  (기존 8080 규칙과 동일한 소스로 8081도 추가).

### ③ 타겟 그룹 — 헬스체크 포트 오버라이드
- 타겟 그룹을 새로 만들 필요 없음. 기존 `cinema-app-tg`의 헬스체크 설정에서
  **Health check port: Override → 8081**로 변경.
- **트래픽은 그대로 8080, 헬스체크만 8081로 분리** — ALB 리스너/퍼블릭 노출은 전혀 안 건드림
  (8081은 VPC 내부 ALB↔ECS 사이에서만 사용, 외부 노출 불필요).
- 헬스체크 경로도 `/health` → `/actuator/health`로 변경.

## 3단계 — 배포 순서 (중요)

**순서를 반드시 지켜야 한다. 반대로 하면 즉시 전체 다운된다.**

1. 코드(1단계) 머지 + 배포 — 이 시점엔 8080만 트래픽/헬스체크 대상, 8081은 아직 아무도 안 봄 → 안전
2. 배포된 새 이미지가 8081에서 `/actuator/health` 정상 응답하는지 확인
   (task 내부 또는 private IP로 직접 curl)
3. 확인 후에만 2단계(태스크 정의 포트 매핑 + 보안그룹 + 타겟그룹 헬스체크 포트 전환) 진행
4. 순서를 바꿔서 헬스체크를 먼저 8081로 돌렸는데 앱이 아직 8081을 안 열고 있으면
   ALB가 즉시 전체 target을 unhealthy로 판정 → 전체 다운

## 4단계 — 검증 (예정)

전환 완료 후, `k6/scenarios/b-seat-hold-stress.js`로 동일한 100~1500rps 스윕을 재실행해서:
- 이번엔 헬스체크 기아로 인한 task 강제 종료가 재현되지 않는지
- 재현되지 않는다면, 그때 나오는 처리량/지연시간 한계가 "이 인프라의 진짜 처리 용량"

이 결과를 `k6/stress-test-results.md`에 수정 전(1차) vs 수정 후(2차) 비교로 추가하고,
최종 트러블슈팅 문서로 종합한다.

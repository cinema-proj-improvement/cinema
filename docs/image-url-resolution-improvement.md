# 영화 이미지 URL 변환 구조 개선안

## 배경

`movie_images.image_url` 컬럼에는 실제 URL이 아니라 스토리지 키(S3 기준 `movies/thumbnails/{uuid}.{ext}`)가 저장되어 있고, 화면/응답에 노출하려면 `FileService.toImageUrl(key)`로 CDN 베이스 URL을 붙여줘야 한다.

이 변환 호출이 빠진 채로 응답을 만든 지점이 여러 곳에서 발견되었다 (`/admin/movies` 목록, 사용자 영화 목록, 홈 화면 TOP4, 예약 결제 페이지). 원인은 "응답을 만드는 코드가 직접 `fileService.toImageUrl()`을 호출해야 한다"는 규칙이 강제되지 않고, 개발자가 매번 기억해야만 지켜지는 구조이기 때문이다.

2026-07-02 기준으로 발견된 누락 지점은 서비스 레이어에서 개별적으로 수정했다 (`MovieService.getAdminMovieListPage`, `MovieService.getUserMovieList`, `HomeService.getTop4Movies`, `ReservationService.getCheckoutPage`). 이 문서는 **같은 종류의 버그가 앞으로 또 발생하는 것을 구조적으로 막기 위한 개선안**을 정리한다. 지금 당장 적용하지는 않았고, 향후 검토용으로 남겨둔다.

## 현재 구조의 한계

- 매퍼(`MovieMapper`, `ReservationMapper` 등)는 전부 `@Mapper(componentModel = "spring")` 순수 인터페이스로, 인스턴스 필드(의존성 주입)를 가질 수 없다. 그래서 `FileService` 같은 외부 의존이 필요한 변환은 매퍼가 아니라 항상 호출부(서비스)가 미리 처리해서 넘겨줘야 한다.
- 결과적으로 "raw key → URL 변환"이 서비스 레이어 곳곳에 개별적으로 흩어져 있고, 새로운 응답 DTO/엔드포인트가 추가될 때마다 매번 새로 챙겨야 한다.
- 컴파일러나 타입 시스템이 "이 문자열 필드는 변환됐는지 여부"를 구분해주지 않는다 — `String thumbnail`이라는 필드만 보고는 raw key인지 완성된 URL인지 알 수 없다.

## 검토한 대안과 트레이드오프

### 안 1. 매퍼 레이어에 FileService 주입

MapStruct 매퍼를 인터페이스에서 abstract class로 바꾸고 `FileService`를 생성자 주입받아, 매퍼 안에서 변환을 수행하는 방식.

```java
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class MovieMapper {
    protected FileService fileService;

    public MovieListResponse toMovieListResponse(MovieListRow row) {
        return new MovieListResponse(
                row.getId(),
                fileService.toImageUrl(row.getThumbnail()),
                ...
        );
    }
}
```

- 장점: 서비스 메서드 코드가 단순해짐.
- 단점: 이미지 응답을 만드는 지점이 `MovieMapper`/`ReservationMapper`(+ 매퍼가 없는 `HomeService`는 신규 `HomeMapper` 필요)로 나뉘어 있어서, 결국 3곳에 규칙을 반복 적용해야 하는 건 동일함. "재발 방지"라는 목표는 달성하지 못하고, 매퍼 레이어 전체의 일관성(현재는 전부 순수 인터페이스)만 깨뜨림.
- **평가: 근본 해결책이 아님. 채택하지 않는 게 낫다.**

### 안 2. 업로드/저장 시점에 완전한 URL을 저장

이미지 업로드 완료 시 `MovieImage.imageUrl`에 raw key 대신 `fileService.toImageUrl(key)`로 변환한 완성 URL을 저장. 이후 조회 경로에서는 변환이 아예 필요 없어짐 (DB 값을 그대로 응답에 사용).

- 장점: 런타임 변환 로직 자체가 사라지므로 "빠뜨릴 여지"가 원천적으로 없어짐. 조회 경로 코드가 가장 단순해짐.
- 단점:
  - CDN/스토리지 base URL이 바뀌면(도메인 이전, CDN 벤더 교체 등) 기존에 저장된 모든 URL을 마이그레이션해야 함 — 현재는 `image-base-url` 설정값만 바꾸면 되는 구조인데 이 장점을 잃음.
  - `local` 프로필처럼 스토리지 타입이 환경마다 다를 경우, DB에 저장된 값이 환경 종속적이 되어 dev↔local 간 데이터 이관 시 문제가 생길 수 있음.
- **평가: 변환 누락 자체는 막지만, "저장된 URL의 환경 독립성"이라는 현재 설계의 장점과 상충한다. 정책적 결정 필요.**

### 안 3. 전용 타입으로 감싸서 변환을 강제 (raw key / resolved URL 타입 분리)

`String` 하나로는 "아직 변환 안 된 값"과 "변환 완료된 값"을 컴파일러가 구분할 수 없다는 게 근본 원인이므로, 이 둘을 서로 다른 타입으로 분리한다.

```java
// 아직 변환되지 않은 값 - 인프라 세부사항을 모르는 상태. 자유롭게 생성 가능.
public class ImageKey {
    private final String value;
    public ImageKey(String value) { this.value = value; }
    public String getValue() { return value; }
}

// 변환 완료된 값 - 표현 계층으로 나갈 준비가 된 상태.
public final class ResolvedImageUrl {
    private final String url;
    private ResolvedImageUrl(String url) { this.url = url; }   // 생성자를 잠근다

    static ResolvedImageUrl of(String url) { return new ResolvedImageUrl(url); }
    // ↑ package-private: FileService(또는 전용 Resolver)와 같은 패키지에서만 호출 가능

    @JsonValue
    public String getUrl() { return url; }

    @Override public String toString() { return url; }  // Thymeleaf th:src="${...}" 그대로 동작
}
```

그리고 DTO의 이미지 필드 타입을 `String`이 아니라 `ResolvedImageUrl`로 강제한다.

```java
public class MovieListResponse {
    private final Long id;
    private final ResolvedImageUrl thumbnail;   // String 아님
    ...
}
```

**핵심은 타입 분리 자체가 아니라 `ResolvedImageUrl`의 생성 경로를 잠그는 것이다.** 생성자가 `public`으로 열려 있으면 `new ResolvedImageUrl(rawKey)`처럼 raw key를 그대로 감싸버릴 수 있어 지금 겪은 버그가 형태만 바뀐 채 재발한다. 생성을 `FileService`(혹은 전용 Resolver) 내부로만 제한해야 "FileService를 거치지 않고는 이 타입의 인스턴스 자체를 만들 수 없다"가 성립한다.

이렇게 생성 경로를 잠그면, **Mapper에 DI를 넣을 필요가 없어진다.** 이미 `MovieMapper.toMovieDetailResponse(Movie movie, String thumbnail, List<String> images)`처럼 "변환이 끝난 값을 파라미터로 받는" 패턴이 존재하므로 (`MovieService.getAdminMovieDetail`에서 `fileService::toImageUrl`로 미리 변환해서 넘겨줌), 이 파라미터 타입만 `String` → `ResolvedImageUrl`로 바꾸면 된다. 서비스 레이어가 `FileService.toImageUrl(key)`를 호출해서 넘겨주는 지금 구조는 그대로 유지되고, Mapper는 인터페이스 그대로 남아 있어도 된다 — MapStruct는 `ResolvedImageUrl` 파라미터를 `ResolvedImageUrl` 필드로 복사만 하면 되기 때문이다. 즉 안 1(매퍼 DI)에서 우려했던 "매퍼 레이어 순수성이 깨진다"는 단점 없이, "FileService를 거치지 않고는 컴파일이 안 된다"는 강제력만 얻을 수 있다. CLAUDE.md의 "모든 entity↔DTO 변환은 Mapper를 거친다"는 기존 컨벤션도 그대로 유지된다.

노출 지점별 통합 방식:
- **Thymeleaf**: `ResolvedImageUrl.toString()`을 오버라이드해두면 `th:src="${movie.thumbnail}"` 템플릿 코드를 한 글자도 안 고쳐도 그대로 동작한다. 커스텀 다이얼렉트 등록 불필요.
- **JSON(`/admin/api/**`)**: getter에 `@JsonValue`만 붙이면 해당 타입이 어디서 쓰이든 자동으로 URL 문자열 하나로 직렬화된다. 커스텀 Jackson 시리얼라이저를 별도로 등록할 필요 없음.
- **QueryDSL 프로젝션**(raw key가 처음 담기는 지점, 예: `AdminMovieJoinRowResponse`): `ImageKey`로 감싼다. `ImageKey`는 생성이 자유로운 타입이므로 프로젝션 생성자에서 그냥 `new ImageKey(...)`로 감싸면 됨.

- 장점: "변환을 깜빡할 수 있는 여지"를 타입 시스템 레벨에서 차단 — 가장 근본적인 해결. 매퍼 레이어 순수성도 깨지지 않음.
- 단점: `MovieListResponse`, `AdminMovieListResponse`, `MovieDetailResponse`, `HomeMovieResponse`, `ReservationCheckoutResponse` 등 이미지 필드를 가진 DTO 전체의 필드 타입을 바꿔야 하고, QueryDSL 프로젝션도 함께 손봐야 함 — 여전히 도입 범위 자체는 작지 않다.
- **평가: 생성자를 잠그는 조건 하에서 가장 견고하고, 기존 아키텍처(Mapper 순수성)와도 충돌하지 않는다. 도입 비용은 안 1/안 2보다 크지만, Thymeleaf/JSON 통합 비용은 처음 검토했을 때(커스텀 다이얼렉트·시리얼라이저)보다 낮아졌다.**

## 결론 (현재 시점 권고)

- 지금 당장은 서비스 레이어에서 개별적으로 변환하는 방식을 유지한다 (이미 검증된 패턴, 낮은 리스크).
- 회귀 방지를 위해, 이미지 URL을 반환하는 각 응답 DTO에 대해 "썸네일/이미지 필드는 항상 base URL로 시작해야 한다"를 검증하는 테스트를 추가하는 것을 권장한다 (별도 작업으로 진행).
- 안 3(raw key / resolved URL 타입 분리, 생성자 잠금)이 가장 근본적이면서도 기존 아키텍처와 충돌하지 않는 해결책이다. 다만 현재는 발견된 누락 지점이 4곳 수준이라 비용 대비 효과를 고려해 보류한다. 유사한 버그가 다시 발생하거나 이미지 노출 지점이 확장될 경우 우선적으로 재검토할 안이다.

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

### 안 3. 전용 타입으로 감싸서 변환을 강제

`String` 대신 raw key를 감싸는 전용 타입(예: `ImageKey`)을 두고, 화면/응답에 노출되는 지점(Thymeleaf 템플릿, JSON 직렬화)에서만 이 타입을 URL 문자열로 변환하도록 강제.

- Thymeleaf: 커스텀 유틸리티/다이얼렉트를 등록해서 템플릿에서 `${imageUrl.resolve(movie.thumbnailKey)}` 형태로만 접근 가능하게 하고, 모델에 raw 문자열을 직접 노출하지 않음.
- JSON(`/admin/api/**`): `ImageKey` 타입에 대한 Jackson 커스텀 시리얼라이저를 등록 — 필드 타입을 `ImageKey`로 선언하면 직렬화 시 자동으로 URL로 바뀌고, `String`으로 잘못 꺼내 쓰는 것 자체가 코드상 불가능해짐.
- 장점: "변환을 깜빡할 수 있는 여지"를 타입 시스템 레벨에서 차단 — 가장 근본적인 해결.
- 단점: 도입 범위가 크다 (Jackson 설정, Thymeleaf 다이얼렉트, 기존 DTO 필드 타입 전면 변경). 이번 버그 규모에 비해 과한 리팩터링일 수 있음.
- **평가: 가장 견고하지만 비용이 큼. 비슷한 종류의 버그가 반복되거나, 이미지 노출 지점이 더 늘어날 경우 재검토.**

## 결론 (현재 시점 권고)

- 지금 당장은 서비스 레이어에서 개별적으로 변환하는 방식을 유지한다 (이미 검증된 패턴, 낮은 리스크).
- 회귀 방지를 위해, 이미지 URL을 반환하는 각 응답 DTO에 대해 "썸네일/이미지 필드는 항상 base URL로 시작해야 한다"를 검증하는 테스트를 추가하는 것을 권장한다 (별도 작업으로 진행).
- 안 3(전용 타입 강제)이 가장 근본적인 해결책이지만, 현재는 발견된 누락 지점이 4곳 수준이라 비용 대비 효과를 고려해 보류한다. 유사한 버그가 다시 발생하거나 이미지 노출 지점이 확장될 경우 재검토 대상으로 남긴다.

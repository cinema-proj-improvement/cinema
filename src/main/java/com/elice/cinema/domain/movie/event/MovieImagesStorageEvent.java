package com.elice.cinema.domain.movie.event;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record MovieImagesStorageEvent(
        Long movieId,
        MultipartFile thumbnailImage,
        List<MultipartFile> extraImages
) {
    public static MovieImagesStorageEvent of(Long movieId, MultipartFile thumbnailImage, List<MultipartFile> extraImages) {
        return new MovieImagesStorageEvent(movieId, thumbnailImage, extraImages);
    }
}

/*
 TODO: MovieImage 테이블에 저장할 땐 originalfileName 컬럼엔 사용자가 올린 파일 이름 저장(필요할 시 - 사용자가 원본 파일명을 알아야 하는 경우),
  실제 로컬에 저장할 땐 파일명을 UUID로 저장(파일명이 겹쳐서 덮어씌어지지 않도록)(PK 개념). 단, s3는 그렇게 안 해줘도 됨 (객체여서 덮어씌어지지 않음)
  업데이트할 땐 고아 파일 방지해야 함
  영화 삭제할 땐 트랜잭션이 영화 DB에만 걸리고(파일에는 안 걸림) 파일엔 안 걸리기 때문에 이거 신경써서 이미지도 같이 삭제되도록 해줘야 함
    -> 영화 삭제 서비스 메서드에서 영화 삭제 후 파일을 삭제해줘야 함. 파일을 먼저 삭제하고 영화 삭제하도록 하면 해당 메서드 실패해서 트랜잭션 롤백될 때
        파일은 트랜잭션 안 타기 때문에 영화는 삭제 안 되고 파일은 삭제되버림. 따라서 이벤트를 통해 이미지 파일 삭제하도록 할 거면 리스너 phase를 after_commit으로?
 */
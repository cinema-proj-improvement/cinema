package com.elice.cinema.domain.movieImage.eventListener;

import com.elice.cinema.domain.movie.event.MovieImagesStorageEvent;
import com.elice.cinema.domain.movieImage.service.MovieImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MovieImagesStorageEventListener {
    private final MovieImageService movieImageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MovieImagesStorageEvent event) {
        movieImageService.storeImages(event.movieId(), event.thumbnailImage(), event.extraImages());
    }
}

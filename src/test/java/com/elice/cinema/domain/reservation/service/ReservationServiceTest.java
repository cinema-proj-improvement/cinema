package com.elice.cinema.domain.reservation.service;

import com.elice.cinema.domain.member.entity.Member;
import com.elice.cinema.domain.member.repository.MemberRepository;
import com.elice.cinema.domain.movie.entity.Movie;
import com.elice.cinema.domain.movie.mapper.MovieMapper;
import com.elice.cinema.domain.movie.repository.MovieRepository;
import com.elice.cinema.domain.policy.service.EnvironmentPolicyService;
import com.elice.cinema.domain.reservation.entity.Reservation;
import com.elice.cinema.domain.reservation.mapper.ReservationMapper;
import com.elice.cinema.domain.reservation.repository.ReservationLockRepository;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.domain.reservation.repository.ReservedSeatRepository;
import com.elice.cinema.domain.screen.entity.Screen;
import com.elice.cinema.domain.screen.entity.Seat;
import com.elice.cinema.domain.screen.repository.SeatRepository;
import com.elice.cinema.domain.screening.entity.Screening;
import com.elice.cinema.domain.screening.mapper.ScreeningMapper;
import com.elice.cinema.domain.screening.repository.ScreeningRepository;
import com.elice.cinema.global.config.properties.SeatHoldProperties;
import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @InjectMocks
    private ReservationService reservationService;

    @Mock private MovieRepository movieRepository;
    @Mock private ScreeningRepository screeningRepository;
    @Mock private ReservedSeatRepository reservedSeatRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationLockRepository reservationLockRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private SeatRepository seatRepository;

    @Mock private EnvironmentPolicyService environmentPolicyService;
    @Mock private SeatHoldProperties seatHoldProperties;
    @Mock private MovieMapper movieMapper;
    @Mock private ScreeningMapper screeningMapper;
    @Mock
    private ReservationMapper reservationMapper;

    @Test
    void holdSeats_seatIdsNullOrEmptyOrExceeded_thenThrow() {
        // given
        given(environmentPolicyService.getMaxReservationCount()).willReturn(2);

        // when & then
        assertThatThrownBy(() -> reservationService.holdSeats(1L, null, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.RESERVATION_SEAT_LIMIT_EXCEEDED.getMessage());

        assertThatThrownBy(() -> reservationService.holdSeats(1L, List.of(), 10L))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> reservationService.holdSeats(1L, List.of(1L,2L,3L), 10L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void holdSeats_whenBlockedSeatExists_thenThrow_andNeverTryRedisLock() {
        // given
        Long screeningId = 1L;
        List<Long> seatIds = List.of(101L, 102L);

        given(environmentPolicyService.getMaxReservationCount()).willReturn(10);
        given(reservedSeatRepository.findBlockedSeatIdsIn(eq(screeningId), eq(seatIds), anyList()))
                .willReturn(List.of(102L)); // blocked

        // when & then
        assertThatThrownBy(() -> reservationService.holdSeats(screeningId, seatIds, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SEAT_ALREADY_HELD));

        then(reservationLockRepository).shouldHaveNoInteractions();
        then(reservationRepository).shouldHaveNoInteractions();
        then(reservedSeatRepository).should(never()).saveAll(any());
    }

    @Test
    void holdSeats_whenRedisLockFails_midway_thenUnlockOnlyLockedSeats_andNoDbSave() {
        // given
        Long screeningId = 1L;
        Long memberId = 99L;
        List<Long> seatIds = List.of(101L, 102L);

        given(environmentPolicyService.getMaxReservationCount()).willReturn(10);
        given(reservedSeatRepository.findBlockedSeatIdsIn(eq(screeningId), eq(seatIds), anyList()))
                .willReturn(List.of());

        given(seatHoldProperties.getMinutes()).willReturn(5);
        given(seatHoldProperties.getRedisGraceMinutes()).willReturn(1);

        // seats
        Seat s1 = mock(Seat.class); given(s1.isActive()).willReturn(true);
        Seat s2 = mock(Seat.class); given(s2.isActive()).willReturn(true);
        given(seatRepository.findAllById(seatIds)).willReturn(List.of(s1, s2));

        // screening/member
        Screening screening = mock(Screening.class);
        given(screeningRepository.findByIdWithMovieAndScreen(screeningId)).willReturn(Optional.of(screening));
        Member member = mock(Member.class);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // redis lock: first ok, second fail
        given(reservationLockRepository.lock(eq(screeningId), eq(101L), eq(memberId), anyLong(), eq(TimeUnit.MINUTES)))
                .willReturn(true);
        given(reservationLockRepository.lock(eq(screeningId), eq(102L), eq(memberId), anyLong(), eq(TimeUnit.MINUTES)))
                .willReturn(false);

        // when
        assertThatThrownBy(() -> reservationService.holdSeats(screeningId, seatIds, memberId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SEAT_ALREADY_HELD));

        // then: DB save X
        then(reservationRepository).should(never()).save(any());
        then(reservedSeatRepository).should(never()).saveAll(any());

        // then: unlock only seat 101
        then(reservationLockRepository).should().unlockAll(eq(screeningId), eq(List.of(101L)));
    }

    @Test
    void holdSeats_success_thenSaveReservationAndReservedSeats_andUnlockAllLockedSeats() {
        // given
        Long screeningId = 1L;
        Long memberId = 99L;
        List<Long> seatIds = List.of(101L, 102L);

        given(environmentPolicyService.getMaxReservationCount()).willReturn(10);
        given(reservedSeatRepository.findBlockedSeatIdsIn(eq(screeningId), eq(seatIds), anyList()))
                .willReturn(List.of());

        given(seatHoldProperties.getMinutes()).willReturn(5);
        given(seatHoldProperties.getRedisGraceMinutes()).willReturn(1);

        Seat s1 = mock(Seat.class); given(s1.isActive()).willReturn(true);
        Seat s2 = mock(Seat.class); given(s2.isActive()).willReturn(true);
        given(seatRepository.findAllById(seatIds)).willReturn(List.of(s1, s2));

        // screening mock + 내부 참조 스텁들
        Screening screening = mock(Screening.class);

        Movie movie = mock(Movie.class);
        given(movie.getTitle()).willReturn("테스트 영화");

        Screen screen = mock(Screen.class);
        given(screen.getName()).willReturn("1관");

        given(screening.getMovie()).willReturn(movie);
        given(screening.getScreen()).willReturn(screen);
        given(screening.getStartAt()).willReturn(LocalDateTime.of(2026, 2, 10, 12, 0));
        given(screening.getEndAt()).willReturn(LocalDateTime.of(2026, 2, 10, 14, 0));

        given(screeningRepository.findByIdWithMovieAndScreen(screeningId))
                .willReturn(Optional.of(screening));

        // member mock + name 스텁
        Member member = mock(Member.class);
        given(member.getName()).willReturn("민성");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // redis lock all ok
        given(reservationLockRepository.lock(eq(screeningId), anyLong(), eq(memberId), anyLong(), eq(TimeUnit.MINUTES)))
                .willReturn(true);

        given(environmentPolicyService.getDefaultPrice()).willReturn(10000);

        // reservation save result
        Reservation saved = mock(Reservation.class);
        given(reservationRepository.save(any())).willReturn(saved);
        given(saved.getId()).willReturn(777L);

        // when
        Long reservationId = reservationService.holdSeats(screeningId, seatIds, memberId);

        // then
        assertThat(reservationId).isEqualTo(777L);
        then(reservationRepository).should().save(any(Reservation.class));
        then(reservedSeatRepository).should().saveAll(anyList());
        then(reservationLockRepository).should().unlockAll(eq(screeningId), eq(seatIds));
    }
}

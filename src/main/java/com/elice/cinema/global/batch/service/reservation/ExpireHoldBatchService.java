package com.elice.cinema.global.batch.service.reservation;

import com.elice.cinema.domain.reservation.dto.SeatLockInfoDto;
import com.elice.cinema.domain.reservation.repository.ReservationLockRepository;
import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.domain.reservation.repository.ReservedSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpireHoldBatchService {
    private final ReservationRepository reservationRepository;
    private final ReservedSeatRepository reservedSeatRepository;

    private final ReservationLockRepository reservationLockRepository;

    @Transactional
    public void expireHolds() {
        LocalDateTime now = LocalDateTime.now();
        int batchSize = 500;

        while(true) {  // FIXME: 만약 반복문 안 코드에서 어떤 문제가 생겼는데 예외를 던지지 않고 탈출조건에도 안 걸리게 된다면? -> 무한 반복이 아니라 시간 단위로 돌아가도록
            // 1) 만료 대상 reservation id들 조회
            List<Long> reservationIds = reservationRepository
                    .findExpiredHoldReservationIds(now, PageRequest.of(0, batchSize));

            if (reservationIds.isEmpty()) break;

            // 2) 좌석 락 해제용 (screeningId, seatId) 조회
            List<SeatLockInfoDto> locks = reservedSeatRepository.findSeatLocksByReservationIds(reservationIds);

            // 3) screeningId별로 좌석들을 묶기 (reservationLockRepository.unlockAll() 쓰기 위한 사전작업)
            Map<Long, List<Long>> seatIdsByScreeningId = locks.stream()
                    .collect(Collectors.groupingBy(
                            SeatLockInfoDto::getScreeningId,
                            Collectors.mapping(SeatLockInfoDto::getSeatId, Collectors.toList())
                    ));

            // 4) redis lock 해제
            for (Map.Entry<Long, List<Long>> e : seatIdsByScreeningId.entrySet()) {
                reservationLockRepository.unlockAll(e.getKey(), e.getValue());
            }

            // 5) DB 정리 (예매 좌석 삭제 + 예매 만료 처리)
            reservedSeatRepository.bulkDeleteHoldSeatsByReservationIds(reservationIds);
            reservationRepository.bulkExpireHoldReservations(reservationIds);
        }
    }
}

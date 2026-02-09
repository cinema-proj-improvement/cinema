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

    private static final int BATCH_SIZE = 500;
    private static final int MAX_BATCHES_PER_RUN = 5;

    @Transactional
    public void expireHolds() {
        LocalDateTime now = LocalDateTime.now();

        for(int i = 0; i < MAX_BATCHES_PER_RUN; i++) {
            int processed = expireHoldsForOneBatch(now);
            if(processed == 0) {
                return;
            }
        }
    }

    protected int expireHoldsForOneBatch(LocalDateTime now) {
        List<Long> reservationIds = reservationRepository
                .findExpiredHoldReservationIds(now, PageRequest.of(0, BATCH_SIZE));

        if(reservationIds.isEmpty()) {
            return 0;
        }

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

        return reservationIds.size();
    }
}

package com.elice.cinema.global.batch.service.reservation;

import com.elice.cinema.domain.reservation.repository.ReservationRepository;
import com.elice.cinema.domain.reservation.repository.ReservedSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpireHoldBatchService {
    private final ReservationRepository reservationRepository;
    private final ReservedSeatRepository reservedSeatRepository;

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
            log.info("HOLD 만료 처리: batch={}, processed={}건", i + 1, processed);
        }
    }

    protected int expireHoldsForOneBatch(LocalDateTime now) {
        List<Long> reservationIds = reservationRepository
                .findExpiredHoldReservationIds(now, PageRequest.of(0, BATCH_SIZE));

        if(reservationIds.isEmpty()) {
            return 0;
        }

        // redis lock은 holdSeats() 처리 직후 이미 해제되어 있으므로(SeatHoldFacade), 여기서는 DB 정리만 하면 됨.
        reservedSeatRepository.bulkDeleteHoldSeatsByReservationIds(reservationIds);
        reservationRepository.bulkExpireHoldReservations(reservationIds);

        return reservationIds.size();
    }
}

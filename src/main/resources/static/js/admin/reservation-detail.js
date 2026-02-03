console.log("reservation-detail.js loaded");

let currentReservationId = null;

/**
 * DOM 헬퍼
 */
function qs(id) {
    return document.getElementById(id);
}

/**
 * 전역 클릭 이벤트 (이벤트 위임)
 */
document.addEventListener('click', (e) => {

    // 예매 취소
    if (e.target.id === 'cancelReservationBtn') {
        console.log('🔥 cancel button clicked');
        cancelReservation();
    }

    // 모달 배경 클릭
    const modal = qs("reservationDetailModal");
    if (modal && !modal.classList.contains("hidden") && e.target === modal) {
        closeReservationDetail();
    }
});

/**
 * 예매 상세 열기
 */
window.openReservationDetail = async function (btn) {
    const reservationId = btn?.dataset?.reservationId;
    if (!reservationId) return;

    currentReservationId = reservationId;
    console.log("fetch reservation detail =", reservationId);

    try {
        const res = await fetch(`/admin/api/reservations/${reservationId}`);
        if (!res.ok) {
            alert("예매 상세 조회 실패");
            return;
        }

        const data = await res.json();
        console.log("detail data =", data);

        /* 날짜 / 시간 */
        let screeningTimeText = "-";
        if (data.screeningDate && data.screeningStartTime) {
            screeningTimeText = `${data.screeningDate} ${data.screeningStartTime}`;
        }

        let reservedAtText = "-";
        if (data.reservedAt) {
            const dt = new Date(data.reservedAt);
            reservedAtText =
                dt.toISOString().slice(0, 10) + " " +
                dt.toTimeString().slice(0, 5);
        }

        /* 기본 정보 */
        qs("detailReservationCode").textContent = data.reservationCode ?? "-";
        qs("detailMemberName").textContent = data.memberName ?? "-";
        qs("detailMemberLoginId").textContent = data.memberLoginId ?? "-";
        qs("detailMovieTitle").textContent = data.movieTitle ?? "-";
        qs("detailScreenName").textContent = data.screenName ?? "-";
        qs("detailScreeningTime").textContent = screeningTimeText;

        /* 좌석 */
        const seatContainer = qs("detailSeats");
        seatContainer.innerHTML = "";

        if (Array.isArray(data.seatCodes) && data.seatCodes.length > 0) {
            data.seatCodes.forEach(code => {
                const badge = document.createElement("span");
                badge.className = "seat-badge";
                badge.textContent = code;
                seatContainer.appendChild(badge);
            });
        } else {
            seatContainer.textContent = "-";
        }

        qs("detailSeatCount").textContent =
            typeof data.seatCount === "number" ? `${data.seatCount}석` : "-";

        /* 결제 */
        qs("detailTotalPrice").textContent =
            typeof data.totalPrice === "number"
                ? data.totalPrice.toLocaleString() + "원"
                : "-";

        qs("detailPaymentStatus").textContent = data.paymentStatus ?? "-";
        qs("detailReservedAt").textContent = reservedAtText;

        /* 상태 */
        const statusEl = qs("detailReservationStatus");
        statusEl.textContent = data.reservationStatus ?? "-";
        statusEl.className = "status-badge";
        if (data.reservationStatus) {
            statusEl.classList.add(data.reservationStatus);
        }

        /* 예매 취소 버튼 */
        const cancelBtn = qs("cancelReservationBtn");
        if (cancelBtn) {
            cancelBtn.classList.toggle("hidden", data.cancelable !== true);
        }

        openModal();

    } catch (e) {
        console.error(e);
        alert("예매 상세 조회 중 오류 발생");
    }
};

/**
 * 예매 취소
 */
function cancelReservation() {
    if (!currentReservationId) return;

    if (!confirm("정말 예매를 취소하시겠습니까?")) return;

    console.log("🚨 cancelReservation called:", currentReservationId);

    fetch(`/admin/api/reservations/${currentReservationId}/cancel`, {
        method: "POST"
    })
        .then(res => {
            if (!res.ok) throw new Error("cancel failed");
            alert("예매가 취소되었습니다.");
            location.reload();
        })
        .catch(err => {
            console.error(err);
            alert("예매 취소 중 오류 발생");
        });
}

/**
 * 모달 제어
 */
function openModal() {
    qs("reservationDetailModal")?.classList.remove("hidden");
}

window.closeReservationDetail = function () {
    qs("reservationDetailModal")?.classList.add("hidden");
};

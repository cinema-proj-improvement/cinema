(function () {
    // ===== 결제수단 선택 UI =====
    const grid = document.getElementById("payMethodGrid");
    const hidden = document.getElementById("paymentMethod");
    const hiddenSubmit = document.getElementById("paymentMethodSubmit"); // 없으면 무시
    const payBtn = document.getElementById("payBtn"); // 없으면 무시

    if (grid) {
        setActive(hidden?.value || "CARD");

        grid.addEventListener("click", (e) => {
            const btn = e.target.closest(".pay-method");
            if (!btn) return;

            const method = btn.dataset.method;
            setActive(method);
        });
    }

    function setActive(method) {
        if (!grid) return;

        const buttons = grid.querySelectorAll(".pay-method");
        buttons.forEach((b) => b.classList.toggle("active", b.dataset.method === method));

        if (hidden) hidden.value = method;
        if (hiddenSubmit) hiddenSubmit.value = method;

        if (payBtn) payBtn.disabled = !method;
    }

    // ===== "이전/뒤로/이탈" 시 HOLD 정리 요청 =====
    const backBtn = document.getElementById("backBtn");
    const reservationId = document.getElementById("reservationId")?.value;

    // reservationId가 없으면 뒤 로직은 실행할 이유 없음
    if (!reservationId) return;

    // (선택) CSRF meta 태그가 있으면 자동으로 헤더에 넣음
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

    function buildHeaders() {
        const headers = {};
        if (csrfTokenMeta && csrfHeaderMeta) {
            headers[csrfHeaderMeta.content] = csrfTokenMeta.content;
        }
        return headers;
    }

    async function cancelHoldViaFetch() {
        try {
            await fetch(`/reservations/${reservationId}/cancel-hold`, {
                method: "POST",
                headers: buildHeaders(),
                // 페이지 이동 직전에도 요청이 끊기지 않게(best-effort)
                keepalive: true
            });
        } catch (e) {
            // 실패해도 이동은 진행 (TTL/배치가 최종 정리)
        }
    }

    function cancelHoldViaBeacon() {
        try {
            // beacon은 헤더를 못 실어서 CSRF 켜면 막힐 수 있음(현재는 off라 OK)
            navigator.sendBeacon(`/reservations/${reservationId}/cancel-hold`);
        } catch (e) {
            // 무시
        }
    }

    // 1) "이전" 버튼 클릭 시: 서버 정리 요청 후 이동
    if (backBtn) {
        backBtn.addEventListener("click", async (e) => {
            e.preventDefault();
            e.stopPropagation();

            await cancelHoldViaFetch();
            window.location.href = "/reservations";
        });
    }

    // 2) 뒤로가기/탭닫기/새로고침 등으로 페이지를 떠날 때: best-effort 정리
    window.addEventListener("pagehide", cancelHoldViaBeacon);

    document.addEventListener("visibilitychange", () => {
        if (document.visibilityState === "hidden") {
            cancelHoldViaBeacon();
        }
    });
})();

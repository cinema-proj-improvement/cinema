(function () {
    const mainEl = document.querySelector(".seat-page");
    if (!mainEl) return;

    const screeningId = Number(mainEl.dataset.screeningId);
    const infoUrl = `/reservations/screenings/${screeningId}/seat-selection/info`;

    const seatGridEl = document.getElementById("seatGrid");
    const btnReset = document.getElementById("btnReset");
    const btnNext = document.getElementById("btnNext");
    const seatHintText = document.getElementById("seatHintText");

    const movieTitleEl = document.getElementById("movieTitle");
    const screenNameEl = document.getElementById("screenName");
    const screeningTypeEl = document.getElementById("screeningType");
    const screeningTimeEl = document.getElementById("screeningTime");

    const selectedSeatsEl = document.getElementById("selectedSeats");
    const selectedCountEl = document.getElementById("selectedCount");
    const defaultPriceEl = document.getElementById("defaultPrice");
    const totalPriceEl = document.getElementById("totalPrice");

    const seatIdInputsEl = document.getElementById("seatIdInputs");
    const holdFormEl = document.getElementById("holdForm");

    /** state */
    let maxReservationCount = 0;
    let defaultPrice = 0;
    let seats = []; // from API
    const selected = new Map(); // seatId -> seatCode

    init();

    async function init() {
        try {
            const data = await fetchInfo();
            applyHeaderInfo(data);
            applyPolicy(data);

            // ✅ 변경 1: seatInfos로 받기
            seats = Array.isArray(data.seatInfos) ? data.seatInfos : [];

            renderSeatGrid(seats);
            renderSelected();
            updateHint();
        } catch (e) {
            console.error(e);
            if (seatHintText) seatHintText.textContent = "좌석 정보를 불러오지 못했습니다. 새로고침 해주세요.";
        }
        bindEvents();
    }

    function bindEvents() {
        if (btnReset) {
            btnReset.addEventListener("click", () => {
                selected.clear();
                renderSeatGrid(seats); // 선택 스타일 제거 위해 재렌더
                renderSelected();
                updateHint();
            });
        }

        if (holdFormEl) {
            holdFormEl.addEventListener("submit", (e) => {
                if (selected.size === 0) {
                    e.preventDefault();
                    alert("좌석을 선택해주세요.");
                    return;
                }
                // seatIds hidden inputs 주입
                syncSeatIdInputs();
            });
        }
    }

    async function fetchInfo() {
        const res = await fetch(infoUrl, { headers: { Accept: "application/json" } });
        if (!res.ok) throw new Error("seat selection info fetch failed");
        return await res.json();
    }

    function applyHeaderInfo(data) {
        // ✅ 변경 2: screeningInfo / screenInfo로 읽기
        const s = data.screeningInfo || {};
        const sc = data.screenInfo || {};

        if (movieTitleEl) movieTitleEl.textContent = s.movieTitle ?? "-";
        if (screenNameEl) screenNameEl.textContent = sc.screenName ?? "-";
        if (screeningTypeEl) screeningTypeEl.textContent = sc.screeningType ?? "-";

        const start = toHHmm(s.startAt);
        const end = toHHmm(s.endAt);
        if (screeningTimeEl) screeningTimeEl.textContent = start && end ? `${start} ~ ${end}` : "-";
    }

    function applyPolicy(data) {
        maxReservationCount = Number(data.maxReservationCount ?? 0);
        defaultPrice = Number(data.defaultPrice ?? 0);

        if (selectedCountEl) selectedCountEl.textContent = `0 / ${maxReservationCount}`;
        if (defaultPriceEl) defaultPriceEl.textContent = formatWon(defaultPrice);
        if (totalPriceEl) totalPriceEl.textContent = formatWon(0);
    }

    function renderSeatGrid(seatItems) {
        if (!seatGridEl) return;
        seatGridEl.innerHTML = "";

        if (!seatItems || seatItems.length === 0) {
            seatGridEl.textContent = "좌석 정보가 없습니다.";
            return;
        }

        const rows = [...new Set(seatItems.map(x => x.rowNo))].sort((a,b)=>a-b);
        const maxCol = Math.max(...seatItems.map(x => x.colNo));

        seatGridEl.style.gridTemplateColumns = `26px repeat(${maxCol}, 30px)`;

        const byPos = new Map();
        for (const it of seatItems) {
            byPos.set(`${it.rowNo}:${it.colNo}`, it);
        }

        for (const r of rows) {
            const label = document.createElement("div");
            label.className = "row-label";
            label.textContent = rowLabel(r);
            seatGridEl.appendChild(label);

            for (let c = 1; c <= maxCol; c++) {
                const key = `${r}:${c}`;
                const seat = byPos.get(key);

                if (!seat) {
                    const spacer = document.createElement("div");
                    spacer.style.width = "30px";
                    spacer.style.height = "30px";
                    seatGridEl.appendChild(spacer);
                    continue;
                }

                const btn = document.createElement("button");
                btn.type = "button";
                btn.className = "seat-btn";
                btn.dataset.seatId = String(seat.seatId);
                btn.dataset.seatCode = seat.seatCode;
                btn.title = seat.seatCode;

                const isSelected = selected.has(seat.seatId);
                const selectable = !!seat.selectable;

                if (!selectable) {
                    btn.classList.add("unavailable");
                    btn.disabled = true;
                    btn.textContent = "×";
                } else {
                    btn.classList.add("available");
                    btn.textContent = String(c);

                    if (isSelected) btn.classList.add("selected");

                    btn.addEventListener("click", () => {
                        toggleSeat(seat.seatId, seat.seatCode);
                        btn.classList.toggle("selected", selected.has(seat.seatId));
                        renderSelected();
                        updateHint();
                    });
                }

                seatGridEl.appendChild(btn);
            }
        }
    }

    function toggleSeat(seatId, seatCode) {
        if (selected.has(seatId)) {
            selected.delete(seatId);
            return;
        }

        if (maxReservationCount > 0 && selected.size >= maxReservationCount) {
            alert(`최대 ${maxReservationCount}좌석까지 선택할 수 있습니다.`);
            return;
        }

        selected.set(seatId, seatCode);
    }

    function renderSelected() {
        if (selectedSeatsEl) selectedSeatsEl.innerHTML = "";

        const seatCodes = [...selected.values()].sort(sortSeatCode);

        for (const code of seatCodes) {
            const chip = document.createElement("div");
            chip.className = "seat-chip";
            chip.textContent = code;
            selectedSeatsEl.appendChild(chip);
        }

        if (selectedCountEl) selectedCountEl.textContent = `${selected.size} / ${maxReservationCount}`;
        if (totalPriceEl) totalPriceEl.textContent = formatWon(selected.size * defaultPrice);

        if (btnNext) btnNext.disabled = (selected.size === 0);
    }

    function syncSeatIdInputs() {
        if (!seatIdInputsEl) return;
        seatIdInputsEl.innerHTML = "";
        for (const seatId of selected.keys()) {
            const input = document.createElement("input");
            input.type = "hidden";
            input.name = "seatIds";
            input.value = String(seatId);
            seatIdInputsEl.appendChild(input);
        }
    }

    function updateHint() {
        if (!seatHintText) return;

        if (selected.size === 0) {
            seatHintText.textContent = `최대 ${maxReservationCount}좌석까지 선택 가능합니다.`;
            return;
        }
        seatHintText.textContent = `${[...selected.values()].sort(sortSeatCode).join(", ")} 선택됨`;
    }

    function toHHmm(v) {
        if (!v) return "";
        const s = String(v);
        const t = s.includes("T") ? s.split("T")[1] : (s.split(" ")[1] || "");
        return t ? t.substring(0, 5) : "";
    }

    function formatWon(n) {
        const num = Number(n || 0);
        return num.toLocaleString("ko-KR") + "원";
    }

    function rowLabel(rowNo) {
        const base = "A".charCodeAt(0);
        const idx = Number(rowNo) - 1;
        if (idx >= 0 && idx < 26) return String.fromCharCode(base + idx);
        return String(rowNo);
    }

    function sortSeatCode(a, b) {
        const pa = parseSeatCode(a);
        const pb = parseSeatCode(b);
        if (pa.row !== pb.row) return pa.row.localeCompare(pb.row);
        return pa.col - pb.col;
    }

    function parseSeatCode(code) {
        const m = String(code).match(/^([A-Za-z]+)(\d+)$/);
        if (!m) return { row: code, col: 0 };
        return { row: m[1], col: Number(m[2]) };
    }
})();

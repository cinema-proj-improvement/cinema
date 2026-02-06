(function () {
    // ====== 설정 ======
    const DAYS = 7;
    const scheduleApiUrl = "/api/reservations/schedule";

    // ====== 엘리먼트 ======
    const movieListEl = document.getElementById("movieList");
    const dateTabsEl = document.getElementById("dateTabs");
    const scheduleListEl = document.getElementById("scheduleList");
    const emptyEl = document.getElementById("empty");

    // (선택) 있으면 쓰고, 없으면 무시
    const loadingEl = document.getElementById("loading");
    const selectedDateTextEl = document.getElementById("selectedDateText");
    const selectedMovieTextEl = document.getElementById("selectedMovieText");

    // ====== 상태 ======
    let selectedDate = toYmd(new Date()); // 기본: 오늘
    let selectedMovieId = null;
    let selectedMovieTitle = "전체";

    // ====== 초기화 ======
    renderDateTabs();
    bindMovieClick();
    updateMeta();
    fetchAndRender();

    // ====== 날짜 탭 렌더 ======
    function renderDateTabs() {
        if (!dateTabsEl) return;
        dateTabsEl.innerHTML = "";

        const today = new Date();
        for (let i = 0; i < DAYS; i++) {
            const d = new Date(today);
            d.setDate(today.getDate() + i);

            const ymd = toYmd(d);
            const label = formatTabLabel(d, i);

            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = "date-tab" + (ymd === selectedDate ? " active" : "");
            btn.dataset.date = ymd;
            btn.innerHTML = `
        <span class="tab-day">${label.day}</span>
        <span class="tab-date">${label.date}</span>
      `;

            btn.addEventListener("click", () => {
                selectedDate = ymd;
                [...dateTabsEl.querySelectorAll(".date-tab")].forEach((x) =>
                    x.classList.remove("active")
                );
                btn.classList.add("active");

                updateMeta();
                fetchAndRender();
            });

            dateTabsEl.appendChild(btn);
        }
    }

    function formatTabLabel(dateObj, index) {
        const daysKo = ["일", "월", "화", "수", "목", "금", "토"];
        const mm = String(dateObj.getMonth() + 1).padStart(2, "0");
        const dd = String(dateObj.getDate()).padStart(2, "0");
        const day = daysKo[dateObj.getDay()];

        if (index === 0) return { day: "오늘", date: `${mm}.${dd} (${day})` };
        return { day, date: `${mm}.${dd}` };
    }

    // ====== 영화 클릭 바인딩 ======
    function bindMovieClick() {
        if (!movieListEl) return;

        movieListEl.addEventListener("click", (e) => {
            const btn = e.target.closest(".movie-btn");
            if (!btn) return;

            const item = btn.closest(".movie-item");
            if (!item) return;

            const movieId = item.dataset.movieId ? Number(item.dataset.movieId) : null;
            const titleEl = item.querySelector(".movie-title");
            const title = titleEl ? titleEl.textContent.trim() : "선택됨";

            // 토글: 같은 영화 다시 누르면 전체로
            if (selectedMovieId === movieId) {
                selectedMovieId = null;
                selectedMovieTitle = "전체";
            } else {
                selectedMovieId = movieId;
                selectedMovieTitle = title;
            }

            [...movieListEl.querySelectorAll(".movie-item")].forEach((li) =>
                li.classList.remove("active")
            );
            if (selectedMovieId !== null) item.classList.add("active");

            updateMeta();
            fetchAndRender();
        });
    }

    // ====== 메타 표시 (요소 없으면 그냥 스킵) ======
    function updateMeta() {
        if (selectedDateTextEl) selectedDateTextEl.textContent = selectedDate;
        if (selectedMovieTextEl) selectedMovieTextEl.textContent = selectedMovieTitle;
    }

    // ====== 데이터 로딩 + 렌더 ======
    async function fetchAndRender() {
        setLoading(true);
        setEmpty(false);
        if (scheduleListEl) scheduleListEl.innerHTML = "";

        try {
            const url = buildScheduleUrl();
            const res = await fetch(url, { headers: { Accept: "application/json" } });

            if (!res.ok) throw new Error("시간표 조회 실패");

            const data = await res.json();

            if (!Array.isArray(data) || data.length === 0) {
                setEmpty(true);
                return;
            }

            renderScheduleList(data);
        } catch (err) {
            setEmpty(true);
            console.error(err);
        } finally {
            setLoading(false);
        }
    }

    function buildScheduleUrl() {
        const params = new URLSearchParams();
        params.set("date", selectedDate);
        if (selectedMovieId !== null) params.set("movieId", String(selectedMovieId));
        return scheduleApiUrl + "?" + params.toString();
    }

    /**
     * ✅ 백엔드 응답 구조에 맞춘 렌더
     * [
     *   {
     *     id(or Id),
     *     startAt, endAt,
     *     movie: { title },
     *     screen: { name },
     *     remainingSeats
     *   }
     * ]
     */
    function renderScheduleList(items) {
        if (!scheduleListEl) return;

        scheduleListEl.innerHTML = "";
        items.forEach((it) => {
            const li = document.createElement("li");
            li.className = "schedule-item";

            const screeningId = it.screeningId ?? it.id ?? it.Id;
            const start = toHHmm(it.startAt);
            const end = toHHmm(it.endAt);

            const title = it.movie?.title ?? "";
            const screenName = it.screen?.name ?? "";
            const screeningType = it.screen?.screeningTypeDisplayName ?? "";
            const totalSeats = it.screen?.totalSeats ?? null;
            const remaining = it.remainingSeats ?? null;

            const seatsText =
                remaining !== null && totalSeats !== null
                    ? `잔여 ${remaining}/${totalSeats}`
                    : remaining !== null
                        ? `잔여 ${remaining}`
                        : "";

            li.innerHTML = `
      <button type="button" class="schedule-btn" data-screening-id="${screeningId ?? ""}">
        <div class="time">
          <span class="time-main">${escapeHtml(start)}</span>
          <span class="time-sub">~ ${escapeHtml(end)}</span>
        </div>

        <div class="info">
          <div class="line1">
            <span class="movie">${escapeHtml(title)}</span>
            ${
                screeningType
                    ? `<span class="pill">${escapeHtml(screeningType)}</span>`
                    : ""
            }
          </div>
          <div class="line2">
            <span class="screen">${escapeHtml(screenName)}</span>
            ${seatsText ? `<span class="seats">${escapeHtml(seatsText)}</span>` : ""}
          </div>
        </div>
      </button>
    `;

            li.querySelector(".schedule-btn").addEventListener("click", () => {
                if (!screeningId) return;
                window.location.href = `/reservations/screenings/${screeningId}/seat-selection`;
            });

            scheduleListEl.appendChild(li);
        });
    }

    function setLoading(on) {
        if (!loadingEl) return;
        loadingEl.hidden = !on;
    }

    function setEmpty(on) {
        if (!emptyEl) return;
        emptyEl.hidden = !on;
    }

    // ====== 유틸 ======
    function toYmd(dateObj) {
        const y = dateObj.getFullYear();
        const m = String(dateObj.getMonth() + 1).padStart(2, "0");
        const d = String(dateObj.getDate()).padStart(2, "0");
        return `${y}-${m}-${d}`;
    }

    function toHHmm(isoOrDateTime) {
        if (!isoOrDateTime) return "";
        const s = String(isoOrDateTime);
        const t = s.includes("T") ? s.split("T")[1] : s.split(" ")[1];
        if (!t) return "";
        return t.substring(0, 5);
    }

    function escapeHtml(str) {
        return String(str)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }
})();

/**
 * 관리자 영화 목록
 * - submit은 적용 버튼으로만
 * - JS는 reset만 담당
 */

/* ===============================
   Reset 버튼
================================ */
const resetBtn = document.getElementById("resetBtn");

if (resetBtn) {
    resetBtn.addEventListener("click", () => {
        window.location.href = "/admin/movies";
    });
}

/* ===============================
   Submit 시 hidden input 세팅
================================ */
const filterForm = document.getElementById("filterForm");

if (filterForm) {
    filterForm.addEventListener("submit", () => {
        document.getElementById("statuses").value =
            getCheckedValues(".status-chk");

        document.getElementById("genres").value =
            getCheckedValues(".genre-chk");

        document.getElementById("ageRatings").value =
            getCheckedValues(".ageRating-chk");
    });
}

/* ===============================
   공통 유틸
================================ */
function getCheckedValues(selector) {
    return [...document.querySelectorAll(`${selector}:checked`)]
        .map(e => e.value)
        .join(",");
}

/* ===============================
   체크 상태 복원 (서버 기준)
================================ */
function restoreCheckedState() {
    const selectedStatuses = getMetaValues("statuses");
    const selectedGenres = getMetaValues("genres");
    const selectedAgeRatings = getMetaValues("ageRatings");

    restoreGroup(".status-chk", selectedStatuses);
    restoreGroup(".genre-chk", selectedGenres);
    restoreGroup(".ageRating-chk", selectedAgeRatings);
}

function getMetaValues(name) {
    const content = document.querySelector(`meta[name='${name}']`)?.content;
    return content ? content.split(",") : [];
}

function restoreGroup(selector, selectedValues) {
    document.querySelectorAll(selector).forEach(cb => {
        if (selectedValues.includes(cb.value)) {
            cb.checked = true;
            cb.closest(".pill")?.classList.add("selected");
        }
    });
}

/* ===============================
   체크 클릭 시 즉시 색상 토글
================================ */
function bindPillToggle(selector) {
    document.querySelectorAll(selector).forEach(cb => {
        cb.addEventListener("change", () => {
            const pill = cb.closest(".pill");
            if (!pill) return;

            pill.classList.toggle("selected", cb.checked);
        });
    });
}

/* ===============================
   초기 실행
================================ */
document.addEventListener("DOMContentLoaded", () => {
    // 1️⃣ 서버 기준 체크 상태 복원
    restoreCheckedState();

    // 2️⃣ 클릭 즉시 색상 반영
    bindPillToggle(".status-chk");
    bindPillToggle(".genre-chk");
    bindPillToggle(".ageRating-chk");
});
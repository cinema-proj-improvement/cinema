/**
 * 관리자 영화 목록
 * - submit은 적용 버튼으로만
 * - JS는 reset만 담당
 */

const resetBtn = document.getElementById("resetBtn");

if (resetBtn) {
    resetBtn.addEventListener("click", () => {
        window.location.href = "/admin/movies";
    });
}

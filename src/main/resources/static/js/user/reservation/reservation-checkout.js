(function () {
    const grid = document.getElementById("payMethodGrid");
    const hidden = document.getElementById("paymentMethod");
    const hiddenSubmit = document.getElementById("paymentMethodSubmit");
    const payBtn = document.getElementById("payBtn");

    if (!grid) return;

    // 기본값
    setActive("CARD");

    grid.addEventListener("click", (e) => {
        const btn = e.target.closest(".pay-method");
        if (!btn) return;

        const method = btn.dataset.method;
        setActive(method);
    });

    function setActive(method) {
        const buttons = grid.querySelectorAll(".pay-method");
        buttons.forEach(b => b.classList.toggle("active", b.dataset.method === method));

        if (hidden) hidden.value = method;
        if (hiddenSubmit) hiddenSubmit.value = method;

        // 결제수단이 반드시 선택되어야만 결제 가능하게 하고 싶으면
        if (payBtn) payBtn.disabled = !method;
    }
})();

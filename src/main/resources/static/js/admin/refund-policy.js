// ===== 생성 모달 =====
const modal = document.getElementById("createModal");

document.getElementById("openCreateModal").onclick = () => {
    modal.style.display = "block";
};

document.getElementById("closeCreateModal").onclick = () => {
    modal.style.display = "none";
};

document.getElementById("createBtn").onclick = async () => {
    const data = {
        name: document.getElementById("nameInput").value,
        beforeStartMinutes: document.getElementById("beforeInput").value,
        refundRate: document.getElementById("rateInput").value
    };

    const res = await fetch("/admin/api/policies/refunds", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    });

    if (res.ok) {
        location.reload();
    }
};

// ===== inline 수정 =====
document.querySelectorAll("tr[data-id]").forEach(row => {
    const editBtn = row.querySelector(".edit-btn");
    const saveBtn = row.querySelector(".save-btn");
    const beforeInput = row.querySelector(".before-input");
    const rateInput = row.querySelector(".rate-input");
    const policyId = row.dataset.id;

    editBtn.onclick = () => {
        beforeInput.disabled = false;
        rateInput.disabled = false;
        editBtn.style.display = "none";
        saveBtn.style.display = "inline-block";
    };

    saveBtn.onclick = async () => {
        const data = {
            beforeStartMinutes: beforeInput.value,
            refundRate: rateInput.value
        };

        const res = await fetch(`/admin/api/policies/refunds/${policyId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data)
        });

        if (res.ok) {
            location.reload();
        }
    };
});

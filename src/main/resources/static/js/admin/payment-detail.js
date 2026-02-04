document.addEventListener('DOMContentLoaded', () => {
    const cancelBtn = document.getElementById('cancelPaymentBtn');

    if (!cancelBtn || cancelBtn.disabled) return;

    cancelBtn.addEventListener('click', async () => {
        const ok = confirm('결제를 취소하시겠습니까?\n이미 취소된 결제는 변경되지 않습니다.');

        if (!ok) return;

        try {
            const paymentId = getPaymentIdFromPage();

            const res = await fetch(`/admin/api/payments/${paymentId}/cancel`, {
                method: 'POST'
            });

            if (!res.ok) {
                throw new Error('결제 취소 실패');
            }

            alert('결제 취소가 완료되었습니다.');
            location.reload();

        } catch (e) {
            alert('결제 취소 중 오류가 발생했습니다.');
            console.error(e);
        }
    });
});

function getPaymentIdFromPage() {
    return document
        .querySelector('.page-header')
        .dataset.paymentId;
}


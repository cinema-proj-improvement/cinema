document.addEventListener("DOMContentLoaded", async () => {
    const payBtn = document.getElementById("payment-button");

    // ✅ Thymeleaf 값을 data-*로 HTML에 심고 여기서 읽는 방식이 CSP에 가장 안전
    const root = document.getElementById("checkout-root");
    const clientKey = root.dataset.clientKey;
    const orderId = root.dataset.orderId;
    const orderName = root.dataset.orderName;
    const amount = Number(root.dataset.amount);
    const customerKey = root.dataset.customerKey;
    const customerEmail = root.dataset.customerEmail || undefined;
    const customerName = root.dataset.customerName || undefined;

    console.log({ clientKey, orderId, orderName, amount, customerKey });

    const tossPayments = TossPayments(clientKey);
    const widgets = tossPayments.widgets({ customerKey });

    await widgets.setAmount({ currency: "KRW", value: amount });

    await Promise.all([
        widgets.renderPaymentMethods({ selector: "#payment-method", variantKey: "DEFAULT" }),
        widgets.renderAgreement({ selector: "#agreement", variantKey: "AGREEMENT" }),
    ]);

    payBtn.addEventListener("click", async (e) => {


        try {
            await widgets.requestPayment({
                orderId,
                orderName,
                successUrl: window.location.origin + "/payments/success",
                failUrl: window.location.origin + "/payments/fail",
                ...(customerEmail ? { customerEmail } : {}),
                ...(customerName ? { customerName } : {}),
            });
            console.log("✅ requestPayment resolved (usually redirects)");
        } catch (err) {
            console.error("❌ requestPayment error:", err);
            alert("결제 요청 실패: 콘솔을 확인하세요");
        }
    });
});

package com.elice.cinema.global.error;

import com.elice.cinema.global.error.exception.BusinessException;
import com.elice.cinema.global.error.exception.PaymentFailRedirectException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     * - 의도된 실패 (결제 불가, 상태 전이 불가 등)
     */
    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(
            BusinessException ex,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ErrorCode errorCode = ex.getErrorCode();

        log.warn("[Business Error] code={} message={} path={}",
                errorCode.getCode(),
                errorCode.getMessage(),
                request.getRequestURI()
        );

        // ✅ [추가] HTTP 상태 코드 명시
        response.setStatus(errorCode.getStatus().value());

        model.addAttribute("code", errorCode.getCode());
        model.addAttribute("message", errorCode.getMessage());
        model.addAttribute("status", errorCode.getStatus().value());
        model.addAttribute("path", request.getRequestURI());

        return "error/custom-error";
    }

    /**
     * 시스템 예외 처리
     * - 예상하지 못한 서버 에러
     */
    @ExceptionHandler(Exception.class)
    public String handleException(
            Exception ex,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.error("[System Error] path={}", request.getRequestURI(), ex);

        // ✅ [추가] HTTP 500 명시
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

        model.addAttribute("code", "E500");
        model.addAttribute("message", "서버 에러가 발생했습니다.");
        model.addAttribute("status", 500);
        model.addAttribute("path", request.getRequestURI());

        return "error/custom-error";
    }

    @ExceptionHandler(PaymentFailRedirectException.class)
    public String handlePaymentFail(
            PaymentFailRedirectException ex,
            RedirectAttributes ra
    ) {
        ra.addAttribute("message", ex.getErrorCode().getMessage());
        ra.addAttribute("orderId", ex.getOrderId());

        return "redirect:/payments/fail";
    }
}
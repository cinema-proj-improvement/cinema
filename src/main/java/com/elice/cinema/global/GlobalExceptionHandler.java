package com.elice.cinema.global;

import com.elice.cinema.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException ex, Model model) {

        log.warn("View BusinessException: {}", ex.getErrorCode().getMessage());

        model.addAttribute("message", ex.getErrorCode().getMessage());
        model.addAttribute("status", ex.getErrorCode().getStatus());

        return "error/custom-error"; // TODO: templates/error/custom-error.html 작성
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {

        log.error("View Exception: ", ex);

        model.addAttribute("message", "서버 에러가 발생했습니다.");
        model.addAttribute("status", 500);

        return "error/custom-error";
    }
}

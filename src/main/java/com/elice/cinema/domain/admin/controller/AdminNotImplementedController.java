package com.elice.cinema.domain.admin.controller;

import com.elice.cinema.global.error.ErrorCode;
import com.elice.cinema.global.error.exception.BusinessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminNotImplementedController {

    @GetMapping("/members")
    public void members() {
        throw new BusinessException(ErrorCode.ADMIN_FEATURE_NOT_IMPLEMENTED);
    }

    @GetMapping("/reviews")
    public void reviews() {
        throw new BusinessException(ErrorCode.ADMIN_FEATURE_NOT_IMPLEMENTED);
    }

/*    @GetMapping("/policies")
    public void policies() {
        throw new BusinessException(ErrorCode.ADMIN_FEATURE_NOT_IMPLEMENTED);
    }*/
}

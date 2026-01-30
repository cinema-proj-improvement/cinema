package com.elice.cinema.domain.auth.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SignupRequest {

    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "올바른 이메일 형식이 아닙니다."
    )
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*\\d)(?=.*[!@#])[A-Za-z\\d!@#]{8,}$",
            message = "비밀번호는 8자 이상, 숫자 1개 이상, 특수문자(!,@,#) 1개 이상 포함해야 하며 그 외 문자는 사용할 수 없습니다."
    )
    private String password;

    @NotBlank
    private String passwordConfirm;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50)
    private String name;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(max = 50)
    private String nickname;

    @NotNull
    @Past
    private LocalDate birthDate;
}

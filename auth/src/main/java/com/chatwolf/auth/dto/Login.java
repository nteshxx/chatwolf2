package com.chatwolf.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Login {

    @NotBlank(message = "email cannot be blank")
    @Size(max = 60, message = "email must not exceed 60 characters")
    @Email(message = "invalid email")
    private String email;

    @NotBlank(message = "password cannot be blank")
    private String password;
}

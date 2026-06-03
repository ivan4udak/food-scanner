package com.foodscanner.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Слой: api
 * DTO входящего запроса. Не является доменным объектом.
 * Валидация на границе HTTP — до попадания в use case.
 */
public class RegisterContributorRequest {

    @NotBlank(message = "Nickname must not be blank")
    @Size(min = 2, max = 100, message = "Nickname must be between 2 and 100 characters")
    private String nickname;

    public RegisterContributorRequest() {}
    public RegisterContributorRequest(String nickname) { this.nickname = nickname; }

    public String getNickname()              { return nickname; }
    public void   setNickname(String value)  { this.nickname = value; }
}

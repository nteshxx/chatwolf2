package com.chatwolf.auth.dto;

import com.chatwolf.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserAndToken {

    private User user;

    private Token token;
}

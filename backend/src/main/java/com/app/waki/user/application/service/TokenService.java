package com.app.waki.user.application.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface TokenService {
    String generateToken(UserDetails userDetails);
}

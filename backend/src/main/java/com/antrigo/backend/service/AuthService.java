package com.antrigo.backend.service;

import com.antrigo.backend.domain.entity.User;
import com.antrigo.backend.dto.request.LoginRequest;
import com.antrigo.backend.dto.request.RegisterStaffRequest;
import com.antrigo.backend.dto.response.JwtResponse;
import com.antrigo.backend.exception.BadCredentialsExceptionCustom;
import com.antrigo.backend.exception.DuplicateResourceException;
import com.antrigo.backend.repository.UserRepository;
import com.antrigo.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public JwtResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndActiveTrue(request.email())
                .orElseThrow(() -> new BadCredentialsExceptionCustom("Email atau password salah"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsExceptionCustom("Email atau password salah");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return JwtResponse.of(token, user.getName(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public JwtResponse registerStaff(RegisterStaffRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email sudah terdaftar");
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return JwtResponse.of(token, user.getName(), user.getEmail(), user.getRole().name());
    }
}

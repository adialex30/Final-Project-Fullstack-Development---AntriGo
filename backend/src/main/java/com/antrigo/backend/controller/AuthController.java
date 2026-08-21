package com.antrigo.backend.controller;

import com.antrigo.backend.dto.request.LoginRequest;
import com.antrigo.backend.dto.request.RegisterStaffRequest;
import com.antrigo.backend.dto.response.JwtResponse;
import com.antrigo.backend.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Login admin/staff & registrasi staff baru")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public JwtResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public JwtResponse register(@Valid @RequestBody RegisterStaffRequest request) {
        return authService.registerStaff(request);
    }
}

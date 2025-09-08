// src/main/java/com/polarisoffice/secuone/api/AdminAuthController.java
package com.polarisoffice.secuone.api;

import com.polarisoffice.secuone.domain.AdminUserEntity;
import com.polarisoffice.secuone.domain.CustomerUserEntity;
import com.polarisoffice.secuone.repository.AdminUserRepository;
import com.polarisoffice.secuone.repository.CustomerUserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

  private static final String ADMIN_SESSION_KEY = "AUTH_ADMIN_ID";

  private final AdminUserRepository adminRepo;
  private final CustomerUserRepository customerRepo;
  private final PasswordEncoder encoder;

  @Value("${admin.signup.secret:}")
  private String signupSecret;

  public AdminAuthController(
      AdminUserRepository adminRepo,
      CustomerUserRepository customerRepo,
      PasswordEncoder encoder
  ) {
    this.adminRepo = adminRepo;
    this.customerRepo = customerRepo;
    this.encoder = encoder;
  }

  // DTOs
  public record SignupReq(@NotBlank String username, @NotBlank String password, String role, String secret) {}
  public record LoginReq(@NotBlank String username, @NotBlank String password) {}
  public record AuthRes(Long id, String username, String role) {}
  public record CustomerLoginReq(@NotBlank String username, @NotBlank String password, @NotBlank String customerCode) {}

  // ----- 관리자 회원가입
  @PostMapping("/signup")
  public ResponseEntity<?> signup(@Valid @RequestBody SignupReq in) {
    boolean hasAny = adminRepo.count() > 0;
    if (hasAny && signupSecret != null && !signupSecret.isBlank()) {
      if (in.secret() == null || !signupSecret.equals(in.secret())) {
        return ResponseEntity.status(401).body(Map.of("message", "가입 시크릿이 올바르지 않습니다."));
      }
    }
    if (adminRepo.existsByUsername(in.username())) {
      return ResponseEntity.status(409).body(Map.of("message", "이미 존재하는 아이디입니다."));
    }

    String role = Optional.ofNullable(in.role()).filter(s -> !s.isBlank()).orElse("EDITOR");
    if (!hasAny) role = "ADMIN";

    AdminUserEntity u = new AdminUserEntity();
    u.setUsername(in.username());
    u.setPasswordHash(encoder.encode(in.password()));
    u.setRole(role);
    u.setIsActive(true);
    u.setCreatedAt(Instant.now());
    u = adminRepo.save(u);

    return ResponseEntity.ok(new AuthRes(u.getId(), u.getUsername(), u.getRole()));
  }

  // ----- 관리자 로그인 (세션 + null-safe 응답)
  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginReq in, HttpSession session) {
    Optional<AdminUserEntity> opt = adminRepo.findByUsernameAndIsActiveTrue(in.username());
    if (opt.isEmpty() || !encoder.matches(in.password(), opt.get().getPasswordHash())) {
      return ResponseEntity.status(401).body(Map.of("message", "아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    AdminUserEntity u = opt.get();
    u.setLastLoginAt(Instant.now());
    adminRepo.save(u);

    session.setAttribute(ADMIN_SESSION_KEY, u.getId());

    Map<String, Object> user = Map.of(
        "id", u.getId(),
        "username", u.getUsername(),
        "role", u.getRole()
    );

    // ⚠️ Map.of 는 null 금지 → 가변 Map 로 응답 구성
    Map<String, Object> resp = new HashMap<>();
    resp.put("type", "admin");
    resp.put("user", user);
    // token 없으면 추가하지 않음 (login.js는 token 없어도 동작)
    return ResponseEntity.ok(resp);
  }

  // ----- 관리자 me (세션 기반)
  @GetMapping("/me")
  public ResponseEntity<?> me(HttpSession session) {
    Long id = (Long) session.getAttribute(ADMIN_SESSION_KEY);
    if (id == null) return ResponseEntity.status(401).build();
    var u = adminRepo.findById(id).orElse(null);
    if (u == null || !Boolean.TRUE.equals(u.getIsActive())) return ResponseEntity.status(401).build();
    return ResponseEntity.ok(Map.of("id", u.getId(), "username", u.getUsername(), "role", u.getRole()));
  }

  // ----- 관리자 로그아웃
  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpSession session) {
    session.invalidate();
    return ResponseEntity.ok(Map.of("ok", true));
  }

  // ----- (옵션) 고객사 가장/대리 로그인 (null-safe 응답)
  @PostMapping("/customer-login")
  public ResponseEntity<?> customerLogin(@Valid @RequestBody CustomerLoginReq in) {
    final String username = in.username().trim().toLowerCase();
    Optional<CustomerUserEntity> opt = customerRepo.findByCustomer_CodeAndUsername(in.customerCode(), username);

    if (opt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    CustomerUserEntity u = opt.get();
    if (!Boolean.TRUE.equals(u.getIsActive())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "비활성화된 계정입니다."));
    }
    if (u.getPasswordHash() == null || !encoder.matches(in.password(), u.getPasswordHash())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    u.setLastLoginAt(Instant.now());
    customerRepo.save(u);

    Map<String, Object> user = Map.of(
        "username", u.getUsername(),
        "role", u.getRole(),
        "customerCode", u.getCustomer().getCode()
    );
    Map<String, Object> resp = new HashMap<>();
    resp.put("user", user);
    return ResponseEntity.ok(resp);
  }
}

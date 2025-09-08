package com.polarisoffice.secuone.api;

import com.polarisoffice.secuone.domain.AdminUserEntity;
import com.polarisoffice.secuone.domain.CustomerUserEntity;
import com.polarisoffice.secuone.repository.AdminUserRepository;
import com.polarisoffice.secuone.repository.CustomerUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AdminUserRepository adminRepo;
  private final CustomerUserRepository customerRepo;
  private final PasswordEncoder encoder;

  public AuthController(AdminUserRepository adminRepo,
                        CustomerUserRepository customerRepo,
                        PasswordEncoder encoder) {
    this.adminRepo = adminRepo;
    this.customerRepo = customerRepo;
    this.encoder = encoder;
  }

  // ===== DTOs (record 대신 일반 클래스) =====
  public static class LoginReq {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private String type;          // "admin" | "customer"
    private String customerCode;  // 고객사 로그인 시 필요

    public LoginReq() {}
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
  }

  public static class UserDto {
    private Long id;
    private String username;
    private String role;
    private String customerCode;

    public UserDto() {}
    public UserDto(Long id, String username, String role, String customerCode) {
      this.id = id; this.username = username; this.role = role; this.customerCode = customerCode;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getCustomerCode() { return customerCode; }
  }

  public static class LoginRes {
    private String type; // "admin" | "customer"
    private UserDto user;
    private String token;

    public LoginRes() {}
    public LoginRes(String type, UserDto user, String token) {
      this.type = type; this.user = user; this.token = token;
    }
    public String getType() { return type; }
    public UserDto getUser() { return user; }
    public String getToken() { return token; }
  }
  // =======================================

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginReq in) {
    final String type = Optional.ofNullable(in.getType()).orElse("admin").toLowerCase();

    if ("customer".equals(type)) {
      return customerLogin(in);
    } else {
      return adminLogin(in);
    }
  }

  private ResponseEntity<?> adminLogin(LoginReq in) {
    Optional<AdminUserEntity> uOpt = adminRepo.findActiveByUsername(in.getUsername());
    if (uOpt.isEmpty() || !encoder.matches(in.getPassword(), uOpt.get().getPasswordHash())) {
      return ResponseEntity.status(401).body(Map.of("message", "아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    AdminUserEntity u = uOpt.get();
    u.setLastLoginAt(Instant.now());
    adminRepo.save(u);

    return ResponseEntity.ok(new LoginRes(
        "admin",
        new UserDto(u.getId(), u.getUsername(), u.getRole(), null),
        null // JWT 쓰면 여기서 발급
    ));
  }

  private ResponseEntity<?> customerLogin(LoginReq in) {
      final String cc   = Optional.ofNullable(in.customerCode).orElse("").trim();
      final String user = Optional.ofNullable(in.username).orElse("").trim();
      if (cc.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("message","customerCode가 필요합니다."));
      }

      var uOpt = customerRepo.findByCustomerCodeIgnoreCaseAndUsernameIgnoreCaseAndIsActiveTrue(cc, user);

      if (uOpt.isEmpty()) {
        return ResponseEntity.status(401).body(Map.of("message","아이디 또는 비밀번호가 올바르지 않습니다."));
      }

      var u = uOpt.get();
      if (!encoder.matches(in.password, u.getPasswordHash())) {
        return ResponseEntity.status(401).body(Map.of("message","아이디 또는 비밀번호가 올바르지 않습니다."));
      }

      u.setLastLoginAt(Instant.now());
      customerRepo.save(u);

      return ResponseEntity.ok(new LoginRes(
          "customer",
          new UserDto(u.getId(), u.getUsername(), u.getRole(), u.getCustomer().getCode()),
          null
      ));
    }

}

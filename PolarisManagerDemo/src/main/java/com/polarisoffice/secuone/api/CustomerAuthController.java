// src/main/java/com/polarisoffice/secuone/api/CustomerAuthController.java
package com.polarisoffice.secuone.api;

import com.polarisoffice.secuone.domain.CustomerContactEntity;
import com.polarisoffice.secuone.domain.CustomerEntity;
import com.polarisoffice.secuone.domain.CustomerUserEntity;
import com.polarisoffice.secuone.repository.CustomerContactRepository;
import com.polarisoffice.secuone.repository.CustomerUserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

//com.polarisoffice.secuone.api.CustomerAuthController
@RestController
@RequestMapping("/api/customer/auth")
public class CustomerAuthController {

private static final String SESSION_KEY = "AUTH_CUSTOMER_ID";

private final CustomerUserRepository users;
private final CustomerContactRepository contacts;
private final PasswordEncoder encoder;

public CustomerAuthController(CustomerUserRepository users,
                             CustomerContactRepository contacts,
                             PasswordEncoder encoder) {
 this.users = users;
 this.contacts = contacts;
 this.encoder = encoder;
}

public record LoginReq(@NotBlank String customerCode,
                      @NotBlank String username,
                      @NotBlank String password) {}
public record LoginRes(Map<String,Object> user, String token) {}
public record ChangePasswordReq(@NotBlank String currentPassword,
                               @NotBlank @Size(min = 8, max = 72) String newPassword) {}

/** 로그인 */
@PostMapping("/login")
@Transactional
public ResponseEntity<?> login(@Valid @RequestBody LoginReq req, HttpSession session) {
 var userOpt = users.findByCustomer_CodeAndUsernameAndIsActiveTrue(
     req.customerCode(), req.username().trim().toLowerCase());

 if (userOpt.isEmpty() || !encoder.matches(req.password(), userOpt.get().getPasswordHash())) {
   return ResponseEntity.status(401).body(Map.of("message", "아이디 또는 비밀번호가 올바르지 않습니다."));
 }

 var user = userOpt.get();
 user.setLastLoginAt(Instant.now());
 users.save(user);

 session.setAttribute(SESSION_KEY, user.getId());
 return ResponseEntity.ok(new LoginRes(userMapForNav(user), null));
}

/** 내 정보(me) – Lazy 문제 방지 위해 트랜잭션 & EntityGraph 사용 */
@GetMapping("/me")
@Transactional(readOnly = true)
public ResponseEntity<?> me(
    HttpSession session,
    @RequestParam(required = false) String customerCode,
    @RequestParam(required = false) String username
) {
  Long id = (Long) session.getAttribute(SESSION_KEY);

  CustomerUserEntity user = null;
  if (id != null) {
    user = users.findById(id).orElse(null); // @EntityGraph로 customer 즉시 로딩
  }

  // ✅ 세션이 없으면 쿼리 파라미터로 폴백
  if (user == null && customerCode != null && !customerCode.isBlank()
      && username != null && !username.isBlank()) {
    user = users
        .findByCustomer_CodeAndUsernameAndIsActiveTrue(customerCode, username)
        .orElse(null);
    if (user == null) {
      // 대소문자 무시 폴백
      user = users
          .findByCustomerCodeIgnoreCaseAndUsernameIgnoreCaseAndIsActiveTrue(customerCode, username)
          .orElse(null);
    }
  }

  if (user == null || Boolean.FALSE.equals(user.getIsActive())) {
    return ResponseEntity.status(401).build();
  }
  return ResponseEntity.ok(enrichedProfile(user));
}
private Map<String,Object> enrichedProfile(CustomerUserEntity u) {
    CustomerEntity cust = u.getCustomer();
    String code = cust != null ? cust.getCode() : null;
    String cname = cust != null ? cust.getName() : null;

    String baseEmail = u.getUsername();
    CustomerContactEntity contact = contacts
        .findFirstByCustomer_CodeAndEmailIgnoreCase(code, baseEmail)
        .orElseGet(() -> contacts
            .findFirstByCustomer_CodeAndIsPrimaryTrueOrderByIdAsc(code)
            .orElse(null));

    Long contactId = contact != null ? contact.getId() : null;
    String name  = contact != null ? contact.getName()  : null;
    String phone = contact != null ? contact.getPhone() : null;
    String email = (contact != null && contact.getEmail() != null) ? contact.getEmail() : baseEmail;

    return Map.of(
        "id", u.getId(),
        "type", "customer",
        "username", u.getUsername(),
        "email", email,
        "name", name,
        "phone", phone,
        "customerCode", code,
        "customerName", cname,
        "role", u.getRole(),
        "contactId", contactId           // 👈 추가
    );
}

/** 로그아웃 */
@PostMapping("/logout")
public ResponseEntity<?> logout(HttpSession session) {
 session.invalidate();
 return ResponseEntity.ok(Map.of("ok", true));
}

/** 비밀번호 변경 */
@PostMapping("/change-password")
@Transactional
public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordReq req,
                                       HttpSession session) {
 Long id = (Long) session.getAttribute(SESSION_KEY);
 if (id == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));

 var user = users.findById(id).orElse(null);
 if (user == null || Boolean.FALSE.equals(user.getIsActive())) {
   return ResponseEntity.status(401).body(Map.of("message", "세션이 유효하지 않습니다."));
 }

 if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
   return ResponseEntity.badRequest().body(Map.of("message", "현재 비밀번호가 일치하지 않습니다."));
 }
 if (encoder.matches(req.newPassword(), user.getPasswordHash())) {
   return ResponseEntity.badRequest().body(Map.of("message", "새 비밀번호가 기존과 동일합니다."));
 }
 if (!isStrongEnough(req.newPassword(), user.getUsername())) {
   return ResponseEntity.badRequest().body(Map.of(
       "message", "비밀번호는 8자 이상, 공백 없음, 아이디/이메일 미포함, 숫자/문자/특수 중 2종류 이상을 포함해야 합니다."
   ));
 }

 user.setPasswordHash(encoder.encode(req.newPassword()));
 // updatedAt은 @PreUpdate가 채워줌
 users.save(user);

 session.invalidate(); // 보안상 재로그인 유도
 return ResponseEntity.ok(Map.of("ok", true, "relogin", true,
     "message", "비밀번호가 변경되었습니다. 다시 로그인해 주세요."));
}

private boolean isStrongEnough(String pw, String username) {
 if (pw == null || pw.length() < 8 || pw.contains(" ")) return false;
 if (username != null && !username.isBlank() &&
     pw.toLowerCase().contains(username.toLowerCase())) return false;
 boolean hasDigit = pw.chars().anyMatch(Character::isDigit);
 boolean hasAlpha = pw.chars().anyMatch(Character::isLetter);
 boolean hasPunct = pw.chars().anyMatch(c -> "!@#$%^&*()[]{}<>?/\\|~`_-+=.:;,'\"".indexOf(c) >= 0);
 return (hasDigit ? 1:0) + (hasAlpha?1:0) + (hasPunct?1:0) >= 2;
}

/** 네비용 최소 정보 */
private Map<String,Object> userMapForNav(CustomerUserEntity u) {
 var c = u.getCustomer(); // @EntityGraph로 이미 로딩됨
 return Map.of(
     "id", u.getId(),
     "type", "customer",
     "username", u.getUsername(),
     "email", u.getUsername(),
     "customerCode", c != null ? c.getCode() : null,
     "customerName", c != null ? c.getName() : null,
     "role", u.getRole()
 );
}


}

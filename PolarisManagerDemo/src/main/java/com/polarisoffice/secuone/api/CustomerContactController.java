package com.polarisoffice.secuone.api;

import com.polarisoffice.secuone.domain.CustomerContactEntity;
import com.polarisoffice.secuone.domain.CustomerEntity;
import com.polarisoffice.secuone.domain.CustomerUserEntity;
import com.polarisoffice.secuone.dto.ContactDtos.ContactReq;
import com.polarisoffice.secuone.dto.ContactDtos.ContactRes;
import com.polarisoffice.secuone.repository.CustomerContactRepository;
import com.polarisoffice.secuone.repository.CustomerUserRepository;
import com.polarisoffice.secuone.service.CustomerContactService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
public class CustomerContactController {

    private static final String SESSION_KEY = "AUTH_CUSTOMER_ID";

    private final CustomerContactService contactService;

    // 추가 의존성: /me 업데이트용
    private final CustomerUserRepository userRepo;
    private final CustomerContactRepository contactRepo;

    public CustomerContactController(CustomerContactService contactService,
                                     CustomerUserRepository userRepo,
                                     CustomerContactRepository contactRepo) {
        this.contactService = contactService;
        this.userRepo = userRepo;
        this.contactRepo = contactRepo;
    }

    /** 담당자 생성/수정 + (옵션) 계정 자동 생성 */
    @PostMapping("/upsert")
    public ResponseEntity<ContactRes> upsert(@RequestBody ContactReq req) {
        ContactRes res = contactService.upsertAndMaybeCreateAccount(req);
        if (res != null && res.id != null) {
            URI location = URI.create("/api/contacts/" + res.id);
            return ResponseEntity.created(location).body(res); // 201 Created
        }
        return ResponseEntity.ok(res); // 200 OK
    }

    /** 고객사별 담당자 목록 조회 (키워드 검색 지원) */
    @GetMapping
    public ResponseEntity<List<ContactRes>> list(
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(contactService.listByCustomer(customerCode, q));
    }

    /** 담당자 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<ContactRes> get(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.get(id));
    }

    /** 담당자 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** 특정 고객사의 모든 담당자 삭제(옵션) */
    @DeleteMapping("/by-customer/{customerCode}")
    public ResponseEntity<Void> deleteByCustomer(@PathVariable String customerCode) {
        contactService.deleteByCustomer(customerCode);
        return ResponseEntity.noContent().build();
    }

    // =========================
    //   내 연락처(me) 업데이트
    // =========================

    public record MeUpdateReq(String name, String phone, String email) {}

    /** 내 연락처 조회 (UI가 이 엔드포인트를 쓰고 싶을 때) */
    @GetMapping("/me")
    public ResponseEntity<?> myContact(HttpSession session) {
        Long uid = (Long) session.getAttribute(SESSION_KEY);
        if (uid == null) return ResponseEntity.status(401).build();

        var u = userRepo.findById(uid).orElse(null);
        if (u == null || Boolean.FALSE.equals(u.getIsActive())) return ResponseEntity.status(401).build();

        var cust = u.getCustomer();
        String code = cust != null ? cust.getCode() : null;
        String baseEmail = u.getUsername();

        CustomerContactEntity c = contactRepo
            .findFirstByCustomer_CodeAndEmailIgnoreCase(code, baseEmail)
            .orElseGet(() -> contactRepo
                .findFirstByCustomer_CodeAndIsPrimaryTrueOrderByIdAsc(code)
                .orElse(null));

        String name  = c != null ? c.getName()  : null;
        String phone = c != null ? c.getPhone() : null;
        String email = (c != null && c.getEmail() != null) ? c.getEmail() : baseEmail;

        return ResponseEntity.ok(Map.of(
            "name", name, "phone", phone, "email", email,
            "customerCode", code,
            "customerName", cust != null ? cust.getName() : null
        ));
    }

    /** 내 연락처 수정 (없으면 생성) */
    @PutMapping("/me")
    @Transactional
    public ResponseEntity<?> updateMyContact(HttpSession session, @RequestBody @Valid MeUpdateReq req) {
        Long uid = (Long) session.getAttribute(SESSION_KEY);
        if (uid == null) return ResponseEntity.status(401).build();

        var u = userRepo.findById(uid).orElse(null);
        if (u == null || Boolean.FALSE.equals(u.getIsActive())) return ResponseEntity.status(401).build();

        CustomerEntity cust = u.getCustomer();
        if (cust == null) return ResponseEntity.badRequest().body(Map.of("error", "Customer not linked"));

        String code = cust.getCode();
        String baseEmail = u.getUsername();

        CustomerContactEntity c = contactRepo
            .findFirstByCustomer_CodeAndEmailIgnoreCase(code, baseEmail)
            .orElseGet(() -> contactRepo
                .findFirstByCustomer_CodeAndIsPrimaryTrueOrderByIdAsc(code)
                .orElse(null));

        if (c == null) {
            c = new CustomerContactEntity();
            c.setCustomer(cust);
            c.setRole("CONTACT");
        }
        if (req.name()  != null) c.setName(req.name());
        if (req.phone() != null) c.setPhone(req.phone());
        if (req.email() != null) c.setEmail(req.email());
        if (c.getEmail() == null) c.setEmail(baseEmail); // 최소 이메일 보장

        contactRepo.save(c);

        return ResponseEntity.ok(Map.of(
            "name", c.getName(),
            "phone", c.getPhone(),
            "email", c.getEmail(),
            "customerCode", code,
            "customerName", cust.getName()
        ));
    }
}

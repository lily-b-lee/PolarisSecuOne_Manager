// src/main/java/com/polarisoffice/secuone/api/HomeController.java
package com.polarisoffice.secuone.api;

import com.polarisoffice.secuone.repository.CustomerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class HomeController {

  // ✅ 주입 누락되었던 부분
  private final CustomerRepository customerRepo;

  // ✅ 생성자 주입 (롬복 없이)
  public HomeController(CustomerRepository customerRepo) {
    this.customerRepo = customerRepo;
  }

  /** 관리자/공용 오버뷰 */
  @GetMapping("/overview")
  public String overview(Model model, Locale locale) {
    setToday(model, locale);
    return "overview";
  }

  /** 고객사 오버뷰 */
  @GetMapping("/manager/overview")
  public String managerOverview(Model model, Locale locale) {
    setToday(model, locale);
    return "manager_overview";
  }

  // ====== 리포트 진입 뷰 ======
  @GetMapping("/manager/reports")
  public String managerReports(HttpServletRequest req,
                               @RequestParam(value="customerCode", required=false) String explicit,
                               Model model, Locale locale) {
    setToday(model, locale);
    // 뷰에서 <meta name="customer-code">로 사용
    String cc = resolveCustomerCodeForView(req, explicit);
    model.addAttribute("customerCode", cc);
    return "manager_report";
  }

  // ====== 직광고 뷰 ======

  /** 직광고 대시보드 (관리자) */
  @GetMapping("/directads")
  public String directads(Model model, Locale locale) {
    setToday(model, locale);
    return "directads";            // templates/directads.html
  }

  /** 직광고 대시보드 (고객사 포털, 필요 시 사용) */
  @GetMapping("/manager/directads")
  public String managerDirectads(Model model, Locale locale) {
    setToday(model, locale);
    return "manager_directads";    // templates/manager_directads.html (선택)
  }

  // ---- 기존 라우팅들 ----
  @GetMapping("/newsletter")   public String newsletter()   { return "newsletter"; }
  @GetMapping("/polarletter")  public String polarletter()  { return "polarletter"; }
  @GetMapping("/notice")       public String notice()       { return "notice"; }
  @GetMapping("/track_report") public String trackReport()  { return "track_report"; }
  @GetMapping("/customers")    public String customers()    { return "customers"; }

  @GetMapping("/customers/detail")
  public String customerDetail() { return "customer_detail"; }

  @GetMapping("/customer_detail")
  public String customerDetailQuery(@RequestParam(required = false) String id, Model model) {
    model.addAttribute("customerId", id);
    return "customer_detail";
  }

  @GetMapping("/customers/{id}")
  public String customerDetailPath(@PathVariable String id, Model model) {
    model.addAttribute("customerId", id);
    return "customer_detail";
  }

  /** 로그인 페이지 */
  @GetMapping("/login")
  public String login(@RequestParam(value = "next", required = false) String next, Model model) {
    if (next != null && !next.isBlank()) model.addAttribute("next", next);
    model.addAttribute("showNav", false);
    return "login";
  }

  @GetMapping("/signup")           public String signup() { return "signup"; }
  @GetMapping("/forgot-password")  public String forgotPassword() { return "forgot_password"; }

  // ====== 내부 유틸 ======
  private void setToday(Model model, Locale locale) {
    var today = LocalDate.now();
    var d1 = DateTimeFormatter.ofPattern("yyyy-MM-dd", locale);
    var d2 = DateTimeFormatter.ofPattern("EEEE", locale);
    model.addAttribute("todayStr", today.format(d1));
    model.addAttribute("weekday",  today.format(d2));
  }

  private static boolean hasText(String s){ return s != null && !s.isBlank(); }

  /** 보고서 뷰용 고객사코드 결정 로직 (헤더/쿼리/JWT/도메인) */
  private String resolveCustomerCodeForView(HttpServletRequest req, String explicit) {
    if (hasText(explicit)) return explicit;

    String byHeader = req.getHeader("X-Customer-Code");
    if (hasText(byHeader)) return byHeader;

    String q = req.getParameter("customerCode");
    if (!hasText(q)) q = req.getParameter("cc");
    if (hasText(q)) return q;

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      Map<String,Object> claims = claimsFromAuth(auth);
      Object v = claims.get("customerCode");
      if (v == null) v = claims.get("tenantCode");
      if (v != null && hasText(String.valueOf(v))) return String.valueOf(v);

      String fromP = tryFromObject(auth.getPrincipal());
      if (hasText(fromP)) return fromP;

      String fromD = tryFromObject(auth.getDetails());
      if (hasText(fromD)) return fromD;
    }

    // ✅ 도메인 → 고객사코드 (여기서 customerRepo 사용)
    String host = Optional.ofNullable(req.getHeader("X-Customer-Domain"))
        .orElseGet(() -> Optional.ofNullable(req.getHeader("X-Forwarded-Host")).orElse(req.getServerName()));
    if (hasText(host)) {
      host = host.replaceFirst(":\\d+$", "");
      return customerRepo.findByDomainIgnoreCase(host)
          .map(c -> c.getCode())
          .orElse("mg"); // 실패 시 dev 기본값
    }
    return "mg";
  }

  /** principal/details 에서 코드 추출 시도 */
  private String tryFromObject(Object src) {
    if (src == null) return null;

    if (src instanceof Map<?,?> map) {
      Object v = map.get("customerCode");
      if (v == null) v = map.get("tenantCode");
      if (v != null && hasText(String.valueOf(v))) return String.valueOf(v);
    }
    for (String getter : List.of("getCustomerCode","customerCode","getTenantCode","tenantCode","getCode","code")) {
      try {
        Method m = src.getClass().getMethod(getter);
        Object v = m.invoke(src);
        if (v != null && hasText(String.valueOf(v))) return String.valueOf(v);
      } catch (NoSuchMethodException ignored) {
      } catch (Exception ignored) { }
    }
    String s = String.valueOf(src);
    var m = java.util.regex.Pattern.compile("(?:customerCode|tenantCode|code)=([A-Za-z0-9_-]+)").matcher(s);
    if (m.find()) return m.group(1);
    return null;
  }

  /** JWT/OAuth 토큰 타입들에서 claims만 ‘리플렉션’으로 안전 추출 */
  @SuppressWarnings("unchecked")
  private Map<String,Object> claimsFromAuth(Authentication auth) {
    if (auth == null) return Collections.emptyMap();

    try {
      var m = auth.getClass().getMethod("getTokenAttributes"); // OAuth2AuthenticationToken 등
      Object v = m.invoke(auth);
      if (v instanceof Map<?,?> map) return (Map<String,Object>) map;
    } catch (NoSuchMethodException ignored) {
    } catch (Exception ignored) {}

    try {
      var mTok = auth.getClass().getMethod("getToken");      // JwtAuthenticationToken#getToken()
      Object jwt = mTok.invoke(auth);
      if (jwt != null) {
        var mClaims = jwt.getClass().getMethod("getClaims"); // Jwt#getClaims()
        Object c = mClaims.invoke(jwt);
        if (c instanceof Map<?,?> map) return (Map<String,Object>) map;
      }
    } catch (NoSuchMethodException ignored) {
    } catch (Exception ignored) {}

    Object p = auth.getPrincipal();
    if (p instanceof Map<?,?> map) return (Map<String,Object>) map;
    return Collections.emptyMap();
  }
}

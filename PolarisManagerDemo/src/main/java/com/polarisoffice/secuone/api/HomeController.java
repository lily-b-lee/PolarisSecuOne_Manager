// src/main/java/com/polarisoffice/secuone/api/HomeController.java
package com.polarisoffice.secuone.api;

import com.polarisoffice.secuone.repository.CustomerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Controller
public class HomeController {

  private static final Pattern CODE_FROM_STRING =
      Pattern.compile("(?:customerCode|tenantCode|code)=([A-Za-z0-9_-]+)");

  private final CustomerRepository customerRepo;

  public HomeController(CustomerRepository customerRepo) {
    this.customerRepo = customerRepo;
  }

  /** 루트 → 로그인 */
  @GetMapping("/")
  public String home() { return "redirect:/login"; }

  /** 로그인 (공용). /admin/login, /customer/login은 기본 모드만 다르게 전달 */
  @GetMapping("/login")
  public String login(@RequestParam(value="next", required=false) String next, Model model) {
    String safeNext = sanitizeNext(next); // 오픈 리다이렉트 방지
    if (hasText(safeNext)) model.addAttribute("next", safeNext);
    model.addAttribute("showNav", false);
    // 템플릿에서 기본 모드 탭 선택에 사용할 값 (admin|customer)
    model.addAttribute("defaultMode", "admin");
    return "login";
  }

  @GetMapping("/admin/login")
  public String adminLogin(@RequestParam(value="next", required=false) String next, Model model) {
    String safeNext = sanitizeNext(next);
    if (hasText(safeNext)) model.addAttribute("next", safeNext);
    model.addAttribute("showNav", false);
    model.addAttribute("defaultMode", "admin");
    return "login";
  }

  @GetMapping("/customer/login")
  public String customerLogin(@RequestParam(value="next", required=false) String next, Model model) {
    String safeNext = sanitizeNext(next);
    if (hasText(safeNext)) model.addAttribute("next", safeNext);
    model.addAttribute("showNav", false);
    model.addAttribute("defaultMode", "customer");
    return "login";
  }

  /**
   * 관리자 회원가입 (별칭 포함)
   * - 회원가입을 노출하지 않기로 한 정책이라면 404로 응답합니다.
   *   노출하려면 아래 throw 라인을 제거하고 "admin_signup" 반환하세요.
   */
  @GetMapping({"/admin/signup", "/admin_signup"})
  public String adminSignup() {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    // return "admin_signup";
  }

  /**
   * 고객사 회원가입 (별칭 포함)
   * - 회원가입 미노출 정책 시 404
   */
  @GetMapping({"/customer/signup", "/customer_signup"})
  public String customerSignup() {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    // return "customer_signup";
  }

  /** 구 주소 호환 */
  @GetMapping("/signup")
  public String legacySignup() { return "redirect:/admin/signup"; }

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

  /** 리포트 진입 뷰 */
  @GetMapping("/manager/reports")
  public String managerReports(HttpServletRequest req,
                               @RequestParam(value="customerCode", required=false) String explicit,
                               Model model, Locale locale) {
    setToday(model, locale);
    String cc = resolveCustomerCodeForView(req, explicit);
    model.addAttribute("customerCode", cc);
    return "manager_report";
  }

  /** 직광고 뷰 */
  @GetMapping("/directads")
  public String directads(Model model, Locale locale) {
    setToday(model, locale);
    return "directads";
  }

  @GetMapping("/manager/directads")
  public String managerDirectads(Model model, Locale locale) {
    setToday(model, locale);
    return "manager_directads";
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
  public String customerDetailQuery(@RequestParam(required=false) String id, Model model) {
    model.addAttribute("customerId", id);
    return "customer_detail";
  }

  @GetMapping("/customers/{id}")
  public String customerDetailPath(@PathVariable String id, Model model) {
    model.addAttribute("customerId", id);
    return "customer_detail";
  }

  // ====== 내부 유틸 ======
  private void setToday(Model model, Locale locale) {
    var today = LocalDate.now();
    var d1 = DateTimeFormatter.ofPattern("yyyy-MM-dd", locale);
    var d2 = DateTimeFormatter.ofPattern("EEEE", locale);
    model.addAttribute("todayStr", today.format(d1));
    model.addAttribute("weekday",  today.format(d2));
  }

  private static boolean hasText(String s){ return s != null && !s.isBlank(); }

  /** /login?next=... 오픈 리다이렉트 방지: 절대 URL/스킴/이중 슬래시 불허, 상대 경로만 허용 */
  private static String sanitizeNext(String next) {
    if (!hasText(next)) return null;
    // 공백 제거
    String n = next.trim();
    // 절대 URL, 스킴, 프로토콜 상대, 이중 슬래시 시작, CR/LF 등 불허
    if (n.startsWith("http://") || n.startsWith("https://") || n.startsWith("//")) return null;
    if (n.contains("\r") || n.contains("\n")) return null;
    // 상대경로만 허용
    if (n.startsWith("/")) return n;
    return null;
  }

  /** 보고서 뷰용 고객사코드 결정 로직 (헤더/쿼리/JWT/도메인) */
  private String resolveCustomerCodeForView(HttpServletRequest req, String explicit) {
    if (hasText(explicit)) return explicit;

    String byHeader = req.getHeader("X-Customer-Code");
    if (hasText(byHeader)) return byHeader;

    String q = req.getParameter("customerCode");
    if (!hasText(q)) q = req.getParameter("cc");
    if (hasText(q)) return q;

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && !(auth instanceof AnonymousAuthenticationToken)) {
      Map<String,Object> claims = claimsFromAuth(auth);
      Object v = claims.get("customerCode");
      if (v == null) v = claims.get("tenantCode");
      if (v != null && hasText(String.valueOf(v))) return String.valueOf(v);

      String fromP = tryFromObject(auth.getPrincipal());
      if (hasText(fromP)) return fromP;

      String fromD = tryFromObject(auth.getDetails());
      if (hasText(fromD)) return fromD;
    }

    String host = headerFirst(req.getHeader("X-Customer-Domain"));
    if (!hasText(host)) host = headerFirst(req.getHeader("X-Forwarded-Host"));
    if (!hasText(host)) host = req.getServerName();

    if (hasText(host)) {
      host = normalizeHost(host);
      return customerRepo.findByDomainIgnoreCase(host)
          .map(c -> c.getCode())
          .orElse("mg");
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
    var m = CODE_FROM_STRING.matcher(s);
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
      var mTok = auth.getClass().getMethod("getToken"); // JwtAuthenticationToken#getToken()
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

  /** 콤마 체인(프록시)에서 첫 호스트만 추출 */
  private static String headerFirst(String headerVal) {
    if (!hasText(headerVal)) return null;
    int idx = headerVal.indexOf(',');
    return (idx > -1) ? headerVal.substring(0, idx).trim() : headerVal.trim();
  }

  /** 호스트 정규화: 소문자, 포트 제거, 선행 www. 제거 */
  private static String normalizeHost(String host) {
    String h = host.toLowerCase(Locale.ROOT).trim();
    // 포트 제거
    h = h.replaceFirst(":\\d+$", "");
    // 선행 www. 제거
    if (h.startsWith("www.")) h = h.substring(4);
    return h;
  }
}

package com.polarisoffice.secuone.service;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class MailService {
  private final JavaMailSender mail;

  @Value("${app.mail.from:${spring.mail.username}}")
  private String fromEmail; // 반드시 이메일 주소여야 함

  @Value("${app.mail.from.personal:SecuOne}")
  private String fromPersonal; // 표시 이름(옵션)

  public MailService(JavaMailSender mail) { this.mail = mail; }

  public void sendInitialPassword(String toRaw, String name, String customerCode, String tempPassword) {
    String from = sanitize(fromEmail);
    String fromName = fromPersonal == null ? "SecuOne" : fromPersonal.trim();
    String toLog = toRaw; // 원본 로깅용(문자 코드 확인)
    try {
      // 1) 수신자 파싱/검증: 콤마/세미콜론/개행/이름 섞여도 처리
      InternetAddress[] tos = parseRecipients(toRaw);
      if (tos.length == 0)
        throw new IllegalArgumentException("수신자 주소가 비어있거나 파싱 실패");

      // 2) 메시지 생성
      var mm = mail.createMimeMessage();
      var helper = new MimeMessageHelper(mm, false, StandardCharsets.UTF_8.name());

      // From (주소는 이메일, 표시 이름은 별도)
      helper.setFrom(new InternetAddress(from, fromName));

      // To (여러 명 가능)
      helper.setTo(tos);

      // 제목/본문
      helper.setSubject("[SecuOne] 초기 비밀번호 안내");
      String displayName = name == null ? "" : name.trim();
      String body = """
        안녕하세요 %s님,

        SecuOne 고객 포털 계정의 초기 비밀번호를 안내드립니다.

        - 고객사 코드: %s
        - 임시 비밀번호: %s

        보안을 위해 최초 로그인 후 비밀번호를 변경해 주세요.
        감사합니다.
        """.formatted(displayName, customerCode, tempPassword);
      helper.setText(body, false); // plain text

      // 3) 방어: 실제 recipients 존재 확인
      if (mm.getAllRecipients() == null || mm.getAllRecipients().length == 0) {
        throw new IllegalStateException("메시지에 수신자가 없습니다.(post-check)");
      }

      // 4) 발송
      mail.send(mm);

    } catch (Exception e) {
      // 디버깅을 위해 수신자 문자열과 유니코드 코드포인트까지 남김
      String toHex = toRaw == null ? "null" :
          toRaw.chars().collect(StringBuilder::new,
              (sb, c) -> sb.append(String.format("%c(0x%04X) ", (char)c, c)),
              StringBuilder::append).toString();
      System.err.println("[MailService] send fail"
          + " from=" + from
          + ", toRaw=" + toLog
          + ", toHex=" + toHex
          + ", msg=" + e.getMessage());
      throw new RuntimeException("메일 발송 실패: " + e.getMessage(), e);
    }
  }

  /** 콤마/세미콜론/개행/공백/이름포맷까지 안전 파싱 + 엄격검증(strict=true) */
  private static InternetAddress[] parseRecipients(String raw) throws Exception {
    if (raw == null) return new InternetAddress[0];
    // 개행 제거, 세미콜론을 콤마로 통일
    String norm = raw.replace("\r", " ").replace("\n", " ").replace(";", ",").trim();
    if (norm.isEmpty()) return new InternetAddress[0];

    // 수동 split 후 각 항목 엄격 검증
    String[] parts = norm.split(",");
    List<InternetAddress> list = new ArrayList<>();
    for (String p : parts) {
      String s = sanitize(p);
      if (s.isEmpty()) continue;
      // "표시이름 <addr@domain>" 형식도 허용, strict=true로 유효성 체크
      InternetAddress[] arr = InternetAddress.parse(s, true);
      for (InternetAddress ia : arr) {
        // display-only(이름만 있고 주소 없는 경우) 필터
        if (ia.getAddress() != null && !ia.getAddress().isBlank()) {
          list.add(ia);
        }
      }
    }
    return list.toArray(InternetAddress[]::new);
  }

  /** 헤더 인젝션 방지용 sanitize */
  private static String sanitize(String s) {
    if (s == null) return "";
    // CR/LF 제거 + 양쪽 공백 제거 + 다중 스페이스 축약
    return s.replaceAll("[\\r\\n]", " ").trim().replaceAll("\\s{2,}", " ");
  }
}

// src/main/java/com/polarisoffice/secuone/api/ImageProxyController.java
package com.polarisoffice.secuone.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@RestController
public class ImageProxyController {

  // ✅ 정확 매칭 호스트
  private static final Set<String> ALLOW_HOSTS = Set.of(
      "images.unsplash.com",
      "static.polarisoffice.com",
      "cdn.polarisoffice.com",
      "wiki.polarisoffice.com",
      "postfiles.pstatic.net"         // ← 추가
  );

  // ✅ 서픽스(하위 도메인 포함) 허용
  private static final List<String> ALLOW_SUFFIXES = List.of(
      ".pstatic.net",                 // 네이버 CDN
      ".naver.com", ".naver.net"
      // 필요 시 ".googleusercontent.com", ".ggpht.com", ".akamaized.net" 등 추가
  );

  private final HttpClient client = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.ALWAYS)
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  @GetMapping("/img-proxy")
  public ResponseEntity<byte[]> proxy(@RequestParam("u") String url) {
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      return ResponseEntity.badRequest().build();
    }

    String scheme = uri.getScheme();
    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
      return ResponseEntity.badRequest().build();
    }

    String host = uri.getHost();
    if (!isAllowedHost(host)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
    }

    // 일부 CDN은 Referer/UA 없으면 403을 반환 → 기본 헤더 부여
    String referer = guessReferer(host);
    String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    HttpRequest.Builder rb = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(10))
        .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
        .header("User-Agent", userAgent);

    if (referer != null) rb.header("Referer", referer);

    HttpResponse<byte[]> res;
    try {
      res = client.send(rb.build(), HttpResponse.BodyHandlers.ofByteArray());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }

    int status = res.statusCode();
    if (status < 200 || status >= 300 || res.body() == null) {
      // 원본이 403/404여도 프록시는 상태만 그대로 알려줌
      return ResponseEntity.status(status).build();
    }

    HttpHeaders out = new HttpHeaders();
    // Content-Type 보존(HTML 같으면 안전 기본)
    MediaType media = null;
    String ct = res.headers().firstValue("content-type").orElse(null);
    if (ct != null) {
      try { media = MediaType.parseMediaType(ct); } catch (Exception ignored) {}
    }
    if (media == null || media.includes(MediaType.TEXT_HTML)) media = MediaType.IMAGE_PNG;
    out.setContentType(media);

    res.headers().firstValue("cache-control").ifPresent(out::setCacheControl);
    res.headers().firstValue("etag").ifPresent(out::setETag);
    res.headers().firstValue("last-modified").ifPresent(v -> out.set(HttpHeaders.LAST_MODIFIED, v));

    return new ResponseEntity<>(res.body(), out, HttpStatus.OK);
  }

  private static boolean isAllowedHost(String host) {
    if (host == null) return false;
    if (ALLOW_HOSTS.contains(host)) return true;
    for (String sfx : ALLOW_SUFFIXES) {
      if (host.endsWith(sfx)) return true;
    }
    return false;
  }

  private static String guessReferer(String host) {
    if (host.endsWith(".pstatic.net") || host.endsWith(".naver.com") || host.endsWith(".naver.net")) {
      return "https://blog.naver.com/"; // 네이버 계열 기본 리퍼러
    }
    return null; // 기본값: 헤더 생략
  }
}

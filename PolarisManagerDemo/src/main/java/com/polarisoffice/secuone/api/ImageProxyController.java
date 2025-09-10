package com.polarisoffice.secuone.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@RestController
public class ImageProxyController {
  private static final Logger log = LoggerFactory.getLogger(ImageProxyController.class);

  // 허용 호스트/서픽스 (오픈 프록시 방지)
  private static final Set<String> ALLOW_HOSTS = Set.of(
      "images.unsplash.com",
      "static.polarisoffice.com",
      "cdn.polarisoffice.com",
      "wiki.polarisoffice.com",
      "postfiles.pstatic.net"
  );
  
  
  private static final List<String> ALLOW_SUFFIXES = List.of(
      ".pstatic.net", ".naver.com", ".naver.net"
  );

  // UA/Referer
  private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
      + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

  
  
  private static ProxySelector buildProxySelectorFromEnv() {
    try {
      String https = System.getenv("HTTPS_PROXY");
      String http  = System.getenv("HTTP_PROXY");
      URI u = https != null ? URI.create(https) : (http != null ? URI.create(http) : null);
      if (u == null || u.getHost() == null || u.getPort() == -1) {
        // 자바 시스템 속성에 설정되어 있을 수 있음(예: -Dhttps.proxyHost= -Dhttps.proxyPort=)
        return ProxySelector.getDefault();
      }
      InetSocketAddress addr = new InetSocketAddress(u.getHost(), u.getPort());
      return ProxySelector.of(addr);
    } catch (Exception e) {
      return ProxySelector.getDefault();
    }
  }

  // HTTP/2 기본, 실패시 1.1로 재시도
  private final HttpClient clientH2 = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .followRedirects(HttpClient.Redirect.ALWAYS)
      .connectTimeout(Duration.ofSeconds(8))
      .proxy(buildProxySelectorFromEnv())
      .build();

  private final HttpClient clientH1 = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_1_1)
      .followRedirects(HttpClient.Redirect.ALWAYS)
      .connectTimeout(Duration.ofSeconds(8))
      .proxy(buildProxySelectorFromEnv())
      .build();

  @GetMapping("/img-proxy")
  public ResponseEntity<byte[]> proxy(@RequestParam("u") String url) {
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      return ResponseEntity.badRequest().body(("bad url").getBytes());
    }

    String scheme = uri.getScheme();
    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
      return ResponseEntity.badRequest().body(("unsupported scheme").getBytes());
    }

    String host = uri.getHost();
    if (!isAllowedHost(host)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(("forbidden host").getBytes());
    }

    HttpRequest.Builder rb = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(12))
        .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
        .header("User-Agent", UA);

    String referer = guessReferer(host);
    if (referer != null) rb.header("Referer", referer);

    // 1차: HTTP/2
    try {
      HttpResponse<byte[]> r = clientH2.send(rb.build(), HttpResponse.BodyHandlers.ofByteArray());
      return toResponse(r);
    } catch (Exception e) {
      log.warn("[img-proxy] h2 request failed for {}: {}", host, e.toString());
      // 2차: HTTP/1.1 재시도
      try {
        HttpResponse<byte[]> r1 = clientH1.send(rb.build(), HttpResponse.BodyHandlers.ofByteArray());
        return toResponse(r1);
      } catch (Exception e1) {
        log.error("[img-proxy] h1 retry failed for {}: {}", host, e1.toString());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .contentType(MediaType.TEXT_PLAIN)
            .body(("bad gateway: " + e1.getClass().getSimpleName()).getBytes());
      }
    }
  }

  private ResponseEntity<byte[]> toResponse(HttpResponse<byte[]> r) {
    int st = r.statusCode();
    if (st < 200 || st >= 300 || r.body() == null) {
      // 업스트림 상태 그대로 반영
      return ResponseEntity.status(st).contentType(MediaType.TEXT_PLAIN)
          .body(("upstream " + st).getBytes());
    }
    HttpHeaders out = new HttpHeaders();
    MediaType media = null;
    String ct = r.headers().firstValue("content-type").orElse(null);
    if (ct != null) {
      try { media = MediaType.parseMediaType(ct); } catch (Exception ignore) {}
    }
    if (media == null || media.includes(MediaType.TEXT_HTML)) media = MediaType.IMAGE_PNG;
    out.setContentType(media);
    r.headers().firstValue("cache-control").ifPresent(out::setCacheControl);
    r.headers().firstValue("etag").ifPresent(out::setETag);
    r.headers().firstValue("last-modified").ifPresent(v -> out.set("Last-Modified", v));
    return new ResponseEntity<>(r.body(), out, HttpStatus.OK);
  }

  private static boolean isAllowedHost(String host) {
    if (host == null) return false;
    if (ALLOW_HOSTS.contains(host)) return true;
    for (String sfx : ALLOW_SUFFIXES) if (host.endsWith(sfx)) return true;
    return false;
  }

  private static String guessReferer(String host) {
    if (host.endsWith(".pstatic.net") || host.endsWith(".naver.com") || host.endsWith(".naver.net"))
      return "https://blog.naver.com/";
    return null;
  }
}

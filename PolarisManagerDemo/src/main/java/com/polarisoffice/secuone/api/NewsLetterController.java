package com.polarisoffice.secuone.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.polarisoffice.secuone.dto.*;
import com.polarisoffice.secuone.service.NewsletterService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
@RestController
@RequestMapping({"/secu-news", "/newsletters"})
public class NewsLetterController {
    private final NewsletterService svc;
    public NewsLetterController(NewsletterService svc) { this.svc = svc; }

    @GetMapping("/{id}")
    public ResponseEntity<NewsletterRes> get(@PathVariable String id) throws Exception {
        var res = svc.get(id);
        return (res == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(res);
    }

    @GetMapping
    public ResponseEntity<List<NewsletterRes>> list(@RequestParam(defaultValue = "20") int limit) throws Exception {
        return ResponseEntity.ok(svc.listLatest(Math.max(1, Math.min(limit, 100))));
    }
    
    @PostMapping
    public ResponseEntity<String> create(@Valid @RequestBody NewsletterCreateReq req) throws Exception {
        String id = svc.create(req);
        return ResponseEntity.ok(id); // 프론트가 그대로 표시함
    }

    // 살아있는지 확인용
    @GetMapping("/_ping")
    public String ping() { return "ok"; }
}

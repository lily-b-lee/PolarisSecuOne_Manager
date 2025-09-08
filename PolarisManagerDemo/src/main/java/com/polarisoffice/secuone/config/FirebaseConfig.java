package com.polarisoffice.secuone.config;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.serviceAccount:}")
    private String serviceAccountPath;

    @Value("${firebase.databaseUrl:}")
    private String databaseUrl;

    @Value("${firebase.appName:polaris-directads}") // 선택적 명명 앱
    private String appName;

    private FirebaseOptions buildOptions() throws Exception {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            throw new IllegalStateException("firebase.serviceAccount 가 비어있습니다.");
        }
        Path p = Path.of(serviceAccountPath.trim());
        if (!Files.exists(p)) {
            throw new IllegalStateException("서비스계정 파일을 찾지 못했습니다: " + p);
        }
        try (FileInputStream in = new FileInputStream(p.toFile())) {
            FirebaseOptions.Builder b = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in));
            if (databaseUrl != null && !databaseUrl.isBlank()) {
                b.setDatabaseUrl(databaseUrl);
            }
            return b.build();
        }
    }

    /** ✅ 반드시 DEFAULT 앱을 보장하고, 필요하면 명명 앱도 만든다. */
    @Bean
    public FirebaseApp firebaseApp() throws Exception {
        FirebaseOptions options = buildOptions();

        List<FirebaseApp> apps = FirebaseApp.getApps();

        // DEFAULT 보장
        FirebaseApp def = apps.stream()
                .filter(a -> a.getName().equals(FirebaseApp.DEFAULT_APP_NAME))
                .findFirst()
                .orElseGet(() -> {
                    FirebaseApp created = FirebaseApp.initializeApp(options);
                    System.out.println("✅ Firebase DEFAULT app initialized. projectId=" + created.getOptions().getProjectId());
                    return created;
                });

        // 선택: 명명 앱도 보장(있으면 스킵)
        boolean hasNamed = apps.stream().anyMatch(a -> a.getName().equals(appName));
        if (!hasNamed) {
            FirebaseApp.initializeApp(options, appName);
            System.out.println("✅ Firebase NAMED app initialized: " + appName);
        }
        return def;
    }

    /** ✅ Firestore 빈 제공 — 앞으로는 이걸 생성자 주입해서 쓰세요. */
    @Bean(destroyMethod = "")  // ⬅️ 중요: Spring이 자동으로 close() 호출하지 않게 막음
    public Firestore firestore(FirebaseApp app) {
        return FirestoreClient.getFirestore(app); // DEFAULT app 기준
    }
}

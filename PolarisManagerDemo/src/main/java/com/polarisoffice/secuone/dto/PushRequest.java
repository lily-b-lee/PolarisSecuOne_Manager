package com.polarisoffice.secuone.dto;

import java.util.List;

public class PushRequest {
	// 단일 수신자: device token 하나일 때
    private String token;

    // 다중 수신자: device token 여러 개일 때
    private List<String> tokens;

    // 토픽으로 발송할 때 (ex. "news", "all-users")
    private String topic;

    // 표시용
    private String title;
    private String body;

    // 앱에서 처리할 커스텀 데이터 (선택)
    private java.util.Map<String, String> data;

    // 플랫폼별 옵션(필요시)
    private boolean highPriority = true;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public List<String> getTokens() {
		return tokens;
	}

	public void setTokens(List<String> tokens) {
		this.tokens = tokens;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public java.util.Map<String, String> getData() {
		return data;
	}

	public void setData(java.util.Map<String, String> data) {
		this.data = data;
	}

	public boolean isHighPriority() {
		return highPriority;
	}

	public void setHighPriority(boolean highPriority) {
		this.highPriority = highPriority;
	}

}

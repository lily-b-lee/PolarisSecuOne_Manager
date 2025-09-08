package com.polarisoffice.secuone.dto;


import jakarta.validation.constraints.*;
import java.time.Instant;

/**
 * 고객사의 사용자 정보 저장
 * */

public class CustomerUser {


	  public static class CreateReq {
	    @NotNull private Long customerId;
	    @NotBlank @Size(max=100) private String username;
	    @NotBlank @Size(min=8, max=255) private String password; // 평문으로 받고 서버에서 해시
	    @NotBlank @Size(max=20) private String role; // ADMIN/EDITOR/VIEWER
	    private Boolean isActive;

	    // getters/setters
	    public Long getCustomerId(){ return customerId; } public void setCustomerId(Long v){ this.customerId = v; }
	    public String getUsername(){ return username; } public void setUsername(String v){ this.username = v; }
	    public String getPassword(){ return password; } public void setPassword(String v){ this.password = v; }
	    public String getRole(){ return role; } public void setRole(String v){ this.role = v; }
	    public Boolean getIsActive(){ return isActive; } public void setIsActive(Boolean v){ this.isActive = v; }
	  }

	  public static class UpdateReq {
	    @Size(max=100) private String username;
	    @Size(min=8, max=255) private String password;
	    @Size(max=20) private String role;
	    private Boolean isActive;

	    // getters/setters
	    public String getUsername(){ return username; } public void setUsername(String v){ this.username = v; }
	    public String getPassword(){ return password; } public void setPassword(String v){ this.password = v; }
	    public String getRole(){ return role; } public void setRole(String v){ this.role = v; }
	    public Boolean getIsActive(){ return isActive; } public void setIsActive(Boolean v){ this.isActive = v; }
	  }

	  public static class Res {
	    private Long id;
	    private Long customerId;
	    private String username;
	    private String role;
	    private Boolean isActive;
	    private Instant lastLoginAt;
	    private Instant createdAt;

	    public Res(Long id, Long customerId, String username, String role,
	               Boolean isActive, Instant lastLoginAt, Instant createdAt){
	      this.id=id; this.customerId=customerId; this.username=username; this.role=role;
	      this.isActive=isActive; this.lastLoginAt=lastLoginAt; this.createdAt=createdAt;
	    }

	    public Long getId(){ return id; }
	    public Long getCustomerId(){ return customerId; }
	    public String getUsername(){ return username; }
	    public String getRole(){ return role; }
	    public Boolean getIsActive(){ return isActive; }
	    public Instant getLastLoginAt(){ return lastLoginAt; }
	    public Instant getCreatedAt(){ return createdAt; }
	  }
}

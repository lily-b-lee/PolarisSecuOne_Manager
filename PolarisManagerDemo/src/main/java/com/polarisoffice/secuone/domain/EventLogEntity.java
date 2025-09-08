package com.polarisoffice.secuone.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
	  name = "event_logs",
	  indexes = {
	    @Index(name = "ix_eventlogs_customer_date", columnList = "customer_code, created_at")
	  }
	)
public class EventLogEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_code", referencedColumnName = "code",
                foreignKey = @ForeignKey(name = "fk_event_customer_code"))
    private CustomerEntity customer;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "object_type", length = 50)
    private String objectType;

    @Column(name = "object_id", length = 100)
    private String objectId;

    @Column(name = "actor", length = 100)
    private String actor;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "ua", length = 255)
    private String ua;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @PrePersist void prePersist() {
      if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUa() {
        return ua;
    }

    public void setUa(String ua) {
        this.ua = ua;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }    
}

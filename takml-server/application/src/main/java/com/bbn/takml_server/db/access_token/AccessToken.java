package com.bbn.takml_server.db.access_token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AccessToken {
    public static final String ACCESS_TOKEN_ID = "accessToken";

    @Id
    String id = ACCESS_TOKEN_ID;

    @Column(length = 1000)
    String tokenId;

    public AccessToken() {
    }

    public AccessToken(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }
}
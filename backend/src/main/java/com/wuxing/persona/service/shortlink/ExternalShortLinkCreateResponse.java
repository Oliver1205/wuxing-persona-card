package com.wuxing.persona.service.shortlink;

public class ExternalShortLinkCreateResponse {

    private String gid;
    private String originUrl;
    private String fullShortUrl;

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public String getFullShortUrl() {
        return fullShortUrl;
    }

    public void setFullShortUrl(String fullShortUrl) {
        this.fullShortUrl = fullShortUrl;
    }
}

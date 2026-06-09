package com.wuxing.persona.service.shortlink;

public class ExternalShortLinkCreateRequest {

    private String domain;
    private String originUrl;
    private String gid;
    private Integer createdType;
    private Integer validDateType;
    private String validDate;
    private String describe;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public Integer getCreatedType() {
        return createdType;
    }

    public void setCreatedType(Integer createdType) {
        this.createdType = createdType;
    }

    public Integer getValidDateType() {
        return validDateType;
    }

    public void setValidDateType(Integer validDateType) {
        this.validDateType = validDateType;
    }

    public String getValidDate() {
        return validDate;
    }

    public void setValidDate(String validDate) {
        this.validDate = validDate;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }
}

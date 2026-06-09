package com.wuxing.persona.service.shortlink;

public class ExternalShortLinkAccessRecordRequest extends ExternalShortLinkStatsRequest {

    private Long current;
    private Long size;

    public Long getCurrent() {
        return current;
    }

    public void setCurrent(Long current) {
        this.current = current;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}

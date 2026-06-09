package com.wuxing.persona.service.shortlink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalShortLinkAccessRecordPageResponse {

    private Long current;
    private Long size;
    private Long total;
    private List<ExternalShortLinkAccessRecordResponse> records = new ArrayList<>();

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

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<ExternalShortLinkAccessRecordResponse> getRecords() {
        return records;
    }

    public void setRecords(List<ExternalShortLinkAccessRecordResponse> records) {
        this.records = records == null ? new ArrayList<>() : records;
    }
}

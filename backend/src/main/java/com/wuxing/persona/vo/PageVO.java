package com.wuxing.persona.vo;

import java.util.List;

public class PageVO<T> {

    private long page;
    private long pageSize;
    private long total;
    private List<T> records;

    public PageVO() {
    }

    public PageVO(long page, long pageSize, long total, List<T> records) {
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.records = records;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}

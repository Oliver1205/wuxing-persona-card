package com.wuxing.persona.vo;

public class NameCountVO {

    private String name;
    private long count;

    public NameCountVO() {
    }

    public NameCountVO(String name, long count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}

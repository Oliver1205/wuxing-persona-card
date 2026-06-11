package com.wuxing.persona.vo;

public class FunnelStepVO {

    private String eventType;
    private String label;
    private long count;
    private double conversionRate;

    public FunnelStepVO() {
    }

    public FunnelStepVO(String eventType, String label, long count, double conversionRate) {
        this.eventType = eventType;
        this.label = label;
        this.count = count;
        this.conversionRate = conversionRate;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(double conversionRate) {
        this.conversionRate = conversionRate;
    }
}

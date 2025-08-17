package com.jasonlat.types.models;

public enum AnalyzeStatus {

    ANALYZING("analyzing"),
    ANALYZED("analyzed"),
    ERROR("error");

    private final String status;

    AnalyzeStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return status;
    }
}
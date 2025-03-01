package com.chung.a9rushtobus;

public class RTHKTrafficEntry {
    private String news;
    private String date;

    public RTHKTrafficEntry(String news, String date) {
        this.news = news;
        this.date = date;
    }

    public String getNews() {
        return news;
    }
    public String getDate() {
        return date;
    }
}
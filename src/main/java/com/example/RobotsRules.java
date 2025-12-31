package com.example;

import java.util.ArrayList;
import java.util.List;

public class RobotsRules {
    List<String> disallowed = new ArrayList<>();
    List<String> allowed = new ArrayList<>();
    int crawlDelayMs = 1000;
    public RobotsRules(List<String> disallowed, List<String> allowed) {
        this.disallowed = disallowed;
        this.allowed = allowed;
    }

    public RobotsRules() {
    }

    public List<String> getDisallowed() {
        return disallowed;
    }

    public void setDisallowed(List<String> disallowed) {
        this.disallowed = disallowed;
    }

    public List<String> getAllowed() {
        return allowed;
    }

    public void setAllowed(List<String> allowed) {
        this.allowed = allowed;
    }
}

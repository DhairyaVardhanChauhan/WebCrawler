package com.example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class RobotsGroup {
    Set<String> userAgents = new HashSet<>();
    List<String> disallow = new ArrayList<>();
    List<String> allow = new ArrayList<>();
    Integer crawlDelaySeconds = null;
}

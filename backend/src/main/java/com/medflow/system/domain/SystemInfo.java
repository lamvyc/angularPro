package com.medflow.system.domain;

import java.time.Instant;
import java.util.List;

/**
 * 系统运行时信息（不包含任何临床或患者数据）。
 */
public record SystemInfo(String application, String version, List<String> profiles, Instant serverTime) {
}

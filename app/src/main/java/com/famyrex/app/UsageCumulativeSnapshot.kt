package com.famyrex.app

data class UsageCumulativeSnapshot(
    val timestampMs: Long,
    val totalsByPackageMs: Map<String, Long>
)

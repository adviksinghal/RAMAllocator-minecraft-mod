package com.ra.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RAMAdvisorEngine {

    public static class DiagnosticReport {
        public final long currentAllocatedMB;
        public final long peakUsageMB;
        public final long targetMB;
        public final String statusSummary;
        public final String explanation;

        public DiagnosticReport(long currentAllocatedMB, long peakUsageMB, long targetMB,
                                String statusSummary, String explanation) {
            this.currentAllocatedMB = currentAllocatedMB;
            this.peakUsageMB = peakUsageMB;
            this.targetMB = targetMB;
            this.statusSummary = statusSummary;
            this.explanation = explanation;
        }
    }

    public static DiagnosticReport generateReport(long peakBaselineMB, List<Long> gameplaySamples) {
        long maxAllocatedBytes = Runtime.getRuntime().maxMemory();
        long currentAllocatedMB = maxAllocatedBytes / (1024 * 1024);

        long calculatedPeakMB = peakBaselineMB;

        if (!gameplaySamples.isEmpty()) {
            List<Long> sortedSamples = new ArrayList<>(gameplaySamples);
            Collections.sort(sortedSamples);

            // Get 95th percentile index to filter out extreme momentary outlier spikes
            int p95Index = (int) Math.ceil(0.95 * sortedSamples.size()) - 1;
            p95Index = Math.max(0, Math.min(p95Index, sortedSamples.size() - 1));

            calculatedPeakMB = Math.max(peakBaselineMB, sortedSamples.get(p95Index));
        }

        // Target: 95th Percentile Peak + 300 MB Cushion
        long targetMB = calculatedPeakMB + 300;

        String statusSummary;
        String explanation;

        long difference = currentAllocatedMB - targetMB;

        if (Math.abs(difference) <= 100) {
            statusSummary = "§a✔ Perfect RAM Setup!";
            explanation = "Your game handles peak memory usage smoothly with your current settings. No changes needed!";
        } else if (currentAllocatedMB < targetMB) {
            statusSummary = "§c⚠ Too Little RAM Allocated";
            explanation = "Your active peak memory reached ~" + calculatedPeakMB + " MB. To prevent lag spikes, allocate exactly §e" + targetMB + " MB§7.";
        } else {
            statusSummary = "§e⚠ Too Much RAM Allocated";
            explanation = "Your pack is well-optimized! You have extra unused RAM allocated. Trim down to §e" + targetMB + " MB§7 for better Garbage Collection stability.";
        }

        return new DiagnosticReport(
                currentAllocatedMB,
                calculatedPeakMB,
                targetMB,
                statusSummary,
                explanation
        );
    }
}
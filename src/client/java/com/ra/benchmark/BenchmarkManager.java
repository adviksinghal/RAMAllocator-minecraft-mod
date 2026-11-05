package com.ra.benchmark;

import com.ra.client.RAMAdvisorClient;
import com.ra.engine.RAMAdvisorEngine;
import com.ra.engine.RAMAdvisorEngine.DiagnosticReport;
import com.ra.state.TestState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkManager {
    private static int tickCounter = 0;
    private static int totalSecondsElapsed = 0;
    private static boolean registered = false;

    private static long peakBaselineMemoryMB = 0;
    private static final List<Long> gameplayHeapSamples = new ArrayList<>();

    public static void register() {
        if (registered) return;
        registered = true;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            TestState state = RAMAdvisorClient.getCurrentState();
            if (state == TestState.IDLE || client.isPaused()) {
                return;
            }

            tickCounter++;
            if (tickCounter >= 20) {
                tickCounter = 0;
                totalSecondsElapsed++;
                processSecond(client);
            }
        });
    }

    public static void resetTimers() {
        tickCounter = 0;
        totalSecondsElapsed = 0;
        peakBaselineMemoryMB = 0;
        gameplayHeapSamples.clear();
    }

    private static void processSecond(Minecraft client) {
        TestState state = RAMAdvisorClient.getCurrentState();
        Runtime runtime = Runtime.getRuntime();
        long currentUsedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        if (state == TestState.STRESS_TEST) {
            int remaining = 30 - totalSecondsElapsed;
            if (currentUsedMB > peakBaselineMemoryMB) {
                peakBaselineMemoryMB = currentUsedMB;
            }

            client.gui.hud.setOverlayMessage(
                    Component.literal("§e[Stress Test] Time remaining: " + remaining + "s"),
                    false
            );

            if (totalSecondsElapsed >= 30) {
                tickCounter = 0;
                totalSecondsElapsed = 0;
                RAMAdvisorClient.setCurrentState(TestState.GAMEPLAY_TEST);

                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal("§a[RAMAdvisor] Step 1 Complete!"));
                    client.player.sendSystemMessage(Component.literal("§b[RAMAdvisor] Step 2 Started: Play normally for 10 minutes!"));
                }
            }
        }
        else if (state == TestState.GAMEPLAY_TEST) {
            gameplayHeapSamples.add(currentUsedMB);

            int remainingSeconds = 600 - totalSecondsElapsed;
            int mins = remainingSeconds / 60;
            int secs = remainingSeconds % 60;

            client.gui.hud.setOverlayMessage(
                    Component.literal(String.format("§b[Gameplay Test] Time remaining: %d:%02d", mins, secs)),
                    false
            );

            if (client.player != null) {
                if (totalSecondsElapsed == 150) {
                    client.player.sendSystemMessage(Component.literal("§7[RAMAdvisor] Diagnostic Check-in: 25% complete!"));
                } else if (totalSecondsElapsed == 300) {
                    client.player.sendSystemMessage(Component.literal("§7[RAMAdvisor] Diagnostic Check-in: 50% complete!"));
                } else if (totalSecondsElapsed == 450) {
                    client.player.sendSystemMessage(Component.literal("§7[RAMAdvisor] Diagnostic Check-in: 75% complete!"));
                }
            }

            if (totalSecondsElapsed >= 600) {
                completeTest(client);
            }
        }
    }

    private static void completeTest(Minecraft client) {
        RAMAdvisorClient.setCurrentState(TestState.IDLE);

        DiagnosticReport report = RAMAdvisorEngine.generateReport(peakBaselineMemoryMB, gameplayHeapSamples);

        if (client.player != null) {
            Component clickableLink = Component.literal("§b§nhttps://syntaxmine.com/articles/allocate-more-ram-minecraft")
                    .setStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://syntaxmine.com/articles/allocate-more-ram-minecraft")))
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7Click to open launcher guide in browser"))));

            client.player.sendSystemMessage(Component.literal("§6=========================================="));
            client.player.sendSystemMessage(Component.literal("§a§l     RAMAdvisor Diagnostics Complete!"));
            client.player.sendSystemMessage(Component.literal("§6=========================================="));
            client.player.sendSystemMessage(Component.literal("§7Status: " + report.statusSummary));
            client.player.sendSystemMessage(Component.literal("§fActive Peak Usage: §e" + report.peakUsageMB + " MB"));
            client.player.sendSystemMessage(Component.literal("§fCurrently Allocated: §e" + report.currentAllocatedMB + " MB"));
            client.player.sendSystemMessage(Component.literal("§fRecommended Target: §a" + report.targetMB + " MB §7(-Xmx" + report.targetMB + "M)"));
            client.player.sendSystemMessage(Component.literal("§6------------------------------------------"));
            client.player.sendSystemMessage(Component.literal("§7" + report.explanation));
            client.player.sendSystemMessage(Component.literal("§6------------------------------------------"));
            client.player.sendSystemMessage(Component.literal("§eNeed help changing your allocated RAM?"));
            client.player.sendSystemMessage(Component.literal("§fCheck out the step-by-step launcher guide here:"));
            client.player.sendSystemMessage(clickableLink);
            client.player.sendSystemMessage(Component.literal("§6=========================================="));
        }

        resetTimers();
    }

    public static long getPeakBaselineMemoryMB() {
        return peakBaselineMemoryMB;
    }

    public static List<Long> getGameplayHeapSamples() {
        return gameplayHeapSamples;
    }
}
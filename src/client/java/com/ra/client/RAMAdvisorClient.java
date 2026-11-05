package com.ra.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.ra.benchmark.BenchmarkManager;
import com.ra.state.TestState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.net.URI;

public class RAMAdvisorClient implements ClientModInitializer {
    private static TestState currentState = TestState.IDLE;

    @Override
    public void onInitializeClient() {
        BenchmarkManager.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            String[] aliases = {"ramadvisor", "mt", "memtest"};
            for (String alias : aliases) {
                dispatcher.register(ClientCommands.literal(alias)
                        .executes(context -> {
                            Minecraft client = Minecraft.getInstance();
                            if (client.player != null) {
                                if (currentState == TestState.IDLE) {
                                    currentState = TestState.STRESS_TEST;
                                    BenchmarkManager.resetTimers();
                                    client.player.sendSystemMessage(Component.literal("§a[RAMAdvisor] Memory Test Started: Step 1 (Baseline Stress Test - 30s)"));
                                } else {
                                    client.player.sendSystemMessage(Component.literal("§c[RAMAdvisor] A test is already running! Type /" + alias + " cancel to stop it."));
                                }
                            }
                            return 1;
                        })
                        .then(ClientCommands.literal("cancel")
                                .executes(context -> {
                                    Minecraft client = Minecraft.getInstance();
                                    if (client.player != null) {
                                        if (currentState != TestState.IDLE) {
                                            currentState = TestState.IDLE;
                                            BenchmarkManager.resetTimers();
                                            client.player.sendSystemMessage(Component.literal("§c[RAMAdvisor] Memory test cancelled."));
                                        } else {
                                            client.player.sendSystemMessage(Component.literal("§7[RAMAdvisor] No test is currently running."));
                                        }
                                    }
                                    return 1;
                                })
                        )
                        .then(ClientCommands.literal("launcher")
                                .then(ClientCommands.argument("type", StringArgumentType.word())
                                        .executes(context -> {
                                            Minecraft client = Minecraft.getInstance();
                                            if (client.player != null) {
                                                Component clickableLink = Component.literal("§b§nhttps://syntaxmine.com/articles/allocate-more-ram-minecraft")
                                                        .setStyle(Style.EMPTY
                                                                .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://syntaxmine.com/articles/allocate-more-ram-minecraft")))
                                                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7Click to open launcher guide in browser"))));

                                                client.player.sendSystemMessage(Component.literal("§6=========================================="));
                                                client.player.sendSystemMessage(Component.literal("§eNeed help changing your allocated RAM?"));
                                                client.player.sendSystemMessage(Component.literal("§fCheck out the step-by-step launcher guide here:"));
                                                client.player.sendSystemMessage(clickableLink);
                                                client.player.sendSystemMessage(Component.literal("§6=========================================="));
                                            }
                                            return 1;
                                        })
                                )
                        )
                );
            }
        });
    }

    public static TestState getCurrentState() {
        return currentState;
    }

    public static void setCurrentState(TestState state) {
        currentState = state;
    }
}
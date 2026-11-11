package com.villagersoldiers;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class VillagerSoldiers implements ModInitializer {
    public static final String MOD_ID = "villager-soldiers";

    @Override
    public void onInitialize() {
        ModItems.init();

        // /soldier spawn <type>   (op level 2)
        CommandRegistrationCallback.EVENT.register((dispatcher, ctx, env) ->
            dispatcher.register(Commands.literal("soldier")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("spawn")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> {
                            for (SoldierType t : SoldierType.values()) b.suggest(t.name().toLowerCase());
                            return b.buildFuture();
                        })
                        .executes(c -> {
                            String name = StringArgumentType.getString(c, "type").toUpperCase();
                            SoldierType type;
                            try { type = SoldierType.valueOf(name); }
                            catch (IllegalArgumentException e) {
                                c.getSource().sendFailure(Component.literal("Unknown soldier type: " + name));
                                return 0;
                            }
                            type.spawn(c.getSource().getLevel(), c.getSource().getPosition());
                            c.getSource().sendSuccess(() -> Component.literal("Spawned " + type), true);
                            return 1;
                        })))));
    }
}

package com.villagersoldiers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.VillagerRenderer;

public class VillagerSoldiersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // SoldierEntity extends Villager, so the vanilla villager renderer (incl. held items) just works.
        EntityRendererRegistry.register(ModEntities.SOLDIER, VillagerRenderer::new);
    }
}

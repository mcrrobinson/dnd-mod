// package mattonfire.dnd.classes;
// package com.example.mymod.client;

// import net.fabricmc.api.ClientModInitializer;
// import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
// import net.minecraft.client.render.entity.CreeperEntityRenderer;
// import net.minecraft.client.render.entity.EntityRendererFactory;
// import net.minecraft.client.render.entity.model.CreeperEntityModel;
// import net.minecraft.client.render.RenderLayer;
// import net.minecraft.client.render.VertexConsumerProvider;
// import net.minecraft.client.util.math.MatrixStack;
// import net.minecraft.entity.mob.CreeperEntity;
// import net.minecraft.util.Identifier;

// public class CreeperShaderMod implements ClientModInitializer {

// @Override
// public void onInitializeClient() {
// EntityRendererRegistry.register(CreeperEntity.class, new
// CustomCreeperRenderer());
// }

// private static class CustomCreeperRenderer extends CreeperEntityRenderer {
// public CustomCreeperRenderer(EntityRendererFactory.Context context) {
// super(context);
// }

// @Override
// public void render(CreeperEntity creeperEntity, float f, float g, MatrixStack
// matrixStack,
// VertexConsumerProvider vertexConsumerProvider, int i) {
// // Use a custom shader or layer
// RenderLayer customLayer = RenderLayer
// .getEyes(new Identifier("mymod", "textures/entity/creeper_shader.png"));
// // Apply the custom layer when rendering
// this.model.render(matrixStack, vertexConsumerProvider.getBuffer(customLayer),
// i,
// RenderLayer.getEyesDamageOverlay(), 1.0F, 1.0F, 1.0F, 1.0F);
// }
// }
// }

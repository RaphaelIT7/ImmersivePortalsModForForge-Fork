package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import com.qouteall.immersive_portals.render.RenderHelper;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer implements IEWorldRenderer {
    
    @Shadow
    private ClientWorld world;
    
    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;
    
    @Shadow
    @Final
    public MinecraftClient client;
    
    @Shadow
    private double lastTranslucentSortX;
    
    @Shadow
    private double lastTranslucentSortY;
    
    @Shadow
    private double lastTranslucentSortZ;
    
    @Shadow
    private BuiltChunkStorage chunks;
    
    @Shadow
    protected abstract void renderLayer(
        RenderLayer renderLayer_1,
        MatrixStack matrixStack_1,
        double double_1,
        double double_2,
        double double_3
    );
    
    @Shadow
    protected abstract void renderEntity(
        Entity entity_1,
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        MatrixStack matrixStack_1,
        VertexConsumerProvider vertexConsumerProvider_1
    );
    
    @Mutable
    @Shadow
    @Final
    private ObjectList<?> visibleChunks;
    
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDD)V"
        )
    )
    private void onRenderBeforeRenderLayer(
        WorldRenderer worldRenderer,
        RenderLayer renderLayer_1,
        MatrixStack matrixStack_1,
        double double_1,
        double double_2,
        double double_3
    ) {
        boolean isTranslucent = renderLayer_1 == RenderLayer.getTranslucent();
        if (isTranslucent) {
            CGlobal.renderer.onBeforeTranslucentRendering(matrixStack_1);
        }
        renderLayer(
            renderLayer_1, matrixStack_1,
            double_1, double_2, double_3
        );
        if (isTranslucent) {
            CGlobal.renderer.onAfterTranslucentRendering(matrixStack_1);
        }
        
    }
    
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"
        )
    )
    private void redirectClearing(int int_1, boolean boolean_1) {
        if (!CGlobal.renderer.shouldSkipClearing()) {
            RenderSystem.clear(int_1, boolean_1);
        }
    }
    
    @Redirect(
        method = "reload",
        at = @At(
            value = "NEW",
            target = "net/minecraft/client/render/BuiltChunkStorage"
        )
    )
    private BuiltChunkStorage redirectConstructingBuildChunkStorage(
        ChunkBuilder chunkBuilder_1,
        World world_1,
        int int_1,
        WorldRenderer worldRenderer_1
    ) {
        return new MyBuiltChunkStorage(
            chunkBuilder_1,
            world_1, int_1,
            worldRenderer_1
        );
    }
    
    @Inject(
        method = "renderLayer",
        at = @At("HEAD")
    )
    private void onStartRenderLayer(
        RenderLayer renderLayer_1,
        MatrixStack matrixStack_1,
        double double_1,
        double double_2,
        double double_3,
        CallbackInfo ci
    ) {
        if (CGlobal.renderer.isRendering()) {
            CGlobal.myGameRenderer.startCulling();
            if (RenderHelper.isRenderingMirror()) {
                GL11.glCullFace(GL11.GL_FRONT);
            }
        }
    }
    
    @Inject(
        method = "renderLayer",
        at = @At("TAIL")
    )
    private void onStopRenderLayer(
        RenderLayer renderLayer_1,
        MatrixStack matrixStack_1,
        double double_1,
        double double_2,
        double double_3,
        CallbackInfo ci
    ) {
        if (CGlobal.renderer.isRendering()) {
            CGlobal.myGameRenderer.endCulling();
            GL11.glCullFace(GL11.GL_BACK);
        }
    }
    
    //to let the player be rendered when rendering portal
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;isThirdPerson()Z"
        )
    )
    private boolean redirectIsThirdPerson(Camera camera) {
        if (CGlobal.renderer.shouldRenderPlayerItself()) {
            return true;
        }
        return camera.isThirdPerson();
    }
    
    //render player itself when rendering portal
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderEntity(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"
        )
    )
    private void redirectRenderEntity(
        WorldRenderer worldRenderer,
        Entity entity_1,
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        MatrixStack matrixStack_1,
        VertexConsumerProvider vertexConsumerProvider_1
    ) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        if (entity_1 == camera.getFocusedEntity()) {
            if (CGlobal.renderer.shouldRenderPlayerItself()) {
                CGlobal.myGameRenderer.renderPlayerItself(() -> {
                    renderEntity(
                        entity_1,
                        double_1, double_2, double_3,
                        float_1,
                        matrixStack_1, vertexConsumerProvider_1
                    );
                });
                return;
            }
        }
        
        renderEntity(
            entity_1,
            double_1, double_2, double_3,
            float_1,
            matrixStack_1, vertexConsumerProvider_1
        );
    }
    
    //avoid render glowing entities when rendering portal
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;isGlowing()Z"
        )
    )
    private boolean doNotRenderGlowingWhenRenderingPortal(Entity entity) {
        if (CGlobal.renderer.isRendering()) {
            return false;
        }
        return entity.isGlowing();
    }
    
    private static boolean isReloadingOtherWorldRenderers = false;
    
    //reload other world renderers when the main world renderer is reloaded
    @Inject(method = "reload", at = @At("TAIL"))
    private void onReload(CallbackInfo ci) {
        ClientWorldLoader clientWorldLoader = CGlobal.clientWorldLoader;
        WorldRenderer this_ = (WorldRenderer) (Object) this;
        if (isReloadingOtherWorldRenderers) {
            return;
        }
        if (CGlobal.renderer.isRendering()) {
            return;
        }
        if (clientWorldLoader.getIsLoadingFakedWorld()) {
            return;
        }
        if (this_ != MinecraftClient.getInstance().worldRenderer) {
            return;
        }
        
        isReloadingOtherWorldRenderers = true;
        
        for (WorldRenderer worldRenderer : clientWorldLoader.worldRendererMap.values()) {
            if (worldRenderer != this_) {
                worldRenderer.reload();
            }
        }
        isReloadingOtherWorldRenderers = false;
    }
    
    //avoid translucent sort while rendering portal
    @Redirect(
        method = "renderLayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/RenderLayer;getTranslucent()Lnet/minecraft/client/render/RenderLayer;"
        )
    )
    private RenderLayer redirectGetTranslucent() {
        if (CGlobal.renderer.isRendering()) {
            return null;
        }
        return RenderLayer.getTranslucent();
    }
    
    @Inject(method = "renderSky", at = @At("HEAD"))
    private void onRenderSkyBegin(MatrixStack matrixStack_1, float float_1, CallbackInfo ci) {
        if (RenderHelper.isRenderingMirror()) {
            GL11.glCullFace(GL11.GL_FRONT);
        }
    }
    
    @Inject(method = "renderSky", at = @At("RETURN"))
    private void onRenderSkyEnd(MatrixStack matrixStack_1, float float_1, CallbackInfo ci) {
        GL11.glCullFace(GL11.GL_BACK);
    }
    
    @Override
    public EntityRenderDispatcher getEntityRenderDispatcher() {
        return entityRenderDispatcher;
    }
    
    @Override
    public BuiltChunkStorage getBuiltChunkStorage() {
        return chunks;
    }
    
    @Override
    public ObjectList getVisibleChunks() {
        return visibleChunks;
    }
    
    @Override
    public void setVisibleChunks(ObjectList l) {
        visibleChunks = l;
    }
}

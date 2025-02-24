// Date: 30-6-2015 15:38:53
// Template version 1.1
// Java generated by Techne
// Keep in mind that you still need to fill in some blanks
// - ZeuX

package me.desht.pneumaticcraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import me.desht.pneumaticcraft.client.util.RenderUtils;
import me.desht.pneumaticcraft.common.minigun.Minigun;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Used in three different places:
 * 1. Drone's minigun (in DroneMinigunLayer)
 * 2. ISTER for the minigun item (RenderItemMinigun)
 * 3. Sentry turrent TER model (SentryTurretRenderer)
 */
public class ModelMinigun extends Model {
    //fields
    private final ModelPart barrel;
    private final ModelPart support1;
    private final ModelPart support2;
    private final ModelPart support3;
    private final ModelPart support4;
    private final ModelPart support5;
    private final ModelPart main;
    private final ModelPart magazine;
    private final ModelPart mount;
    private final ModelPart magazineColor;

    private static final String BARREL = "barrel";
    private static final String SUPPORT1 = "support1";
    private static final String SUPPORT2 = "support2";
    private static final String SUPPORT3 = "support3";
    private static final String SUPPORT4 = "support4";
    private static final String SUPPORT5 = "support5";
    private static final String MAIN = "main";
    private static final String MAGAZINE = "magazine";
    private static final String MOUNT = "mount";
    private static final String MOUNT_R1 = "mount_r1";
    private static final String MOUNT_R2 = "mount_r2";
    private static final String MAGAZINECOLOR = "magazineColor";

    public ModelMinigun(ModelPart root) {
        super(RenderType::entityCutoutNoCull);

        barrel = root.getChild(BARREL);
        support1 = root.getChild(SUPPORT1);
        support2 = root.getChild(SUPPORT2);
        support3 = root.getChild(SUPPORT3);
        support4 = root.getChild(SUPPORT4);
        support5 = root.getChild(SUPPORT5);
        main = root.getChild(MAIN);
        magazine = root.getChild(MAGAZINE);
        mount = root.getChild(MOUNT);
        magazineColor = root.getChild(MAGAZINECOLOR);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild(BARREL, CubeListBuilder.create().texOffs(30, 15)
                        .addBox("barrel_0", -0.5F, 1.4333F, -4.0F, 1, 1, 20, 0, 3),
                PartPose.offset(0.0F, 20.9667F, -8.0F));
        partdefinition.addOrReplaceChild(SUPPORT1, CubeListBuilder.create().texOffs(0, 0)
                        .addBox("support1_0", -1.5F, -1.5F, -5.0F, 3, 3, 4, 22, 16)
                        .addBox("support1_1", -1.5F, -1.5F, 0.0F, 3, 3, 1, 0, 13)
                        .addBox("support1_2", -1.5F, -1.5F, 5.0F, 3, 3, 1, 0, 13)
                        .addBox("support1_3", -1.5F, -1.5F, 7.0F, 3, 3, 1, 0, 13)
                        .addBox("support1_4", -1.5F, -1.5F, 13.0F, 3, 3, 1, 0, 13),
                PartPose.offset(0.0F, 21.0F, -6.0F));
        partdefinition.addOrReplaceChild(SUPPORT2, CubeListBuilder.create().texOffs(0, 4)
                        .addBox("support2_0", -1.5F, 1.5F, -5.0F, 3, 1, 4, 22, 8)
                        .addBox("support2_1", -1.5F, 1.5F, 0.0F, 3, 1, 1, 0, 11)
                        .addBox("support2_2", -1.5F, 1.5F, 5.0F, 3, 1, 1, 0, 21)
                        .addBox("support2_3", -1.5F, 1.5F, 7.0F, 3, 1, 1, 0, 21)
                        .addBox("support2_4", -1.5F, 1.5F, 13.0F, 3, 1, 1, 0, 21),
                PartPose.offset(0.0F, 21.0F, -6.0F));
        partdefinition.addOrReplaceChild(SUPPORT3, CubeListBuilder.create().texOffs(0, 6)
                        .addBox("support3_0", -1.5F, -2.5F, -5.0F, 3, 1, 4, 22, 8)
                        .addBox("support3_1", -1.5F, -2.5F, 0.0F, 3, 1, 1, 0, 11)
                        .addBox("support3_2", -1.5F, -2.5F, 5.0F, 3, 1, 1, 0, 11)
                        .addBox("support3_3", -1.5F, -2.5F, 7.0F, 3, 1, 1, 0, 11)
                        .addBox("support3_4", -1.5F, -2.5F, 13.0F, 3, 1, 1, 0, 11),
                PartPose.offset(0.0F, 21.0F, -6.0F));
        partdefinition.addOrReplaceChild(SUPPORT4, CubeListBuilder.create().texOffs(0, 8)
                        .addBox("support4_0", 1.5F, -1.5F, -5.0F, 1, 3, 4, 22, 1)
                        .addBox("support4_1", 1.5F, -1.5F, 0.0F, 1, 3, 1, 0, 17)
                        .addBox("support4_2", 1.5F, -1.5F, 5.0F, 1, 3, 1, 4, 17)
                        .addBox("support4_3", 1.5F, -1.5F, 7.0F, 1, 3, 1, 4, 17)
                        .addBox("support4_4", 1.5F, -1.5F, 13.0F, 1, 3, 1, 4, 17),
                PartPose.offset(0.0F, 21.0F, -6.0F));
        partdefinition.addOrReplaceChild(SUPPORT5, CubeListBuilder.create().texOffs(0, 11)
                        .addBox("support5_0", -2.5F, -1.5F, -5.0F, 1, 3, 4, 32, 1)
                        .addBox("support5_1", -2.5F, -1.5F, 0.0F, 1, 3, 1, 0, 17)
                        .addBox("support5_2", -2.5F, -1.5F, 5.0F, 1, 3, 1, 0, 17)
                        .addBox("support5_3", -2.5F, -1.5F, 7.0F, 1, 3, 1, 4, 17)
                        .addBox("support5_4", -2.5F, -1.5F, 13.0F, 1, 3, 1, 4, 17),
                PartPose.offset(0.0F, 21.0F, -6.0F));
        partdefinition.addOrReplaceChild(MAIN, CubeListBuilder.create().texOffs(36, 0)
                        .addBox("main_0", 0.0F, 0.0F, 1.0F, 6, 6, 8, 36, 18)
                        .addBox("main_1", 0.5F, 0.5F, 0.0F, 5, 5, 10, 34, 3),
                PartPose.offset(-3.0F, 18.0F, 8.0F));
        partdefinition.addOrReplaceChild(MAGAZINE, CubeListBuilder.create().texOffs(0, 14)
                        .addBox("magazine_0", -8.0F, -2.0F, 2.5F, 5, 6, 5, 0, 0)
                        .mirror(),
                PartPose.offset(3.0F, 22.0F, 9.0F));
        PartDefinition mount = partdefinition.addOrReplaceChild(MOUNT, CubeListBuilder.create().texOffs(0, 23),
                PartPose.offset(-1.0F, 15.0F, 11.0F));
        mount.addOrReplaceChild(MOUNT_R1, CubeListBuilder.create().texOffs(0, 23)
                        .addBox("mount_r1_0", -0.5F, -3.0F, -0.75F, 1, 2, 1, 60, 0),
                PartPose.offsetAndRotation(1.0F, 2.0F, 2.0F, -0.3927F, 0.0F, 0.0F));
        mount.addOrReplaceChild(MOUNT_R2, CubeListBuilder.create().texOffs(0, 23)
                        .addBox("mount_r2_0", -1.0F, -5.0F, -1.0F, 2, 1, 3, 54, 3)
                        .addBox("mount_r2_1", -1.0F, -4.0F, 0.0F, 2, 6, 2, 56, 18),
                PartPose.offsetAndRotation(1.0F, 2.0F, 2.0F, -0.2618F, 0.0F, 0.0F));
        partdefinition.addOrReplaceChild(MAGAZINECOLOR, CubeListBuilder.create().texOffs(8, 0)
                        .addBox("magazineColor_0", -9.6F, -2.0F, 2.0F, 1, 2, 4, 54, 7)
                        .mirror(),
                PartPose.offset(4.3F, 22.5F, 10.0F));

        return LayerDefinition.create(meshdefinition, 64, 32);
    }


    public void renderMinigun(PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, Minigun minigun, float partialTick, boolean renderMount) {
        VertexConsumer builder = buffer.getBuffer(RenderType.entityCutout(Textures.MODEL_DRONE_MINIGUN));
        matrixStack.pushPose();

        if (renderMount) {
            matrixStack.pushPose();
            matrixStack.translate(0, 5 / 16D, -12 / 16D);
            mount.render(matrixStack, builder, combinedLight, combinedOverlay);
            matrixStack.popPose();
        }

        float barrelRotation = 0;
        if (minigun != null) {
            barrelRotation = minigun.getOldMinigunRotation() + partialTick * (minigun.getMinigunRotation() - minigun.getOldMinigunRotation());
            float yaw = minigun.oldMinigunYaw + partialTick * Minigun.clampYaw(minigun.minigunYaw - minigun.oldMinigunYaw);
            float pitch = minigun.oldMinigunPitch + partialTick * (minigun.minigunPitch - minigun.oldMinigunPitch);

            matrixStack.translate(0, 23 / 16D, 0);
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(yaw));
            matrixStack.mulPose(Vector3f.XP.rotationDegrees(pitch));
            matrixStack.translate(0, -18 / 16D, -12 / 16D);
        }
        barrel.yRot = 0;
        barrel.xRot = 0;
        for (int i = 0; i < 6; i++) {
            barrel.zRot = (float) (Math.PI / 3 * i) + barrelRotation;
            barrel.render(matrixStack, builder, combinedLight, combinedOverlay);
        }
        support1.zRot = barrelRotation;
        support2.zRot = barrelRotation;
        support3.zRot = barrelRotation;
        support4.zRot = barrelRotation;
        support5.zRot = barrelRotation;
        support1.render(matrixStack, builder, combinedLight, combinedOverlay);
        support2.render(matrixStack, builder, combinedLight, combinedOverlay);
        support3.render(matrixStack, builder, combinedLight, combinedOverlay);
        support4.render(matrixStack, builder, combinedLight, combinedOverlay);
        support5.render(matrixStack, builder, combinedLight, combinedOverlay);
        magazine.render(matrixStack, builder, combinedLight, combinedOverlay);
        main.render(matrixStack, builder, combinedLight, combinedOverlay);

        float[] cols = RenderUtils.decomposeColorF(minigun != null ? 0xFF000000 | minigun.getAmmoColor() : 0xFF313131);
        magazineColor.render(matrixStack, builder, combinedLight, combinedOverlay, cols[1], cols[2], cols[3], cols[0]);

        matrixStack.popPose();
    }

    public void setRotationAngle(ModelPart modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }

    @Override
    public void renderToBuffer(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {
    }
}

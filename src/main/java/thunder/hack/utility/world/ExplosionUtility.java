package thunder.hack.utility.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.explosion.Explosion;
import org.apache.commons.lang3.mutable.MutableInt;
import thunder.hack.injection.accesors.IExplosion;
import thunder.hack.utility.math.PredictUtility;

import java.util.Objects;

import static thunder.hack.features.modules.Module.mc;

public final class ExplosionUtility {

    public static boolean terrainIgnore = false;
    public static Explosion explosion;

    public static float getAutoCrystalDamage(Vec3d crystalPos, PlayerEntity target, int predictTicks, boolean optimized) {
        if (predictTicks == 0) return getExplosionDamage(crystalPos, target, optimized);
        else
            return getExplosionDamageWPredict(crystalPos, target, PredictUtility.predictBox(target, predictTicks), optimized);
    }

    public static float getSelfExplosionDamage(Vec3d explosionPos, int predictTicks, boolean optimized) {
        return getAutoCrystalDamage(explosionPos, mc.player, predictTicks, optimized);
    }

    public static float getExplosionDamage(Vec3d explosionPos, PlayerEntity target, boolean optimized) {
        if (mc.world.getDifficulty() == Difficulty.PEACEFUL || target == null) return 0f;

        if (explosion == null)
            explosion = new Explosion(mc.world, mc.player, 1f, 33f, 7f, 6f, false, Explosion.DestructionType.DESTROY);

        ((IExplosion) explosion).setX(explosionPos.x);
        ((IExplosion) explosion).setY(explosionPos.y);
        ((IExplosion) explosion).setZ(explosionPos.z);

        if (((IExplosion) explosion).getWorld() != mc.world) ((IExplosion) explosion).setWorld(mc.world);

        if (!new Box(MathHelper.floor(explosionPos.x - 11), MathHelper.floor(explosionPos.y - 11), MathHelper.floor(explosionPos.z - 11), MathHelper.floor(explosionPos.x + 13), MathHelper.floor(explosionPos.y + 13), MathHelper.floor(explosionPos.z + 13)).intersects(target.getBoundingBox()))
            return 0f;

        if (!target.isImmuneToExplosion(explosion) && !target.isInvulnerable()) {
            double distExposure = (float) target.squaredDistanceTo(explosionPos) / 144.;
            if (distExposure <= 1.0) {
                terrainIgnore = false;
                double exposure = getExposure(explosionPos, target.getBoundingBox(), optimized);
                double finalExposure = (1.0 - distExposure) * exposure;

                float toDamage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2. * 7. * 12. + 1.);

                if (mc.world.getDifficulty() == Difficulty.EASY) toDamage = Math.min(toDamage / 2f + 1f, toDamage);
                else if (mc.world.getDifficulty() == Difficulty.HARD) toDamage = toDamage * 3f / 2f;

                toDamage = DamageUtil.getDamageLeft(target, toDamage, ((IExplosion) explosion).getDamageSource(), (float) target.getArmor(), (float) target.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

                if (target.hasStatusEffect(StatusEffects.RESISTANCE)) {
                    int resistance = 25 - (target.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1) * 5;
                    float resistance_1 = toDamage * resistance;
                    toDamage = Math.max(resistance_1 / 25f, 0f);
                }

                if (toDamage <= 0f) toDamage = 0f;
                else {
                    float protAmount = 32f;
                    if (protAmount > 0)
                        toDamage = DamageUtil.getInflictedDamage(toDamage, protAmount);
                }
                return toDamage;
            }
        }
        return 0f;
    }

    public static float getExplosionDamageWPredict(Vec3d explosionPos, PlayerEntity target, Box predict, boolean optimized) {
        if (mc.world.getDifficulty() == Difficulty.PEACEFUL || target == null || predict == null) return 0f;

        if (explosion == null)
            explosion = new Explosion(mc.world, mc.player, 1f, 33f, 7f, 6f, false, Explosion.DestructionType.DESTROY);

        ((IExplosion) explosion).setX(explosionPos.x);
        ((IExplosion) explosion).setY(explosionPos.y);
        ((IExplosion) explosion).setZ(explosionPos.z);

        if (((IExplosion) explosion).getWorld() != mc.world) ((IExplosion) explosion).setWorld(mc.world);

        if (!new Box(MathHelper.floor(explosionPos.x - 11d), MathHelper.floor(explosionPos.y - 11d), MathHelper.floor(explosionPos.z - 11d), MathHelper.floor(explosionPos.x + 13d), MathHelper.floor(explosionPos.y + 13d), MathHelper.floor(explosionPos.z + 13d)).intersects(predict))
            return 0f;

        if (!target.isImmuneToExplosion(explosion) && !target.isInvulnerable()) {
            double distExposure = predict.getCenter().add(0, -0.9, 0).squaredDistanceTo(explosionPos) / 144.;
            if (distExposure <= 1.0) {
                terrainIgnore = false;
                double exposure = getExposure(explosionPos, predict, optimized);
                double finalExposure = (1.0 - distExposure) * exposure;

                float toDamage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * 12d + 1.0);

                if (mc.world.getDifficulty() == Difficulty.EASY) toDamage = Math.min(toDamage / 2f + 1f, toDamage);
                else if (mc.world.getDifficulty() == Difficulty.HARD) toDamage = toDamage * 3f / 2f;

                toDamage = DamageUtil.getDamageLeft(target, toDamage, ((IExplosion) explosion).getDamageSource(), (float) target.getArmor(), (float) Objects.requireNonNull(target.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)).getValue());

                if (toDamage <= 0f) toDamage = 0f;
                else {
                    float protAmount = 32f;
                    if (protAmount > 0) toDamage = DamageUtil.getInflictedDamage(toDamage, protAmount);
                }
                return toDamage;
            }
        }
        return 0f;
    }

    // Hàm này rất quan trọng cho InteractionUtility.java
    public static BlockHitResult rayCastBlock(RaycastContext context, BlockPos block) {
        return BlockView.raycast(context.getStart(), context.getEnd(), context, (raycastContext, blockPos) -> {
            BlockState blockState = blockPos.equals(block) ? Blocks.OBSIDIAN.getDefaultState() : Blocks.AIR.getDefaultState();
            VoxelShape voxelShape = raycastContext.getBlockShape(blockState, mc.world, blockPos);
            BlockHitResult blockHitResult = mc.world.raycastBlock(raycastContext.getStart(), raycastContext.getEnd(), blockPos, voxelShape, blockState);
            VoxelShape voxelShape2 = VoxelShapes.empty();
            BlockHitResult blockHitResult2 = voxelShape2.raycast(raycastContext.getStart(), raycastContext.getEnd(), blockPos);
            double d = blockHitResult == null ? Double.MAX_VALUE : raycastContext.getStart().squaredDistanceTo(blockHitResult.getPos());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : raycastContext.getStart().squaredDistanceTo(blockHitResult2.getPos());
            return d <= e ? blockHitResult : blockHitResult2;
        }, (raycastContext) -> {
            Vec3d vec3d = raycastContext.getStart().subtract(raycastContext.getEnd());
            return BlockHitResult.createMissed(raycastContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(raycastContext.getEnd()));
        });
    }

    public static float getExposure(Vec3d source, Box box, boolean optimized) {
        int miss = 0, hit = 0;
        for (int k = 0; k <= 1; k++) {
            for (int l = 0; l <= 1; l++) {
                for (int m = 0; m <= 1; m++) {
                    Vec3d vec3d = new Vec3d(MathHelper.lerp(k, box.minX, box.maxX), MathHelper.lerp(l, box.minY, box.maxY), MathHelper.lerp(m, box.minZ, box.maxZ));
                    if (raycast(vec3d, source, false) == HitResult.Type.MISS) miss++;
                    hit++;
                    if (optimized) break; // Chỉ check 1 điểm nếu optimized
                }
            }
        }
        return (float) miss / (float) hit;
    }

    public static float getExposure(Vec3d source, Box box) {
        return getExposure(source, box, false);
    }

    private static BlockHitResult raycastGhost(RaycastContext context, BlockPos bPos) {
        return BlockView.raycast(context.getStart(), context.getEnd(), context, (innerContext, pos) -> {
            BlockState blockState = pos.equals(bPos) ? Blocks.OBSIDIAN.getDefaultState() : mc.world.getBlockState(pos);
            VoxelShape voxelShape = innerContext.getBlockShape(blockState, mc.world, pos);
            return mc.world.raycastBlock(innerContext.getStart(), innerContext.getEnd(), pos, voxelShape, blockState);
        }, innerContext -> {
            Vec3d vec3d = innerContext.getStart().subtract(innerContext.getEnd());
            return BlockHitResult.createMissed(innerContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(innerContext.getEnd()));
        });
    }

    public static HitResult.Type raycast(Vec3d start, Vec3d end, boolean ignoreTerrain) {
        return BlockView.raycast(start, end, null, (innerContext, blockPos) -> {
            BlockState blockState = mc.world.getBlockState(blockPos);
            if (blockState.getBlock().getBlastResistance() < 600 && ignoreTerrain) return null;
            BlockHitResult hitResult = blockState.getCollisionShape(mc.world, blockPos).raycast(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
        }, (innerContext) -> HitResult.Type.MISS);
    }

    public static int getProtectionAmount(Iterable<ItemStack> equipment) {
        MutableInt mutableInt = new MutableInt();
        equipment.forEach(i -> mutableInt.add(getProtectionAmount(i)));
        return mutableInt.intValue();
    }

    public static int getProtectionAmount(ItemStack stack) {
        if (stack.isEmpty() || mc.world == null) return 0;
        // Fix Enchantment chuẩn cho bản mới
        int modifierBlast = EnchantmentHelper.getLevel(mc.world.getRegistryManager().get(Enchantments.BLAST_PROTECTION.getRegistryRef()).getEntry(Enchantments.BLAST_PROTECTION).get(), stack);
        int modifier = EnchantmentHelper.getLevel(mc.world.getRegistryManager().get(Enchantments.PROTECTION.getRegistryRef()).getEntry(Enchantments.PROTECTION).get(), stack);
        return modifierBlast * 2 + modifier;
    }
}

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

        if (((IExplosion) explosion).getWorld() != mc.world)
            ((IExplosion) explosion).setWorld(mc.world);

        if (!new Box(
                explosionPos.x - 11, explosionPos.y - 11, explosionPos.z - 11,
                explosionPos.x + 13, explosionPos.y + 13, explosionPos.z + 13
        ).intersects(target.getBoundingBox())) return 0f;

        if (!target.isImmuneToExplosion(explosion) && !target.isInvulnerable()) {
            double dist = target.squaredDistanceTo(explosionPos) / 144.0;
            if (dist <= 1.0) {
                double exposure = getExposure(explosionPos, target.getBoundingBox(), optimized);
                double finalExposure = (1.0 - dist) * exposure;

                float damage = (float) ((finalExposure * finalExposure + finalExposure) * 7.0 * 12.0 / 2.0 + 1.0);

                if (mc.world.getDifficulty() == Difficulty.EASY)
                    damage = Math.min(damage / 2f + 1f, damage);
                else if (mc.world.getDifficulty() == Difficulty.HARD)
                    damage *= 1.5f;

                damage = DamageUtil.getDamageLeft(
                        target,
                        damage,
                        ((IExplosion) explosion).getDamageSource(),
                        target.getArmor(),
                        (float) Objects.requireNonNull(
                                target.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS)
                        ).getValue()
                );

                if (target.hasStatusEffect(StatusEffects.RESISTANCE)) {
                    int amp = Objects.requireNonNull(target.getStatusEffect(StatusEffects.RESISTANCE)).getAmplifier();
                    damage *= (1f - (amp + 1) * 0.2f);
                }

                if (damage > 0) {
                    damage = DamageUtil.getInflictedDamage(damage, getProtectionAmount(target.getArmorItems()));
                }

                return Math.max(damage, 0f);
            }
        }
        return 0f;
    }

    public static float getExplosionDamageWPredict(Vec3d pos, PlayerEntity target, Box predict, boolean optimized) {
        if (mc.world.getDifficulty() == Difficulty.PEACEFUL || target == null || predict == null) return 0f;

        if (explosion == null)
            explosion = new Explosion(mc.world, mc.player, 1f, 33f, 7f, 6f, false, Explosion.DestructionType.DESTROY);

        ((IExplosion) explosion).setX(pos.x);
        ((IExplosion) explosion).setY(pos.y);
        ((IExplosion) explosion).setZ(pos.z);

        if (!new Box(
                pos.x - 11, pos.y - 11, pos.z - 11,
                pos.x + 13, pos.y + 13, pos.z + 13
        ).intersects(predict)) return 0f;

        double dist = predict.getCenter().squaredDistanceTo(pos) / 144.0;
        if (dist > 1.0) return 0f;

        double exposure = getExposure(pos, predict, optimized);
        double finalExposure = (1.0 - dist) * exposure;

        float damage = (float) ((finalExposure * finalExposure + finalExposure) * 7.0 * 12.0 / 2.0 + 1.0);
        damage = DamageUtil.getInflictedDamage(damage, getProtectionAmount(target.getArmorItems()));
        return Math.max(damage, 0f);
    }

    public static float getExposure(Vec3d source, Box box, boolean optimized) {
        int hit = 0, miss = 0;

        for (int x = 0; x <= 1; x++)
            for (int y = 0; y <= 1; y++)
                for (int z = 0; z <= 1; z++) {
                    Vec3d p = new Vec3d(
                            MathHelper.lerp(x, box.minX, box.maxX),
                            MathHelper.lerp(y, box.minY, box.maxY),
                            MathHelper.lerp(z, box.minZ, box.maxZ)
                    );
                    if (raycast(p, source, false) == HitResult.Type.MISS) miss++;
                    hit++;
                }

        return hit == 0 ? 0f : (float) miss / hit;
    }

    public static HitResult.Type raycast(Vec3d start, Vec3d end, boolean ignoreTerrain) {
        return BlockView.raycast(start, end, null,
                (ctx, pos) -> {
                    BlockState state = mc.world.getBlockState(pos);
                    if (ignoreTerrain && state.getBlock().getBlastResistance() < 600) return null;
                    return state.getCollisionShape(mc.world, pos).raycast(start, end, pos);
                },
                ctx -> HitResult.Type.MISS
        );
    }

    public static int getProtectionAmount(Iterable<ItemStack> armor) {
        MutableInt total = new MutableInt();
        armor.forEach(i -> total.add(getProtectionAmount(i)));
        return total.intValue();
    }

    public static int getProtectionAmount(ItemStack stack) {
        int blast = EnchantmentHelper.getLevel(Enchantments.BLAST_PROTECTION, stack);
        int prot = EnchantmentHelper.getLevel(Enchantments.PROTECTION, stack);
        return blast * 2 + prot;
    }
}

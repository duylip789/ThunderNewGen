package thunder.hack.utility.world;

import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;
import net.minecraft.world.explosion.Explosion;
import org.apache.commons.lang3.mutable.MutableInt;
import thunder.hack.injection.accesors.IExplosion;
import thunder.hack.utility.math.PredictUtility;

import java.util.Objects;

import static thunder.hack.features.modules.Module.mc;

public final class ExplosionUtility {

    public static boolean terrainIgnore = false;
    public static Explosion explosion;

    /* ================= DAMAGE ================= */

    public static float getAutoCrystalDamage(Vec3d pos, PlayerEntity target, int predict, boolean opt) {
        return predict == 0
                ? getExplosionDamage(pos, target, opt)
                : getExplosionDamageWPredict(pos, target, PredictUtility.predictBox(target, predict), opt);
    }

    public static float getExplosionDamage(Vec3d pos, PlayerEntity target, boolean opt) {
        if (mc.world.getDifficulty() == Difficulty.PEACEFUL || target == null) return 0f;

        if (explosion == null)
            explosion = new Explosion(mc.world, mc.player, 0, 0, 0, 6f, false, Explosion.DestructionType.DESTROY);

        ((IExplosion) explosion).setX(pos.x);
        ((IExplosion) explosion).setY(pos.y);
        ((IExplosion) explosion).setZ(pos.z);

        if (!target.isImmuneToExplosion(explosion) && !target.isInvulnerable()) {
            double dist = target.squaredDistanceTo(pos) / 144.0;
            if (dist > 1.0) return 0f;

            double exposure = getExposure(pos, target.getBoundingBox());
            double impact = (1.0 - dist) * exposure;

            float damage = (float) ((impact * impact + impact) * 7.0 * 12.0 / 2.0 + 1.0);

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

            damage = DamageUtil.getInflictedDamage(damage, getProtectionAmount(target.getArmorItems()));
            return Math.max(damage, 0f);
        }
        return 0f;
    }

    public static float getExplosionDamageWPredict(Vec3d pos, PlayerEntity target, Box predict, boolean opt) {
        if (target == null || predict == null) return 0f;
        double dist = predict.getCenter().squaredDistanceTo(pos) / 144.0;
        if (dist > 1.0) return 0f;

        double exposure = getExposure(pos, predict);
        double impact = (1.0 - dist) * exposure;

        float damage = (float) ((impact * impact + impact) * 7.0 * 12.0 / 2.0 + 1.0);
        return DamageUtil.getInflictedDamage(damage, getProtectionAmount(target.getArmorItems()));
    }

    /* ================= RAYCAST ================= */

    public static HitResult.Type raycast(Vec3d start, Vec3d end, boolean ignoreTerrain) {
        return BlockView.raycast(
                start,
                end,
                null,
                (ctx, pos) -> {
                    BlockState state = mc.world.getBlockState(pos);
                    if (ignoreTerrain && state.getBlock().getBlastResistance() < 600) return null;
                    return state.getCollisionShape(mc.world, pos).raycast(start, end, pos);
                },
                ctx -> HitResult.Type.MISS
        );
    }

    private static float getExposure(Vec3d src, Box box) {
        int hit = 0, miss = 0;

        for (int x = 0; x <= 1; x++)
            for (int y = 0; y <= 1; y++)
                for (int z = 0; z <= 1; z++) {
                    Vec3d p = new Vec3d(
                            MathHelper.lerp(x, box.minX, box.maxX),
                            MathHelper.lerp(y, box.minY, box.maxY),
                            MathHelper.lerp(z, box.minZ, box.maxZ)
                    );
                    if (raycast(p, src, false) == HitResult.Type.MISS) miss++;
                    hit++;
                }

        return hit == 0 ? 0f : (float) miss / hit;
    }

    /* ================= ARMOR ================= */

    public static int getProtectionAmount(Iterable<ItemStack> armor) {
        MutableInt total = new MutableInt();
        armor.forEach(i -> total.add(getProtectionAmount(i)));
        return total.intValue();
    }

    public static int getProtectionAmount(ItemStack stack) {
        int blast = EnchantmentHelper.getLevel(
                Registries.ENCHANTMENT.getEntry(Enchantments.BLAST_PROTECTION),
                stack
        );
        int prot = EnchantmentHelper.getLevel(
                Registries.ENCHANTMENT.getEntry(Enchantments.PROTECTION),
                stack
        );
        return blast * 2 + prot;
    }
}

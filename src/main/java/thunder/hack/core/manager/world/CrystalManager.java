package thunder.hack.core.manager.world;

import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.IManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CrystalManager implements IManager {

    private final Map<Integer, Long> deadCrystals = new ConcurrentHashMap<>();
    private final Map<Integer, Attempt> attackedCrystals = new ConcurrentHashMap<>();
    private final Map<BlockPos, Attempt> awaitingPositions = new ConcurrentHashMap<>();

    /* ================= ATTACK ================= */

    public void onAttack(EndCrystalEntity crystal) {
        setDead(crystal.getId(), System.currentTimeMillis());
        addAttack(crystal);
    }

    public void reset() {
        deadCrystals.clear();
        attackedCrystals.clear();
        awaitingPositions.clear();
    }

    public void update() {
        long time = System.currentTimeMillis();
        long ping = Managers.SERVER.getPing();

        deadCrystals.entrySet().removeIf(e -> time - e.getValue() > ping * 2L);
        attackedCrystals.entrySet().removeIf(e -> e.getValue().shouldRemove());
        awaitingPositions.entrySet().removeIf(e -> e.getValue().shouldRemove());
    }

    /* ================= DEAD ================= */

    public boolean isDead(Integer id) {
        return deadCrystals.containsKey(id);
    }

    public void setDead(Integer id, long deathTime) {
        deadCrystals.putIfAbsent(id, deathTime);
    }

    /* ================= ATTACK TRACK ================= */

    public boolean isBlocked(Integer id) {
        return attackedCrystals.containsKey(id);
    }

    public void addAttack(EndCrystalEntity entity) {
        attackedCrystals.compute(entity.getId(), (id, attempt) -> {
            if (attempt == null) {
                return new Attempt(System.currentTimeMillis(), entity.getPos());
            }
            return attempt;
        });
    }

    /* ================= PLACE TRACK ================= */

    public Map<BlockPos, Attempt> getAwaitingPositions() {
        return awaitingPositions;
    }

    public void confirmSpawn(BlockPos bp) {
        awaitingPositions.remove(bp);
    }

    public void addAwaitingPos(BlockPos blockPos) {
        awaitingPositions.compute(blockPos, (pos, attempt) -> {
            if (attempt == null) {
                return new Attempt(System.currentTimeMillis(), blockPos.toCenterPos());
            }
            return attempt;
        });
    }

    public boolean isPositionBlocked(BlockPos bp) {
        return awaitingPositions.containsKey(bp);
    }

    /* ================= ATTEMPT ================= */

    public class Attempt {
        long time;
        public Vec3d pos;
        float distance;

        Attempt(long time, Vec3d pos) {
            this.time = time;
            this.pos = pos;
            this.distance = (float) mc.player.squaredDistanceTo(pos);
        }

        public boolean shouldRemove() {
            return Math.abs(distance - mc.player.squaredDistanceTo(pos)) >= 1f;
        }
    }
}

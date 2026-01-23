package thunder.hack.injection;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import thunder.hack.features.modules.misc.FakePlayer;
import thunder.hack.utility.interfaces.IOtherClientPlayerEntity;

@Mixin(OtherClientPlayerEntity.class)
public class MixinOtherClientPlayerEntity extends AbstractClientPlayerEntity implements IOtherClientPlayerEntity {
    @Unique private double backUpX, backUpY, backUpZ;

    public MixinOtherClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Override
    public void resolve(Object mode) {
        // Kiểm tra FakePlayer để tránh lỗi
        if ((Object) this == FakePlayer.fakePlayer) {
            backUpY = -999;
            return;
        }

        // Lưu lại vị trí cũ (Backup)
        backUpX = getX();
        backUpY = getY();
        backUpZ = getZ();

        // ĐÃ XÓA SẠCH LOGIC:
        // Vì bạn đã xóa phần Resolver/Backtrack trong Aura nên không cần tính toán gì ở đây nữa.
        // Để trống như thế này là an toàn nhất, game sẽ dùng vị trí mặc định của Server.
    }

    @Override
    public void releaseResolver(Object mode) {
        // Khôi phục lại vị trí cũ sau khi render xong
        if (backUpY != -999) {
            setPosition(backUpX, backUpY, backUpZ);
            backUpY = -999;
        }
    }
}

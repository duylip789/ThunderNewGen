package thunder.hack.injection;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import thunder.hack.injection.interfaces.IOtherClientPlayerEntity;

@Mixin(OtherClientPlayerEntity.class)
public abstract class MixinOtherClientPlayerEntity extends AbstractClientPlayerEntity implements IOtherClientPlayerEntity {

    public MixinOtherClientPlayerEntity(ClientWorld world, com.mojang.authlib.GameProfile profile) {
        super(world, profile);
    }

    private double backUpX, backUpY = -999, backUpZ;

    @Override
    public void releaseResolver() {
        if (backUpY != -999) {
            setPosition(backUpX, backUpY, backUpZ);
            backUpY = -999;
        }
    }
}

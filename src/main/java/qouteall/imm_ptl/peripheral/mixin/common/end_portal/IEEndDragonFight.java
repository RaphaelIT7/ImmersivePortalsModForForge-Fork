package qouteall.imm_ptl.peripheral.mixin.common.end_portal;

import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EndDragonFight.class)
public interface IEEndDragonFight {
    @Accessor("needsStateScanning")
    boolean ip_getNeedsStateScanning();
    
    @Invoker("scanState")
    void ip_scanState();
}

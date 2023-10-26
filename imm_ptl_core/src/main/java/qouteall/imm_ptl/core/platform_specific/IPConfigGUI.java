package qouteall.imm_ptl.core.platform_specific;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IPConfigGUI {
    public static Screen createClothConfigScreen(Screen parent) {
        return AutoConfig.getConfigScreen(IPConfig.class, parent).get();
    }
}

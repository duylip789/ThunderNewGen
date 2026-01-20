package thunder.hack.features.modules.combat;

import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.ModuleCategory;

public class AutoAnchor extends Module {

    public AutoAnchor() {
        super("AutoAnchor", ModuleCategory.COMBAT);
        setEnabled(false);
        setDrawn(false);
    }
}

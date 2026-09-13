package com.elduin.cell_phone.platform.fabric;

//? fabric {

import com.elduin.cell_phone.CellPhone;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ModInitializer;

@Entrypoint("main")
public class FabricEntrypoint implements ModInitializer {

	@Override
	public void onInitialize() {
		CellPhone.onInitialize();
	}
}
//?}

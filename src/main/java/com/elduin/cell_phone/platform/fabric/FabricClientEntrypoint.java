package com.elduin.cell_phone.platform.fabric;

//? fabric {

import com.elduin.cell_phone.CellPhone;
import com.elduin.cell_phone.client.PhoneClient;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ClientModInitializer;

@Entrypoint("client")
public class FabricClientEntrypoint implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		CellPhone.onInitializeClient();
		PhoneClient.register();
	}

}
//?}

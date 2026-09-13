package com.elduin.cell_phone.platform.fabric.datagen;

//? fabric {
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

// The build lists this entrypoint for every mod. The phone's recipe is a plain
// JSON file, so there is nothing to generate.
public class FabricDataGeneratorEntrypoint implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
	}

}
//?}

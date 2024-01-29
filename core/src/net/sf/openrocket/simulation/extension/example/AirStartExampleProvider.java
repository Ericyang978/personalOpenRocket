package net.sf.openrocket.simulation.extension.example;

import net.sf.openrocket.plugin.Plugin;
import net.sf.openrocket.simulation.extension.AbstractSimulationExtensionProvider;

@Plugin
public class AirStartExampleProvider extends AbstractSimulationExtensionProvider {
    public AirStartExampleProvider() {
        super(AirStartExample.class, "Launch conditions", "Air-start example-hi");
    }
}
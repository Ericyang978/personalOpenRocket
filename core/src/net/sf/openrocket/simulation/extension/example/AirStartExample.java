package net.sf.openrocket.simulation.extension.example;


import net.sf.openrocket.aerodynamics.FlightConditions;
import net.sf.openrocket.simulation.SimulationConditions;
import net.sf.openrocket.simulation.SimulationStatus;
import net.sf.openrocket.simulation.exception.SimulationException;
import net.sf.openrocket.simulation.extension.AbstractSimulationExtension;
import net.sf.openrocket.simulation.listeners.AbstractSimulationListener;
import net.sf.openrocket.util.Coordinate;

/**
 * Simulation extension that launches a rocket from a specific altitude.
 */
public class AirStartExample extends AbstractSimulationExtension {

    @Override
    public void initialize(SimulationConditions conditions) throws SimulationException {
        conditions.getSimulationListenerList().add(new AirStartListener());
    }

    @Override
    public String getName() {
        return "Air-Start Example";
    }

    @Override
    public String getDescription() {
        return "Simple extension example for air-start";
    }

    public double getLaunchAltitude() {
        return config.getDouble("launchAltitude", 1000.0);
    }

    public void setLaunchAltitude(double launchAltitude) {
        config.put("launchAltitude", launchAltitude);
        fireChangeEvent();
    }

    private class AirStartListener extends AbstractSimulationListener {

        @Override
        public void startSimulation(SimulationStatus status) throws SimulationException {

            status.setRocketPosition(new Coordinate(0, 0, 0));
        }
//
//        @Override
//        public FlightConditions postFlightConditions(SimulationStatus status, FlightConditions flightConditions) throws SimulationException {
//            return status.getFlightData().;
//        }

    }
}
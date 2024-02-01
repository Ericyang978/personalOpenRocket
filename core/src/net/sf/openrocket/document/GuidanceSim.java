package net.sf.openrocket.document;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import net.sf.openrocket.simulation.*;
import net.sf.openrocket.simulation.exception.SimulationException;
import net.sf.openrocket.simulation.SimulationEngine;

import javax.swing.*;

public class GuidanceSim {

    private SimulationConditions conditions;
    private final Class<? extends SimulationEngine> simulationEngineClass = BasicEventSimulationEngineGuidance.class;
    private SimulationEngine simulator;

    //DATA variables
    private FlightData flightSummary;
    private ArrayList<SimulationStatus> allflightData;

    private Double[][] data;


    public GuidanceSim(SimulationConditions presetConditions) {

        //FUNDAMENTAL vars
        try {
            simulator = simulationEngineClass.getConstructor().newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("Cannot instantiate simulator.", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access simulator instance?! BUG!", e);
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        conditions = presetConditions.clone();

        //Variable definition
        data = new Double[10000][4];
    }

    public void runSim() throws SimulationException {
        flightSummary = simulator.simulate(conditions); //outputs flight vars, but not the important ones, also changes simulator
        allflightData = ((BasicEventSimulationEngineGuidance) simulator).getAllStatus();
        saveData();
        exportData();
    }

    public void saveData(){
        int count = 0;
        for (SimulationStatus status: allflightData){
            data[count][0] = status.getSimulationTime();
            data[count][1] = status.getRocketPosition().x;
            data[count][2] = status.getRocketPosition().y;
            data[count][3] = status.getRocketPosition().z;
            count++;
        }
    }
    public void exportData() {
        Object[] columnNames = {"simulation time",  "positionEast of Launch (m)", "position North of Launch (m)",
                "altitude(m)"};

        //suspicious, could cause problem cuz wrapping in double, not primitive type double
        JTable tableData = new JTable(data, columnNames);
        tableData.setBounds(30, 40, 200, 300);

        //Sets up table details
        JFrame frame = new JFrame();

        // Frame Title
        frame.setTitle("Guidance Sim data");

        // adding it to JScrollPane
        JScrollPane sp = new JScrollPane(tableData);
        frame.add(sp);
        // Frame Size
        frame.setSize(500, 200);
        // Frame Visible = true
        frame.setVisible(true);
    }





}

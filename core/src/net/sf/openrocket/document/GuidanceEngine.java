package net.sf.openrocket.document;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import net.sf.openrocket.aerodynamics.FlightConditions;
import net.sf.openrocket.simulation.*;
import net.sf.openrocket.simulation.exception.SimulationException;
import net.sf.openrocket.simulation.SimulationEngine;
import net.sf.openrocket.util.Coordinate;

import javax.swing.*;

public class GuidanceEngine {

    private SimulationConditions conditions;
    private final Class<? extends SimulationEngine> simulationEngineClass = BasicEventSimulationEngineGuidance.class;
    private SimulationEngine simulator;

    //DATA variables
    private FlightData flightSummary;
    private ArrayList<SimulationStatus> allflightData;

    private Double[][] data;

    private double[] altToWindMap;
    private ArrayList<Double> canard1Angle ;
    private ArrayList<Double> canard2Angle ;
    private ArrayList<Double> rollRates = new ArrayList<>();
    private ArrayList<Coordinate> rotationalAccelerations = new ArrayList<>();



    public GuidanceEngine(SimulationConditions presetConditions) {

        //FUNDAMENTAL vars
        try {
            simulator = simulationEngineClass.getConstructor().newInstance();
            //Sets a rollControl model for simulation engine, passes it in to simulation engine instance
            ((BasicEventSimulationEngineGuidance)simulator).setRollControlModel(new RollControlModel());
        } catch (InstantiationException e) {
            throw new IllegalStateException("Cannot instantiate simulator.", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access simulator instance?! BUG!", e);
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        conditions = presetConditions.clone();

        //Variable definition
        data = new Double[20000][11];

        //Canard angle trackers

    }

    public void runSim() throws SimulationException {
        flightSummary = simulator.simulate(conditions); //outputs flight vars, and runs sim
        //Retrieving data from simulator
        retrieveData();


        saveData();
        exportData();

        //Tests
        System.out.println("");
        System.out.println("BREAK!");
        System.out.println("");
    }

    /**
     * Retrieves flight data, canard 1 angle, canard 2 angle, roll rates (and more later), and rotational
     * accelerations in body frame
     */
    public void retrieveData(){
        allflightData = ((BasicEventSimulationEngineGuidance) simulator).getAllStatus();
        canard1Angle = ((BasicEventSimulationEngineGuidance) simulator).getCanard1Angle();
        canard2Angle =  ((BasicEventSimulationEngineGuidance) simulator).getCanard2Angle();
        rollRates = ((BasicEventSimulationEngineGuidance) simulator).getRollRates();
        rotationalAccelerations = ((BasicEventSimulationEngineGuidance) simulator).getRotationalAccelerations();
    }
    public void saveData(){
        int count = 0;
        for (SimulationStatus status: allflightData){
            data[count][0] = count/1.0;
            data[count][1] = status.getSimulationTime();
            data[count][2] = status.getRocketPosition().x;
            data[count][3] = status.getRocketPosition().y;
            data[count][4] = status.getRocketPosition().z;
            data[count][5] = Math.toDegrees(rollRates.get(count));
            data[count][6] = rotationalAccelerations.get(count).x;
            data[count][7] = rotationalAccelerations.get(count).y;
            data[count][8] = rotationalAccelerations.get(count).z;
            data[count][9] = Math.toDegrees(canard1Angle.get(count)); //ERRORMODE, canard1Angle not right size
            data[count][10] = Math.toDegrees(canard2Angle.get(count));



            count++;
        }
    }
    public void exportData() {
        Object[] columnNames = {"step #", "simulation time",  "positionEast of Launch (m)", "position North of Launch (m)",
                "altitude(m)", "rocket roll velocity", "Rotational acc x", "Rotational acc y",
                "Rotational acc z", "canard 1 angle", "canard 2 angle"};

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

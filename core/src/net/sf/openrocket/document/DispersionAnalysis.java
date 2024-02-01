package net.sf.openrocket.document;


import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.openrocket.aerodynamics.AerodynamicCalculator;
import net.sf.openrocket.aerodynamics.BarrowmanCalculator;
import net.sf.openrocket.aerodynamics.FlightConditions;
import net.sf.openrocket.masscalc.MassCalculator;
import net.sf.openrocket.models.atmosphere.AtmosphericModel;
import net.sf.openrocket.models.atmosphere.ExtendedISAModel;
import net.sf.openrocket.models.gravity.GravityModel;
import net.sf.openrocket.models.gravity.WGSGravityModel;
import net.sf.openrocket.models.wind.PinkNoiseWindModel;
import net.sf.openrocket.models.wind.WindModel;
import net.sf.openrocket.rocketcomponent.*;
import net.sf.openrocket.simulation.*;
import net.sf.openrocket.simulation.exception.SimulationException;
import net.sf.openrocket.simulation.listeners.SimulationListener;
import net.sf.openrocket.util.*;
import net.sf.openrocket.simulation.SimulationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;


//This file will generate an entire simulation based on inputted data.
public class DispersionAnalysis {


    //Notes:
    //How calls flow: Simulator -> simulationEngine interface, specifically BasicEventSimulationEngine,
    //-> simulationConditions ->

    /**CHANGES MADE to the following classes:
     * Simulation
     * BasicEventSimulationEngine
     * SimulationEngine
     * Recovery Device,
     * OpenRocketComponentLoader - this was to fix the parachute database not appearing mysteriously
     */

    //Fundamental variables: DO NOT CHANGE
    private SimulationConditions conditions;
    private final Class<? extends SimulationEngine> simulationEngineClass = BasicEventSimulationEngine.class;
    private SimulationEngine simulator;


    //DATA variables
    private Double[][] data;
    private int index;


    //Changable Variables (set values for simulation condition)
    private FlightData flightSummary;
    private double launchRodLength; //in meters LMAO

    /**
     * Launch rod angle >= 0, radians from vertical
     */
    private double launchRodAngle;

    /**
     * Launch rod direction, 0 = north
     */
    double launchRodDirection;

    // Launch site location (lat, lon, alt)
    private WorldCoordinate launchSite = new WorldCoordinate(28.6, -80.6, 0); //EDIT CORDS HERE


    //Wind Speed
    private double averageWindSpeed;
    private double windSpeedDeviation;
    private double windDirection;
    /**
     * 0 = north, pi/2 = east, pi = south, 3pi/2 = west
     */

    private double windTurbulence;
    private double[] altToWindMap;
    private double[] altToWindDirectionMap;


    private PinkNoiseWindModel windModel;


    /* Whether to calculate additional data or only primary simulation figures */
    private int randomSeed;

    //ROCKET VARS

    private ArrayList<RecoveryDevice> recoveryDevices;
    private double deploymentTimeDrogue;
    private double depolymentAltitudeDrogue;

    private double deploymentTimeMain;
    private double depolymentAltitudeMain;


    /**
     * Constructor, sets up the simulationEngineClass, simulatorEngine (which actually runs the sim),
     * All SimulationCondition values, and some user set values for simulationConditions like wind speed
     */
    public DispersionAnalysis(SimulationConditions presetConditions) {

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


        //DATA, values can change depending on how many iterations occur
        data = new Double[1000][12 + 1]; //plus 1 adds an extra row for index, 11 params, 6 output 6 input

        //CHANGEABLE vars:

        //BASIC vars
        randomSeed = new Random().nextInt();

        //LAUNCH Vars
        launchRodLength = 1;
        launchRodAngle = 0;
        launchRodDirection = 0;

        //WIND Vars
        averageWindSpeed = 5;
        windSpeedDeviation = 0;
        windDirection = 0;
        windTurbulence = 0;

        int windMapSize = 500; //Determines how many intervals are in altToWindMap and altToWindMapDirection

        altToWindMap = new double[windMapSize]; //asumes intervals of 100, alt = i*100
        altToWindDirectionMap = new double[windMapSize]; //represents the wind direction at certain altitude


        //initialize wind model, and set values
        windModel = new PinkNoiseWindModel(randomSeed);
        windModel.setAverage(averageWindSpeed);
        windModel.setStandardDeviation(windSpeedDeviation);
        windModel.setDirection(windDirection);
        windModel.setTurbulenceIntensity(windTurbulence);

        //ROCKET VARS
        deploymentTimeDrogue = 50;
        depolymentAltitudeDrogue = 15000;

        deploymentTimeMain = 50;
        depolymentAltitudeMain = 15000;

        recoveryDevices = new ArrayList<>();
        // finds the recover devices using recursive search
        findRecoveryDevices(conditions.getSimulation().getRocket());



    }

    //This determines the parameters to be looped over
    public void loopSim() throws SimulationException {

        for (int i = 0; i <20; i++) {
            configSimConditions();
            runSimulation();

            randomSeed = new Random().nextInt();
            deploymentTimeMain += 5;


            //associates the 4 wind speed parameters to the wind model
            setWindModel();
        }

        exportData(); //after data is collected, it is exported
    }


    //configures simulation conditions for single sim
    //TODO: Use debug to find values for variables in SimulationOptions.java
    //TODO: NOTE, USE Application.getpreferences() to retrieve the user's input!!
    public void configSimConditions() throws SimulationException {

        //Configures rocket
        configRocket();
        //ITERABLE PARAMETERS

        conditions.setLaunchRodLength(launchRodLength);
        conditions.setLaunchRodAngle(launchRodAngle);
        conditions.setLaunchRodDirection(launchRodDirection);
        conditions.setLaunchSite(launchSite);


//        conditions.setGeodeticComputation(geodeticComputation);
        conditions.setRandomSeed(randomSeed);

        boolean variableWind = false; // boolean determines if variable wind model is used. Default is avg wind model
        if (variableWind) {
            setVariableWindModel(); //defines instance variable int[] wind
            //casts simulator to basic event simulator, and calls method to update wind model in engine to variable wind model
            ((BasicEventSimulationEngine) simulator).setAltToWind(altToWindMap, altToWindDirectionMap);
        } else {
            conditions.setWindModel(windModel); //assuming normal wind, just set wind model directly
            /**
             * Note, this is allowed because per simulation, wind speed is constant. Therefore, the conditions object doesn't
             need to change during a single simulation. However, for variable wind speed, the condition's average wind speed
             needs to change over the simulation, which requires editing it directly in the Basic Simulation engine, hence
             the different notations for doing variable wind vs. constant wind
             */
        }

    }

    //CONFIGURES ROCKET. Most important determines delay time of recovery devices
    public void configRocket() {
        //Gets  object and its type (Parachute, streamer, something)
        // Gets simulation -> rocket -> Sustainer -> Body (multiple, must pick correct) -> inner tube
        //NOTE: this sequence of .getChild(int n) is arbitrary, depends on what the rocket tree looks like (can figure this out
        //just by looking at the tree on the GUI, or using debug in intellij and looking down the inheritance.

        //one parachute, assume main
        if (recoveryDevices.size() ==1) {
            recoveryDevices.get(0).setDeploymentTime(deploymentTimeMain);

        }
        //Two parachutes, one main one drouge, need to figure out which is which
        else {
            //the main will be bigger. if .get(0) is bigger, then .get(0) is main
            if (((Parachute) recoveryDevices.get(0)).getDiameter() > ((Parachute) recoveryDevices.get(1)).getDiameter()) {
                (recoveryDevices.get(0)).setDeploymentTime(deploymentTimeMain);
                (recoveryDevices.get(1)).setDeploymentTime(deploymentTimeDrogue);

            } else {
                (recoveryDevices.get(0)).setDeploymentTime(deploymentTimeDrogue);
                (recoveryDevices.get(1)).setDeploymentTime(deploymentTimeMain);

            }
        }


    }

    public void findRecoveryDevices(RocketComponent r){
        //base case 1: it is a recovery device
        if (r instanceof RecoveryDevice){
            recoveryDevices.add((RecoveryDevice) r);
            return;
        }
        //base case 2: it is null
        if (r == null){
            return;
        }
        for (int i =0; i < r.getChildCount(); i++){
            findRecoveryDevices(r.getChild(i));
        }

    }

    //Runs one single simulation
    public void runSimulation() throws SimulationException {
        flightSummary = simulator.simulate(conditions); //outputs flight vars, but not the important ones, also changes simulator
        saveData();
    }

    //saves data into dataTable
    public void saveData() {
        data[index][0] = index / 1.0;  //saves index, /1.0 casts to double
        data[index][1] = averageWindSpeed;
        data[index][2] = windSpeedDeviation;
        data[index][3] = windDirection;
        data[index][4] = windTurbulence;
        data[index][5] = deploymentTimeDrogue;
        data[index][6] = deploymentTimeMain;

        //Full flight data contains all positions, velocities, accelerations, etc. during a flight
        ArrayList<SimulationStatus> allflightData = ((BasicEventSimulationEngine) simulator).getAllStatus();
        //We only care about final position, so get only position in last entry of flight data
        data[index][7] = allflightData.get(allflightData.size() - 1).getRocketPosition().x;
        data[index][8] = allflightData.get(allflightData.size() - 1).getRocketPosition().y;
        data[index][9] = allflightData.get(allflightData.size() - 1).getRocketPosition().z;
        data[index][10] = flightSummary.getMaxAltitude();
        data[index][11] = flightSummary.getDeploymentVelocity();
        data[index][12] = flightSummary.getGroundHitVelocity();
        index++;
    }


    public void exportData() {
        Object[] columnNames = {"number", "avg Wind speed", "windSpeed standard dev", "wind direction", "wind Turbulence",
                "deploymentTime Drogue", "deploymentTime Main", "positionEast of Launch (m)", "position North of Launch (m)",
                "altitude(m)", "Max altitude", "deployment Velocity", "groundSpeedVelocity"};

        //suspicious, could cause problem cuz wrapping in double, not primitive type double
        JTable tableData = new JTable(data, columnNames);
        tableData.setBounds(30, 40, 200, 300);

        //Sets up table details
        JFrame frame = new JFrame();

        // Frame Title
        frame.setTitle("Sim data");

        // adding it to JScrollPane
        JScrollPane sp = new JScrollPane(tableData);
        frame.add(sp);
        // Frame Size
        frame.setSize(500, 200);
        // Frame Visible = true
        frame.setVisible(true);
    }

    //Streamlines setting up the wind model, avoids a lot of needless lines of code doing
    //windModel.setAverage(windModel.getAverage+=.5);
    public void setWindModel() {
        windModel.setAverage(averageWindSpeed);
        windModel.setStandardDeviation(windSpeedDeviation);
        windModel.setDirection(windDirection);
        windModel.setTurbulenceIntensity(windTurbulence);
    }

    //makes the array which represents the map from altitude to wind speed, used if variable wind speed mode is desired
    public void setVariableWindModel() {
        for (int i = 0; i < altToWindMap.length; i++) {
            altToWindMap[i] = 5;
            altToWindDirectionMap[i] = 0;
        }
//
//        for (int i=0; i < altToWindDirectionMap.length; i++){
//                altToWindDirectionMap[i] = Math.PI/2;
//        }

    }
}
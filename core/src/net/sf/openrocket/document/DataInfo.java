package net.sf.openrocket.document;


//Class that will store flight, conditions, and rocket data to later be evaluated


import net.sf.openrocket.aerodynamics.FlightConditions;
import net.sf.openrocket.simulation.SimulationStatus;
import net.sf.openrocket.util.Coordinate;

import java.util.ArrayList;

public class DataInfo {

    //Instance variables
    private ArrayList<SimulationStatus> fullFlightStatus = new ArrayList<>(); //used to store all flight conditions every time step

    private ArrayList<FlightConditions> allFlightConds = new ArrayList<>();

    private ArrayList<Double> allCanard1Angle = new ArrayList<>(); //used to track canard 1 angle over flight
    private ArrayList<Double> allCanard2Angle = new ArrayList<>(); //used to track canard 2 angle over flight

    private ArrayList<Coordinate> allRotationalAccelerations = new ArrayList<>();


    private ArrayList<Double> allRolls = new ArrayList<>();


    //Generate default values
    public void addDefault(){
        allFlightConds.add(null);
        allRotationalAccelerations.add(new Coordinate(-100, -100, -100));
        allCanard1Angle.add(Math.toRadians(-1));
        allCanard2Angle.add(Math.toRadians(-1));
        allRolls.add(Math.toRadians(-1));
    }


    //adders
    public void addFlightStatus(SimulationStatus flightStatus) {
        this.fullFlightStatus.add(flightStatus);
    }


    public void addFlightConds(FlightConditions flightConds) {
        this.allFlightConds.add(flightConds);
    }


    public void addCanard1Angle(Double canard1Angle) {
        this.allCanard1Angle.add(canard1Angle);
    }


    public void addCanard2Angle(Double canard2Angle) {
        this.allCanard2Angle.add(canard2Angle);
    }

    public void addRotationalAccelerations(Coordinate rotationalAccelerations) {
        this.allRotationalAccelerations.add(rotationalAccelerations);
    }

    public void addRoll(double roll) {
        this.allRolls.add(roll);
    }

    //Getters

    public ArrayList<SimulationStatus> getFullFlightStatus() {
        return fullFlightStatus;
    }

    public ArrayList<FlightConditions> getAllFlightConds() {
        return allFlightConds;
    }


    public ArrayList<Double> getCanard1Angle() {
        return allCanard1Angle;
    }


    public ArrayList<Double> getCanard2Angle() {
        return allCanard2Angle;
    }


    public ArrayList<Coordinate> getRotationalAccelerations() {
        return allRotationalAccelerations;
    }

    public ArrayList<Double> getAllRolls(){
        return allRolls;
    }



}

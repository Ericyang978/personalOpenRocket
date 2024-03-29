package net.sf.openrocket.document;

import net.sf.openrocket.simulation.SimulationStatus;
import net.sf.openrocket.util.MathUtil;

import java.util.ArrayList;


public class RollControlModel {

    private ArrayList<Integer> integratedError;
    private double startTime = 0.5; //When controller will start to control roll

    private double roll = 0;
    private double prevTime = 0; //used to determine roll and to find integral error
    private double prevRollRate = 0;  //used to determine roll
    private double intState = 0; //integral error
    private double finPosition = 0; //starting fin position

    //Coefficients
    private double KP = 1;
    private double KI = -0.0001;
    private double KD = -1;

    private double setPointRate = 0 ;//roll rate of 0

    private double setPointRoll = Math.toRadians(90) ;//Roll is defined as 90 degrees in flight

    private double maxFinRate = Math.toRadians(10);  //maximium rate of change of fin
    private double maxFinAngle = Math.toRadians(15); //maximium fin angle

    //Data variable
    private ArrayList<Double> rollArray = new ArrayList<>();

    public RollControlModel(){}

    //Calculates roll by integrating roll rate
    public void roll(SimulationStatus status, double rollRate){
        double deltaT = status.getSimulationTime() - prevTime;
        double avgRollRate = (prevRollRate + rollRate)/2;
        roll += deltaT*avgRollRate;

        prevRollRate = rollRate;
        //updated previous simulation time is done in another method since sim time is still needed there

    }

    public void PIDControl(SimulationStatus status, double rollRate){

        roll(status,rollRate);


        // Determine time step
        double deltaT = status.getSimulationTime() - prevTime;
        prevTime = status.getSimulationTime();

        // PID controller
        double error = setPointRoll - roll;

        double p = KP * error;
        double d = KD * rollRate;

        intState += error * deltaT;
        double i = KI* intState;

        double value = p + i + d;

        //Stops control once within accetable distance of correct roll
//        if (Math.abs(roll-setPointRoll) < Math.toRadians(3)) { // if roll is close enough, focus on minimizing roll rate
//            value = 0;
//        }
        if (status.getSimulationTime()< startTime){
            finPosition = 0;
        } else if (finPosition < value) {    // Limit the fin turn rate
            finPosition = Math.min(finPosition + maxFinRate * deltaT, value);
        } else {
            finPosition = Math.max(finPosition - maxFinRate * deltaT, value);
        }

        // Clamp the fin angle between bounds
        if (Math.abs(finPosition) > maxFinAngle) {
            System.err.printf("Attempting to set angle %.1f at t=%.3f, clamping.\n",
                    finPosition * 180 / Math.PI, status.getSimulationTime());
            finPosition = MathUtil.clamp(finPosition, -maxFinAngle, maxFinAngle);
        }

    }



    public void RollVelocityControl(SimulationStatus status, double rollRate){

        // Determine time step
        double deltaT = status.getSimulationTime() - prevTime;
        prevTime = status.getSimulationTime();

        // PID controller
        double error = setPointRate - rollRate;

        double p = KP * error;
        intState += error * deltaT;
        double i = KI* intState;

        double value = p + i;

        // Limit the fin turn rate
        if (finPosition < value) {
            finPosition = Math.min(finPosition + maxFinRate * deltaT, value);
        } else {
            finPosition = Math.max(finPosition - maxFinRate * deltaT, value);
        }

        // Clamp the fin angle between bounds
        if (Math.abs(finPosition) > maxFinAngle) {
            System.err.printf("Attempting to set angle %.1f at t=%.3f, clamping.\n",
                    finPosition * 180 / Math.PI, status.getSimulationTime());
            finPosition = MathUtil.clamp(finPosition, -maxFinAngle, maxFinAngle);
        }
    }

    public double getFinPosition(){
        return finPosition;
    }

    //Get methods
    public double getStartTime(){
        return startTime;
    }

    public double getRoll(){return roll;}




}

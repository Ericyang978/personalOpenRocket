package net.sf.openrocket.document;

import net.sf.openrocket.simulation.SimulationStatus;
import net.sf.openrocket.util.MathUtil;

import java.util.ArrayList;


public class RollControlModel {

    private ArrayList<Integer> integratedError;
    private double startTime; //When controller will start to control roll

    private double roll = 0;
//    private double prevTime = 0; //used to determine roll and to find integral error
    private double prevRollRate = 0;  //used to determine roll
    private double intState = 0; //integral error
    private double finPosition = 0; //starting fin position

    //Coefficients
//    private double KP = 1;
//    private double KI = -0.0001;
//    private double KD = -1;

//    private double KP = .1;
//    private double KI = 0;
//    private double KD = 0;

    private double KP = .233;
    private double KI = -0.00679;
    private double KD = -1.02573*Math.pow(10, -6);
    private double setPointRate = 0 ;//roll rate of 0

    private double setPointRoll;//Roll is defined as 90 degrees in flight

    private double maxFinRate = Math.toRadians(69)/(18.0/60);  //maximium rate of change of fin, based on video angle and frames shot at 60 fps
    private double maxFinAngle = Math.toRadians(15); //maximium fin angle

    private double clockSpeed = 10.0/1000; //clock speed

    private double prevTime = 0;
    private double deltaT = 0; //General deltaT, updated every time and used for roll

    private double prevControlTime; //based on last time electronics were used, must at least be greater than clocktime from current time

    //Data variable
    private ArrayList<Double> rollArray = new ArrayList<>();

    public RollControlModel(){}

    //Calculates roll by integrating roll rate
    public void roll(SimulationStatus status, double rollRate){
        deltaT = status.getSimulationTime() - prevTime;
        double avgRollRate = (prevRollRate + rollRate)/2;
        roll += deltaT*avgRollRate;

        prevRollRate = rollRate;
        prevTime = status.getSimulationTime();
        //updated previous simulation time is done in another method since sim time is still needed there

    }



    public void PIDControl(SimulationStatus status, double rollRate){

//        roll(status,rollRate);

        //Before roll control starts, no need for controller to work
        if (status.getSimulationTime()< startTime){
            finPosition = 0;
            return;
        }
        //only actives roll control every 10 miliseconds (roughly, to match clock speed)
        if (Math.floor(( status.getSimulationTime()-deltaT)/clockSpeed) == Math.floor(status.getSimulationTime()/clockSpeed)){
            return;
        }


        // Determine time step

        double deltaT =  status.getSimulationTime() - prevControlTime;
        prevControlTime = status.getSimulationTime();

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
        if (finPosition < value) {    // Limit the fin turn rate
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
        double deltaT = status.getPreviousTimeStep();

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
    public double getSetPointRoll(){return setPointRoll; }

    //Set methods

    public void setStartTime(double startTime){
        this.startTime = startTime;
        this.prevControlTime = startTime;

    }

    public void setSetPointRoll(double setPointRoll){this.setPointRoll = setPointRoll; }

    public double getRoll(){return roll;}
    public double getIntState(){return intState;}



    //UPDATE coefficients
    public void updateKP(double dKP){
        KP += dKP;
    }
    public void updateKD(double dKD){
        KD += dKD;

    }
    public void updateKI(double dKI){
        KI += dKI;
        if (KI < -10000000){
            System.out.println("BRUH " + dKI);
        }
    }

    public double getKP(){
        return KP;
    }
    public double getKD(){
        return KD;
    }
    public double getKI(){
        return KI;
    }

    //resets RollControlModel for new run
    public void resetRollControlModel(){
        roll = 0;
        prevRollRate = 0;  //used to determine roll
        intState = 0; //integral error
        finPosition = 0; //starting fin position
    }


}

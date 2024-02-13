package net.sf.openrocket.document;

import net.sf.openrocket.simulation.SimulationStatus;
import net.sf.openrocket.util.MathUtil;

import java.util.ArrayList;


public class RollControlModel {

    private ArrayList<Integer> integratedError;
    private double startTime = 0.5;

    private double prevTime = 0;
    private double intState = 0;
    private double finPosition = 0;

    //Coefficients
    private double KP = 0.07;
    private double KI = 0.2;

    private double setPoint = 0 ;//role rate of 0

    private double maxFinRate = Math.toRadians(10);  //maximium rate of change of fin
    private double maxFinAngle = Math.toRadians(10); //maximium fin angle

    public RollControlModel(){}

    public void PIDControl(double simTime, double prevSimTime,  double currRollVel, double currRollAcc){
//        System.out.println("roll rate is " + currRollVel);



    }

    public void sampleRollControl(SimulationStatus status, double rollRate){
        if (status.getSimulationTime() < getStartTime()) {
            prevTime = status.getSimulationTime();
            return;
        }

        // Determine time step
        double deltaT = status.getSimulationTime() - prevTime;
        prevTime = status.getSimulationTime();

        // PID controller
        double error = setPoint - rollRate;

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




}

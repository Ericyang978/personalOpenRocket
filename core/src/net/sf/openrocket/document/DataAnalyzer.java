package net.sf.openrocket.document;

import net.sf.openrocket.aerodynamics.FlightConditions;
import net.sf.openrocket.simulation.SimulationStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

// Takes flight data from one simulation, agregates the important data about the run.
public class DataAnalyzer {

    //Data varaibles
    private DataInfo datainfo;
    private ArrayList<Double> simulationTimes = new ArrayList<>();
    private ArrayList<Double> rollArr = new ArrayList<>();
    private ArrayList<Double> rollRate = new ArrayList<>();
    private ArrayList<Double> velocity = new ArrayList<>();


    //Metrics
    private double metric_integralError;
    private ArrayList<Double> error_arr = new ArrayList<>();
    private ArrayList<Double> error_arr_percentage = new ArrayList<>();

    /**
     *     includes the percentages for negative percentage values, 0 and then positive percentage values
     *     index 0-99 represents negative error [-100 to -1]
     *     index 100 represents error of 0
     *     index 101-200 represents positive error [1-100]
     */
    private double[] speed_metric = new double[201];

    private double maxOvershootPercent_metric; //maximinium overshoot
    private int numOsciliations_metric = 0; // number of times

    private double steadyStatePercent_metric = 0;


    public DataAnalyzer(DataInfo datainfo){
        this.datainfo = datainfo;
    }


    public void analyze(RollControlModel rollModel){
        extractImportantValues(rollModel.getStartTime());
        defineErrorArray(rollModel.getSetPointRoll());
        determineSpeedMetrics(rollModel.getStartTime());


        determineError_Percentage_Metrics();

        metric_integralError = rollModel.getIntState();
    }

    public void defineErrorArray(double setPointRoll){
        error_arr = (ArrayList<Double>) rollArr.stream().map(i -> i- setPointRoll).collect(Collectors.toList());
        error_arr_percentage = (ArrayList<Double>) error_arr.stream().map(i -> i/setPointRoll*100).collect(Collectors.toList());
    }

    /**
     * Determines amount of time required to reach certain percentage of total roll
     */

    public void determineSpeedMetrics(double startTime){

        speed_metric= Arrays.stream(speed_metric).map(i -> Integer.MAX_VALUE).toArray(); //changes the default vakue of the array
        //speed_metric, an array of 100 elements, were each index represents error percentage and the corresponding
        //value represents minimium time to reach that error
        for (int i = 0; i < error_arr_percentage.size(); i ++){

            int index = (int)Math.round(error_arr_percentage.get(i)) + 100;

            /**
             *  for loop is used to check past, unused values in case they exist.
             * For example, if theres an error of -97%, but no error of -96%, then that means the time step
             * skipped -96% so the earliest time associated with -97% is also given to -96% error.
             */
            for(int j = index; j >= 0; j--){
                //checks if that index is empty yet (if not, value is filled already)
                if (speed_metric[j] == Integer.MAX_VALUE ) {
                    speed_metric[j] = simulationTimes.get(i) - startTime;
                } else{
                    break;
                }
            }
        }
    }

    /**
     * Determines max overshoot, osciliations, steady state error,
     */
    public void determineError_Percentage_Metrics(){
        double max = Integer.MIN_VALUE;
        boolean positive = false;

        int finalOsciliationIndex = 0; //determines when the final osciliation occurs, this allows for steady state to be determined

        for (int i = 0; i < error_arr_percentage.size(); i ++){
            //Max overshoot
            if(error_arr_percentage.get(i) > max){
                max = error_arr_percentage.get(i);
            }

            //Osciliations

            //If roll error percentageis greater than 0, and previously roll was !positive i.e. negative
            if(error_arr_percentage.get(i) > 0 && !positive){
                numOsciliations_metric +=1;
                positive = true;
                finalOsciliationIndex = i;
            } else if (error_arr_percentage.get(i) < 0 && positive){
                numOsciliations_metric +=1;
                positive = false;
                finalOsciliationIndex = i;
            }
        }

        maxOvershootPercent_metric = max;

        //Stady state- finds when roll rate stabilizes after osciliations
        boolean steadyReached = false;
        for (int i = finalOsciliationIndex; i < error_arr_percentage.size(); i ++){
            if (Math.abs(rollRate.get(i)) < 0.25){
                steadyReached = true;
            }
            if(steadyReached){
                //finds accumulated sum of errors
                steadyStatePercent_metric += error_arr_percentage.get(i);
            }
        }
        //finds average percentage error

        //Prevents case when oscilitation cross at the end of the simulation run
        if(error_arr_percentage.size() != finalOsciliationIndex+1){
            steadyStatePercent_metric = steadyStatePercent_metric/(error_arr_percentage.size()-finalOsciliationIndex-1);
        }
        else{
            steadyStatePercent_metric = error_arr_percentage.get(error_arr_percentage.size()-1);
        }
    }



    /**
     * Gets simulation time data, roll and roll rate
     */
    public void extractImportantValues(double startTime){
//        for (SimulationStatus status: datainfo.getFullFlightStatus()){
//            simulationTimes.add(status.getSimulationTime());
//        }
//        for (FlightConditions cond: datainfo.getAllFlightConds()){
//            rollRate.add(cond.getRollRate());
//        }
//        rollArr = datainfo.getAllRolls();
        //extracts all values after roll rate
        for (int i = 0; i < datainfo.getFullFlightStatus().size(); i ++){
            if (datainfo.getFullFlightStatus().get(i).getSimulationTime() >= startTime){
                simulationTimes.add(datainfo.getFullFlightStatus().get(i).getSimulationTime());
                rollRate.add(datainfo.getAllFlightConds().get(i).getRollRate());
                velocity.add(datainfo.getAllFlightConds().get(i).getVelocity());
            }
        }

        //removes the irrelevant rolls (ones before start time)
        rollArr = (ArrayList<Double>)datainfo.getAllRolls().clone();
        for(int i = 0; i < datainfo.getFullFlightStatus().size()-simulationTimes.size(); i++ ){
            rollArr.remove(0);
        }


    }

    public double[] getSpeed_metric() {
        return speed_metric;
    }

    public double getMaxOvershootPercent_metric() {
        return maxOvershootPercent_metric;
    }

    public int getNumOsciliations_metric() {
        return numOsciliations_metric;
    }

    public double getSteadyStatePercent_metric() {
        return steadyStatePercent_metric;
    }

    /**
     * Used to get rough average velocity at time of roll control. Determines this by finding time until
     * rocket reaches a specific error of target roll (like 5%), finds associated indicies in velocity array that
     * corresponds to this
     * @return
     */
    public double getAverageRocketVelocity(double percent_error){
        double endTime = speed_metric[(int)percent_error+100];
        int endIndex = 0;

        //finds the index of the time considered to be the "end" of roll control
        for(int i = 0; i < simulationTimes.size(); i ++){
            if (simulationTimes.get(i)  >= endTime){
                endIndex= i ;
                break;
            }
        }

        return (velocity.get(0) + velocity.get(endIndex))/2;
    }






}

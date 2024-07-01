package net.sf.openrocket.document;

public class Optimizer {

    //DEFINES MAX TOLERABLE VALUES
    private final double maxOvershootPercentage = 5;
//    private final int maxNumOsciliations = ;
    private final double maxSteadyStateError = Math.toRadians(0.25);  //in radians

    private final double maxSettlingTime_5 = 2;  //max settling time for percentage error 0
    private final double maxSettlingTime_20 = 1.5;  //max settling time for percentage error 0

    //DETERMINE LEARN RATE

    private final double alpha_KI = -0.00000002;
    private final double alpha_KD = - 0.0002;
    private final double alpha_KP = 0.0002;



    public void optimize(DataAnalyzer dataAnalyzer, RollControlModel rollModel){
        double dKP = dKP(dataAnalyzer);
        double dKD = dKD(dataAnalyzer);
        double dKI = dKI(dataAnalyzer);

        updateRollModel(dKP, dKD, dKI, rollModel);

    }

    public void updateRollModel(double dKP, double dKD, double dKI, RollControlModel rollModel){
        rollModel.updateKP(dKP);
        rollModel.updateKD(dKD);
        rollModel.updateKI(dKI);

    }

    public double dKP(DataAnalyzer dataAnalyzer){
        double rocketVelocity = dataAnalyzer.getAverageRocketVelocity(-0.05);

        double scaledSettling_20 = dataAnalyzer.getSpeed_metric()[-20+100]/(maxSettlingTime_20/rocketVelocity);
        double scaledSettling_5 = dataAnalyzer.getSpeed_metric()[-5+100]/(maxSettlingTime_5/rocketVelocity);
        return ((0.3)*scaledSettling_20+(0.7)*scaledSettling_5)*alpha_KP;

    }


    //Finds the change in KD based on overshoot and osciliation number
    public double dKD(DataAnalyzer dataAnalyzer){
        double scaledOvershoot = dataAnalyzer.getMaxOvershootPercent_metric()/ maxOvershootPercentage;
//        double scaledOsciliations = dataAnalyzer.getNumOsciliations_metric()/maxNumOsciliations;
        return (scaledOvershoot)*alpha_KD;

    }

    public double dKI(DataAnalyzer dataAnalyzer){
        double scaledSteadStateError = dataAnalyzer.getSteadyStatePercent_metric()/maxSteadyStateError;
        return alpha_KI*scaledSteadStateError;
    }




}

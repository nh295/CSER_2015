/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import java.util.Arrays;
import java.util.LinkedList;
import madkitdemo3.ModifyAgent.ModifyMode;
import rbsa.eoss.Result;


/**
 *
 * @author Nozomi
 */
public class AgentArm {
    private static int maxDiversity=0;
    private static double maxQuality=0;

    private double playCount;
    private double avgReward;
    private double avgDev;
    private LinkedList<Double> extremeValue = new LinkedList();
    private LinkedList<Result> resultList = new LinkedList();
    private double maxDev;
    private int solutionDiversity;
    
    private final double delta;
    private final double lambda;
    private final String name;
    private final ModifyMode mode;
    private final int window;
    
    public AgentArm(ModifyMode mode,double delta, double lambda, int window){
        reset();
        this.name = mode.toString();
        this.mode = mode;
        this.delta = delta;
        this.lambda = lambda;
        this.window = window;
    }
    
    public static int getMaxDiversity() {
        return maxDiversity;
    }

    public static void setMaxDiversity(int maxDiversity) {
        AgentArm.maxDiversity = maxDiversity;
    }

    public static double getMaxQuality() {
        return maxQuality;
    }

    public static void setMaxQuality(double maxQuality) {
        AgentArm.maxQuality = maxQuality;
    }
    
    public double getPlayCount(){
        return playCount;
    }
    
    public double getAvgReward(){
        return avgReward/getMaxQuality();
    }
    
    public double getAvgDev(){
        return avgDev;
    }
    
    public double getMaxDev(){
        return maxDev;
    }
    
    public String getName(){
        return name;
    } 
    
    public double getMaxExtremeReward(){
        double max = 0;
        for (Double extremeValue1 : extremeValue) {
            if (extremeValue1 > max) {
                max = extremeValue1;
            }
        }
        return max;
    }
    public int getSolutionDiversity() {
        return solutionDiversity/getMaxDiversity();
    }
    
    /**
     * use after every play. Will increment play count by one and update parameters
     * @param data reward received after that play
     * @return true if PH test detects change 
     */
    public void updateArm(AgentArmCredit data){
        avgReward = (playCount*avgReward+data.getInstantReward())/(AgentEvaluationCounter.getAgentEvals(mode));
        if(avgReward>getMaxQuality())
            setMaxQuality(avgReward);
        playCount=AgentEvaluationCounter.getAgentEvals(mode);
        avgDev = avgDev + (avgReward-data.getInstantReward()+delta);
        maxDev = Math.max(maxDev, avgDev);
        
        //method for extreme-value based credit assignment
        if(data.getInstantReward()>0){
            extremeValue.addLast(data.getInstantReward());
            if(extremeValue.size()>window)
                extremeValue.removeFirst();
        }
        
        //method to update solution diversity
        if(resultList.size()>1){
            for(int i=0;i<resultList.size();i++){
                solutionDiversity += computeHammingDist(resultList.get(i),data.getNewRes());
            }
        }
        if(solutionDiversity>getMaxDiversity())
            setMaxDiversity(solutionDiversity);
                
        resultList.add(data.getNewRes());
    }
    
    public boolean PHtest(){
        return((maxDev - avgDev)>lambda);
    }
    
    private int computeHammingDist(Result origRes, Result newRes){        
        boolean[] origBit = origRes.getArch().getBitString();
        boolean[] newBit  = newRes.getArch().getBitString();
        int dist = 0;
        
        for(int i=0;i<origBit.length;i++){
            if(origBit[i]!=newBit[i])
                dist++;
        }
        return dist;
    }
    
    public final void reset(){
        playCount = 0.0001;
        avgReward = 0;
        avgDev = 0;
        maxDev =0;
        extremeValue.clear();
        resultList.clear();
        solutionDiversity = 0;
    }
    
    @Override
    public boolean equals(Object obj){
        if(!obj.getClass().equals(this.getClass()))
            return false;
        AgentArm other = (AgentArm)obj;
        if(!other.name.equalsIgnoreCase(name))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import madkitdemo3.ModifyAgent.ModifyMode;
import rbsa.eoss.Result;
import rbsa.eoss.ResultCollection;
import rbsa.eoss.local.Params;

/**
 *
 * @author SEAK1
 */
public class AgentEvaluationCounter implements Serializable{
    private static AgentEvaluationCounter AEC = null;
    private static AgentEvaluationCounterData data;
    private static int archSortSaves;
    private static boolean sorterIsDead;
    private static int numIteration;
    
    
    private AgentEvaluationCounter(){
        data = new AgentEvaluationCounterData();
        archSortSaves = 0;
        sorterIsDead = false;
    }
    
    public static int getNumIter(){
        return numIteration;
    }
    
    public static void setNumIter(int i){
        numIteration = i;
    }
    
    public static AgentEvaluationCounter getInstance(){
        if(AEC==null){
            AEC = new AgentEvaluationCounter();
            return AEC;
        }
        else
            return AEC;
    }
    
    public static void addStat(ModifyMode modMode, Result refRes, Result newRes){
        data.addStat(modMode, refRes, newRes);
    }
    
    public static void resetPlay(){
        data.resetPlay();
    }
    
    public static int getTotalEvals(){
        return data.getTotalEvals();
    }
    
    public static int getAgentEvals(ModifyMode modMode){
        return data.getAgentEvals(modMode);
    }
    
    public static int getAgentPlayCount(ModifyMode modMode){
        return data.getAgentPlayCount(modMode);
    }
    
    public static HashMap<ModifyMode,Integer> getHashMap(){
        return data.getHashMap();
    }
    
    public static void incrementSPSave(){
       archSortSaves++;
    }
   
    public static int getSPSaveCount(){
       return archSortSaves;
    }
   
    public static boolean isSorterDead(){
       return sorterIsDead;
    }
   
    public static void setSorterDead(){
       sorterIsDead=true;
    }
    
    public static void saveAgentStats(int i){
        try {
            String name = "stat" + i;
            SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd--HH-mm-ss" );
            String stamp = dateFormat.format( new Date() );
            String file_path = Params.path_save_results + "\\" + name + "_" + stamp + ".rs";
            FileOutputStream file = new FileOutputStream( file_path );
            ObjectOutputStream os = new ObjectOutputStream( file );
            os.writeObject(data);
            os.close();
            file.close();
        } catch (Exception e) {
            System.out.println( e.getMessage() );
        }
    }
    
    public static AgentEvaluationCounterData loadAgentStatFromFile(String filePath )
    {
        AgentEvaluationCounterData stats;
        try {
            FileInputStream file = new FileInputStream( filePath );
            ObjectInputStream is = new ObjectInputStream( file );
            stats = (AgentEvaluationCounterData)is.readObject();
            is.close();
            file.close();
            data = stats;
            return stats;
        } catch (Exception e) {
            System.out.println( "The stats for agents is not found" );
            System.out.println( e.getMessage() );
            return null;
        }
    }
    
    public static void reset(){
        AEC = new AgentEvaluationCounter();
    }
    
    public class AgentEvaluationCounterData implements Serializable{
        
        private  HashMap<ModifyMode,Integer> agentEvals;
        private  HashMap<ModifyMode,Integer> agentPlays;
        private  HashMap<ModifyMode,ArrayList<Integer>> dominanceHistory;
        private  int totalEval;
        
        AgentEvaluationCounterData(){
            agentEvals = new HashMap();
            agentPlays = new HashMap();
            dominanceHistory =  new HashMap();
            totalEval = 0;
        }
        
        
    
        private void addEval(ModifyMode modMode){
            totalEval++;
            if(agentEvals.containsKey(modMode))
                agentEvals.put(modMode,agentEvals.get(modMode)+1);
            else
                agentEvals.put(modMode, 1);
        }
        
        private void addPlay(ModifyMode modMode){
            if(agentPlays.containsKey(modMode))
                agentPlays.put(modMode,agentPlays.get(modMode)+1);
            else
                agentPlays.put(modMode, 1);
        }
        
        public void addStat(ModifyMode modMode,Result refRes,Result newRes){
            addEval(modMode);
            addPlay(modMode);
            if(!dominanceHistory.containsKey(modMode))
                dominanceHistory.put(modMode,new ArrayList<Integer>());
            dominanceHistory.get(modMode).add(newRes.dominates(refRes));
        }
        
        public int getTotalEvals(){
            return totalEval;
        }
        
        public void resetPlay(){
            agentPlays.clear();
        }
        
        public int getAgentEvals(ModifyMode modMode){
            return agentEvals.get(modMode);
        }
        
        public int getAgentPlayCount(ModifyMode modMode){
            return agentPlays.get(modMode);
        }
        
        public HashMap<ModifyMode,Integer> getHashMap(){
            return agentEvals;
        }

    }
    
}



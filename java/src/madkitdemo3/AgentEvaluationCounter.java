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
public class AgentEvaluationCounter {
    private static AgentEvaluationCounter AEC = null;
    private static HashMap<ModifyMode,Integer> agentEvals;
    private static HashMap<ModifyMode,ArrayList<Integer>> dominanceHistory;
    private static int totalEval;
    private AgentEvaluationCounter(){
        agentEvals = new HashMap();
        dominanceHistory =  new HashMap();
        totalEval = 0;
    }
    
    public static AgentEvaluationCounter getInstance(){
        if(AEC==null)
            return new AgentEvaluationCounter();
        else
            return AEC;
    }
    
    public static void addStat(ModifyMode modMode, Result refRes, Result newRes){
        addEval(modMode);
        if(!dominanceHistory.containsKey(modMode))
            dominanceHistory.put(modMode,new ArrayList<Integer>());
        dominanceHistory.get(modMode).add(newRes.dominates(refRes));
    }
    
    private static void addEval(ModifyMode modMode){
        totalEval++;
        if(agentEvals.containsKey(modMode))
            agentEvals.put(modMode,agentEvals.get(modMode)+1);
        else
            agentEvals.put(modMode, 1);
    }
    
    public static int getTotalEvals(){
        return totalEval;
    }
    
    public static HashMap<ModifyMode,Integer> getHashMap(){
        return agentEvals;
    }
    
    public static void saveAgentStats(int i){
        try {
            String name = "stat" + i;
            SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd--HH-mm-ss" );
            String stamp = dateFormat.format( new Date() );
            String file_path = Params.path_save_results + "\\" + name + "_" + stamp + ".rs";
            FileOutputStream file = new FileOutputStream( file_path );
            ObjectOutputStream os = new ObjectOutputStream( file );
            os.writeObject(dominanceHistory);
            os.close();
            file.close();
        } catch (Exception e) {
            System.out.println( e.getMessage() );
        }
    }
    
    public HashMap<ModifyMode,ArrayList<Integer>> loadAgentStatFromFile(String filePath )
    {
        HashMap<ModifyMode,ArrayList<Integer>> stats;
        
        try {
            FileInputStream file = new FileInputStream( filePath );
            ObjectInputStream is = new ObjectInputStream( file );
            stats = (HashMap<ModifyMode,ArrayList<Integer>>)is.readObject();
            is.close();
            file.close();
            dominanceHistory = stats;
            return stats;
        } catch (Exception e) {
            System.out.println( "The stats for agents is not found" );
            System.out.println( e.getMessage() );
            return null;
        }
    }
    
    public static void resetCounter(){
        AEC = new AgentEvaluationCounter();
    }
}

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
import java.util.Set;
import madkitdemo3.ModifyAgent.ModifyMode;
import rbsa.eoss.local.Params;

/**
 *
 * @author Nozomi
 */
public class AgentSelectionHistory implements Serializable{
    private static AgentSelectionHistory ASH;
    private static ArrayList<ModifyMode> selectionHistory;
    private static int resetNum;
    private static ArrayList<Integer> resetTime;
    private static final ModifyMode[] modeOptions = ModifyMode.values();
    
    private AgentSelectionHistory(){
        resetNum = 0;
        selectionHistory = new ArrayList();
        resetTime = new ArrayList();
    }
    
    public static AgentSelectionHistory getInstance(){
        if(ASH==null){
            ASH = new AgentSelectionHistory();
            return new AgentSelectionHistory();
        }
        else
            return ASH;
    }
    
    public static void addSelection(ModifyMode mode){
        selectionHistory.add(mode);
    }
    
    public static void saveSelectionHistory(int iteration){
        try {
            String name = "DMABHistory"+Integer.toString(iteration);
            SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd--HH-mm-ss" );
            String stamp = dateFormat.format( new Date() );
            String file_path = Params.path_save_results + "\\" + name + "_" + stamp + ".rs";
            FileOutputStream file = new FileOutputStream( file_path );
            ObjectOutputStream os = new ObjectOutputStream( file );
            os.writeObject(ASH);
            os.close();
            file.close();
        } catch (Exception e) {
            System.out.println( e.getMessage() );
        }
    }
    
    public static AgentSelectionHistory loadAgentStatFromFile(String filePath )
    {
        AgentSelectionHistory history;
        try {
            FileInputStream file = new FileInputStream( filePath );
            ObjectInputStream is = new ObjectInputStream( file );
            history = (AgentSelectionHistory)is.readObject();
            is.close();
            file.close();
            ASH = history;
            return ASH;
        } catch (Exception e) {
            System.out.println( "The history for agents is not found" );
            System.out.println( e.getMessage() );
            return null;
        }
    }
    
    public static ArrayList<Integer> loadArmResets(String filePath )
    {
        ArrayList<Integer> resetTimes;
        try {
            FileInputStream file = new FileInputStream( filePath );
            ObjectInputStream is = new ObjectInputStream( file );
            resetTimes = (ArrayList<Integer>)is.readObject();
            is.close();
            file.close();
            resetTime = resetTimes;
            return resetTimes;
        } catch (Exception e) {
            System.out.println( "The number of resets is not found" );
            System.out.println( e.getMessage() );
            return null;
        }
    }
    
    public static void incResetNum(){
        resetNum++;
    }
    
    public static int getResetNum(){
        return resetNum;
    }
    
    public static void setResetTime(int time){
        resetTime.add(time);
    }
    
    public static ArrayList<Integer> getResetTimes(){
        return resetTime;
    }
    
    public static ModifyMode[] getModes(){
        return modeOptions;
    }
    
    public static void reset(){
        ASH = new AgentSelectionHistory();
    }
    
}

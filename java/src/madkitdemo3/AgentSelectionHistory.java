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
    private static SelectionData data;
    private static final ModifyMode[] modeOptions = ModifyMode.values();
    
    private AgentSelectionHistory(){
        data = new SelectionData();
    }
    
    public static AgentSelectionHistory getInstance(){
        if(ASH==null){
            ASH = new AgentSelectionHistory();
            return ASH;
        }
        else
            return ASH;
    }
    
    public static void addSelection(ModifyMode mode){
        data.addSelection(mode);
    }
    
    public static void saveSelectionHistory(int iteration){
        try {
            String name = "DMABHistory"+Integer.toString(iteration);
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
    
    public static SelectionData loadSelectionHistoryFromFile(String filePath )
    {
        SelectionData history;
        try {
            FileInputStream file = new FileInputStream( filePath );
            ObjectInputStream is = new ObjectInputStream( file );
            history = (SelectionData)is.readObject();
            is.close();
            file.close();
            data = history;
            return data;
        } catch (Exception e) {
            System.out.println( "The history for agents is not found" );
            System.out.println( e.getMessage() );
            return null;
        }
    }
    
    public static void incResetNum(){
        data.incResetNum();
    }
    
    public static int getResetNum(){
        return data.getResetNum();
    }
    
    public static void setResetTime(int time){
        data.setResetTime(time);
    }
    
    public static ArrayList<Integer> getResetTimes(){
        return data.getResetTimes();
    }
    
    public static ModifyMode[] getModes(){
        return modeOptions;
    }
    
    public static void reset(){
        ASH = new AgentSelectionHistory();
    }
    
    public class SelectionData implements Serializable{
        
        private ArrayList<ModifyMode> selectionHistory;
        private int resetNum;
        private ArrayList<Integer> resetTime;
        
        public SelectionData(){
            resetNum = 0;
            selectionHistory = new ArrayList();
            resetTime = new ArrayList();
        }
        
        public void addSelection(ModifyMode mode){
            selectionHistory.add(mode);
        }
        
        public void incResetNum(){
            resetNum++;
        }
        
        public int getResetNum(){
            return resetNum;
        }
        
        public void setResetTime(int time){
            resetTime.add(time);
        }
        
        public ArrayList<Integer> getResetTimes(){
            return resetTime;
        }
    }
    
}

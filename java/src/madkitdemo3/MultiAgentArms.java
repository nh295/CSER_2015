/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import madkitdemo3.ModifyAgent.ModifyMode;

/**
 *
 * @author Nozomi
 */
public class MultiAgentArms {
    private HashMap<String,AgentArm> arms = new HashMap();
    
    public MultiAgentArms(Collection<ModifyMode> modes,double delta, double lambda,int window){
        Iterator<ModifyMode> modeIter = modes.iterator();
        
        while(modeIter.hasNext()){
            ModifyMode mode = modeIter.next();
            arms.put(mode.toString(), new AgentArm(mode.toString(),delta,lambda,window));
        }
    }
    
    public void updateArm(ModifyMode arm,AgentArmCredit data){
        boolean reset = arms.get(arm.toString()).updateArm(data);
        if(reset){
            System.out.println(arm.toString() + " triggered PH test. Reseting all arms");
            Set<String> set = arms.keySet();
            Iterator<String> iter = set.iterator();
            while(iter.hasNext()){
                arms.get(iter.next()).reset();
            }
        }
    }
    
    public double getAvgExtremeValues(ModifyMode arm){
        return arms.get(arm.toString()).getAvgExtremeReward();
    }
    
    public double getPlayCount(ModifyMode arm){
        return arms.get(arms.toString()).getPlayCount();
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import rbsa.eoss.Architecture;
import rbsa.eoss.Result;

/**
 *
 * @author Nozomi
 */
public class AgentArmCredit {
    private final double instantReward;
    private final Result newRes;

    
    public AgentArmCredit(Result origRes, Result newRes){
        this.newRes = newRes;
        this.instantReward = computeReward(origRes,newRes);
    }

    public Result getNewRes() {
        return newRes;
    }

    public double getInstantReward() {
        return instantReward;
    }
    
    private double computeReward(Result origRes, Result newRes){
        return newRes.dominates(origRes)+1;
    }
}

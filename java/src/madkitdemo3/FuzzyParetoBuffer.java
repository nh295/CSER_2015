/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;
import static madkitdemo3.Society.*;

/**
 *
 * @author nozomihitomi
 */
public class FuzzyParetoBuffer extends BufferAgent{
    @Override
    protected void activate(){
//        setLogLevel(Level.FINEST);

        createGroupIfAbsent(COMMUNITY, aDesignTeam);
        requestRole(COMMUNITY, aDesignTeam, fuzzyParetoBuffer);
        
        this.instantiatePopulation();
    }
    
    @Override
    protected void live(){
        while(isAlive() && !endLive){
            Message mail = waitNextMessage();
            switch(mail.getSender().getRole()){
                case archSorter:
                    ArchPopulation paretoFront= (ArchPopulation)((ObjectMessage)mail).getContent();
                    setPopulation(paretoFront);
                    AgentAddress tradespaceAddress = findAgent(COMMUNITY,aDesignTeam,tradespace);
                    sendCurrentPopulationToAddress(tradespaceAddress);
                    break;
                case bestArchBuffer:
                    if(getPopulationSize()>0)
                        sendCurrentPopulationInReply(mail);
                    break;
                default: logger.warning("unsupported sender: " + mail.getSender().getRole());
            }
        }
    }
    
    protected void end(){
        System.out.println("Fuzzy Pareto Optimal Arch Buffer dying");
    }
}

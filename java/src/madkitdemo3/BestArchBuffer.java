/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import java.util.ArrayList;
import java.util.List;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;
import static madkitdemo3.Society.*;

/**
 *
 * @author nozomihitomi
 */
public class BestArchBuffer extends BufferAgent {
    
    @Override
    protected void activate(){
//        setLogLevel(Level.FINEST);

        createGroupIfAbsent(COMMUNITY, aDesignTeam);
        requestRole(COMMUNITY, aDesignTeam, bestArchBuffer);
        
        this.instantiatePopulation();
    }
    
    @Override
    protected void live(){
        ArchPopulation fuzzyParetoPopulation = new ArchPopulation();
        ArchPopulation previousBestPopulation = new ArchPopulation();
        List<ArchPopulation> newBestPopToSort = new ArrayList<>();
        
        while(isAlive() && !endLive){            
            AgentAddress paretoBufferAddress = findAgent(COMMUNITY,aDesignTeam,fuzzyParetoBuffer);
            AgentAddress archSorterAddress = findAgent(COMMUNITY,aDesignTeam,archSorter);
            
            Message mail = waitNextMessage();
            switch(mail.getSender().getRole()){
                case evaluatedBuffer:
                    //reset all populations
                    fuzzyParetoPopulation.clearPopulation();
                    
                    //get fuzzy pareto front and high scoring architectures
                    fuzzyParetoPopulation = getPopulationFromBuffer(paretoBufferAddress);
                    
                    newBestPopToSort.add(0, fuzzyParetoPopulation);
                    newBestPopToSort.add(1, previousBestPopulation);
                    
                    ObjectMessage findNewParetoMessage = new ObjectMessage(newBestPopToSort);
                    Message reply = sendMessageAndWaitForReply(archSorterAddress,findNewParetoMessage);
                    
                    setPopulation((ArchPopulation)((ObjectMessage)reply).getContent());
                    previousBestPopulation = this.getCurrentPopulation().copyPopulation();
                    
                    //send best archs to evaluated buffer so evaluatedBUffer can reset its population
                    ObjectMessage bestArchs = new ObjectMessage(getCurrentPopulation());
                    sendReply(mail,bestArchs);
//                    logger.info(Integer.toString(this.getPopulationSize()));
                    break;
                default: logger.warning("unsupported sender: " + mail.getSender().getRole());
            }
        }
    }
    
    protected void end(){
        System.out.println("Best Yet Arch Buffer dying");
    }
    
}

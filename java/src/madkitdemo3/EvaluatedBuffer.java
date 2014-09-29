/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import java.util.ArrayList;
import java.util.logging.Level;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;
import static madkitdemo3.Society.*;
import rbsa.eoss.Architecture;
import rbsa.eoss.ArchitectureEvaluator;
import rbsa.eoss.Result;

/**
 *
 * @author nozomihitomi
 */
public class EvaluatedBuffer extends BufferAgent{
    
    @Override
    protected void activate(){
//        setLogLevel(Level.FINEST);

        createGroupIfAbsent(COMMUNITY, aDesignTeam);
        requestRole(COMMUNITY, aDesignTeam, evaluatedBuffer);
        
        this.instantiatePopulation();
    }
    
    @Override
    protected void live(){
        
        while(isAlive() && !endLive){
            
            Message mail = waitNextMessage(10);
            if(mail==null){
                pause(1);
            }else if(mail.getSender().getRole().equalsIgnoreCase(modifier)){
                Object content = ((ObjectMessage)mail).getContent();
                if(content.getClass()==String.class){
                    Architecture next = getRandArch();
                    if(next==null){
                        ObjectMessage noArchReady = new ObjectMessage(null);
                        sendReply(mail,noArchReady);
                    }else{
                        ObjectMessage reply = new ObjectMessage(next);
                        sendReply(mail,reply);
                    }
                }else if(content.getClass()==Result.class){
                    Result res = ((ObjectMessage<Result>)mail).getContent();
                    addResult(res);
                    if(getPopulationSize()>=100){
                        AgentAddress paretoSorterAddress = findAgent(COMMUNITY, aDesignTeam, archSorter);
                        
                        ObjectMessage paretoSortMail = new ObjectMessage(getCurrentPopulation());
                        ObjectMessage<ArchPopulation> reply = (ObjectMessage<ArchPopulation>)sendMessageAndWaitForReply(paretoSorterAddress,paretoSortMail);
                        clearPopulation();
                        ArchPopulation paretoPopulation = reply.getContent();
                        setPopulation(paretoPopulation.copyPopulation());
                        cleanUpBuffer();
                        logger.info("cleaning buffer");
//                        logger.info("sending archs to sorter");
//                        logger.info("pop size"+Integer.toString(getPopulationSize()));
                    }
                }
            }else if(mail.getSender().getRole().equalsIgnoreCase(manager)){
                Result res = ((ObjectMessage<Result>)mail).getContent();
                addResult(res);
            }else
                logger.warning("unsupported sender: " + mail.getSender().getRole());
        }
    }
    
    protected void end(){
        System.out.println("Evaluated Arch Buffer dying");
    }
    
    private void cleanUpBuffer(){
        
        //clear population from AE
        ArchitectureEvaluator AE = ArchitectureEvaluator.getInstance();
        AE.clearResults();
    }

}
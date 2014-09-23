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
            
            Message mail = waitNextMessage();
            switch(mail.getSender().getRole()){
                case modifier:
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
                        if(getPopulationSize()>0 && getPopulationSize()%400==0){
                            AgentAddress paretoSorterAddress = findAgent(COMMUNITY, aDesignTeam, archSorter);
                            
                            ObjectMessage paretoSortMail = new ObjectMessage(getCurrentPopulation());
                            sendMessage(paretoSorterAddress,paretoSortMail);
//                        logger.info("sending archs to sorter");
//                        logger.info("pop size"+Integer.toString(getPopulationSize()));
                        }
                        if(getPopulationSize()>=400){
                            cleanUpBuffer();
                            logger.info("cleaning buffer");
                        }
                    }
                    break;
                case manager:
                    Result res = ((ObjectMessage<Result>)mail).getContent();
                    addResult(res);
                    break;
                default: logger.warning("unsupported sender: " + mail.getSender().getRole());
            }
        }
    }
    
    protected void end(){
        System.out.println("Evaluated Arch Buffer dying");
    }
    
    private void cleanUpBuffer(){
        clearPopulation();
        AgentAddress paretoBufferAddress = findAgent(COMMUNITY, aDesignTeam, bestArchBuffer);
        Message reply = sendMessageAndWaitForReply(paretoBufferAddress,new Message());
        ArchPopulation paretoPopulation = (ArchPopulation)((ObjectMessage)reply).getContent();
        setPopulation(paretoPopulation.copyPopulation());
        
        //clear population from AE
        ArchitectureEvaluator AE = ArchitectureEvaluator.getInstance();
        AE.clearResults();
    }

}
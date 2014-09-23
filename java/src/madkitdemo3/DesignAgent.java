/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import agentInterfaces.IAgent;
import java.util.logging.Level;
import java.util.logging.Logger;
import madkit.kernel.Agent;
import madkit.kernel.AgentAddress;
import madkit.message.ObjectMessage;
import rbsa.eoss.Architecture;
import rbsa.eoss.Result;

/**
 *
 * @author nozomihitomi
 */
public class DesignAgent extends Agent implements IAgent{
    
    boolean endLive;
    private int numExec;
    private double sendProb;
    
    public boolean sendArchToAgentWithRole(String receiverCommunity,String receiverGroup,String receiverRole,Architecture arch,String senderRole){            
        //look for agent to send to. If 
        AgentAddress receivingAgent = findAgent(receiverCommunity, receiverGroup, receiverRole);
        sendMessageWithRole(receivingAgent,new ObjectMessage(arch),senderRole);
        return true;
    }
    
    public boolean sendResultToAgentWithRole(String receiverCommunity,String receiverGroup,String receiverRole,Result res,String senderRole){            
        //look for agent to send to. If 
        AgentAddress receivingAgent = findAgent(receiverCommunity, receiverGroup, receiverRole);
        sendMessageWithRole(receivingAgent,new ObjectMessage(res),senderRole);
        return true;
    }
    
    /**
     * Attempts to get a random architecture from a specified buffer. If that 
     * buffer is empty or doesn't return an architecture, the agent will keep 
     * requesting for another Architecture. If that buffer doesn't exist, the 
     * agent will kill itself, terminating further execution
     * @param bufferAddress
     * @return 
     */
    public Architecture getRandArchFromBufferWithRole(AgentAddress bufferAddress){
        ObjectMessage request = new ObjectMessage("Requesting Arch");
        ObjectMessage<Architecture> reply = new ObjectMessage(null);
        boolean noArchReady = true;
        
        while(noArchReady){
            reply = (ObjectMessage)sendMessageAndWaitForReply(bufferAddress,request,500);
            if(reply!=null){
                //if reply comes back
                if(reply.getContent()!=null){
                    noArchReady = false;
                }                    
            }
            // if buffer no longer exists
            else if(reply==null && !checkAgentAddress(bufferAddress)){
                logger.info("Buffer no longer exists");
                killAgent(this);
            }
        }
        Architecture arch = reply.getContent();
        return arch;
    }
    
     /**
     * Attempts to get a a population of architectures from a specified buffer. 
     * If that buffer is empty or doesn't return an architecture, the agent will keep 
     * requesting for another Architecture. If that buffer doesn't exist, the 
     * agent will kill itself, terminating further execution
     * @param bufferAddress
     * @return 
     */
    public ArchPopulation getPopulationFromBuffer(AgentAddress bufferAddress){
        ObjectMessage request = new ObjectMessage("Requesting Population");
        ObjectMessage reply = new ObjectMessage("");
        boolean noArchReady = true;
        
        while(noArchReady){
            reply = (ObjectMessage)sendMessageAndWaitForReply(bufferAddress,request,500);
            if(reply!=null){
                //if reply comes back
                if(reply.getContent()!=null){
                    noArchReady = false;
                }                    
            }
            // if buffer no longer exists
            else if(reply==null && !checkAgentAddress(bufferAddress)){
                logger.info("Buffer no longer exists");
                killAgent(this);
            }
        }
        ArchPopulation population = (ArchPopulation)reply.getContent();
        return population;
    }
    
    public AgentAddress findAgent(String community, String group, String role){
        //method to find an agent belonging to a specific CGR
        
        AgentAddress address = null;
        while(address == null){
            address = getAgentWithRole(community, group, role);
        }
        return address;
    }
        
    public void initiateLive(){
        live();
    }
    
    public void suicide(){
        endLive=true;
    }
    
    public int getNumExec(){
        return numExec;
    }
    
    public void resetNumExec(){
        numExec=0;
    }
    
    public void incrementNumExec(){
        numExec++;
    }
    
    public double getSendProb(){
        return sendProb;
    }
    
    public void setSendProb(double sendProb){
        this.sendProb = sendProb;
    }
}

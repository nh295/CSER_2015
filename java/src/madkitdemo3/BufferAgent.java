/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import agentInterfaces.IAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;
import rbsa.eoss.Architecture;
import rbsa.eoss.Result;

/**
 *
 * @author nozomihitomi
 */
public class BufferAgent extends DesignAgent implements IAgent{
    
    private ArchPopulation population;
    
    public void instantiatePopulation(){
        population = new ArchPopulation();
    }
    
    public ArchPopulation getCurrentPopulation(){
        // creates a copy of the list of architectures
        // returns a snapshot of the current population
        final ArchPopulation copy = population.copyPopulation();
        return copy;
    }
    
    public void setPopulation(ArchPopulation population){
        this.population=population;
    }
    
    public Architecture getRandArch(){
        return population.getRandArch();
    }
    
    public Architecture getRandArchAndRemoveIt(){
        return population.getNextArchAndRemoveIt();
    }
    
    public void clearPopulation(){
        population.clearPopulation();
    }
    
    private void addArch(Architecture arch){
        population.addArch(arch);
    }
    
    public void addResult(Result res){
        Architecture arch = res.getArch();
        arch.setResult(res);
        addArch(arch);
    }
    
    public int getPopulationSize(){
        return population.getSize();
    }
    
    public void sendCurrentPopulationInReply(Message m){
        ObjectMessage populationMail = new ObjectMessage(getCurrentPopulation());
        sendReply(m,populationMail);
    }
    
    public void sendCurrentPopulationToAddress(AgentAddress recipient){
        ObjectMessage populationMail = new ObjectMessage(getCurrentPopulation());
        sendMessage(recipient,populationMail);
    }
        
    public Message sendCurrentPopulationToAddressAndWaitForReply(AgentAddress recipient){
        ObjectMessage populationMail = new ObjectMessage(getCurrentPopulation());
        Message reply = sendMessageAndWaitForReply(recipient,populationMail);
        return reply;
    }
}
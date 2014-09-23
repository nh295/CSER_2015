/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;
import madkitdemo3.ModifyAgent.ModifyMode;

import static madkitdemo3.Society.*;
import rbsa.eoss.Architecture;
import rbsa.eoss.ArchitectureEvaluator;
import rbsa.eoss.ArchitectureGenerator;
import rbsa.eoss.Result;
import rbsa.eoss.SearchPerformance;


/**
 *
 * @author nozomihitomi
 */
public class ATeamsManager extends DesignAgent{
    
    private static final Collection<AbstractAgent> searchAgents = new ArrayList<>();
    private static final Collection<AbstractAgent> bufferAgents = new ArrayList<>();
    private static final Collection<AbstractAgent> ancillaryAgents = new ArrayList<>();
    private final int populationSize = 200;
    
    @Override
    protected void activate() {
        //Everything will be automatically logged
//        setLogLevel(Level.FINEST);

        createGroupIfAbsent(COMMUNITY, aDesignTeam);
        requestRole(COMMUNITY, aDesignTeam, manager);
       
        
        // initialize buffer agents
        bufferAgents.addAll(launchAgentsIntoLive(EvaluatedBuffer.class.getName(),1,true));
        bufferAgents.addAll(launchAgentsIntoLive(FuzzyParetoBuffer.class.getName(),1));
        bufferAgents.addAll(launchAgentsIntoLive(BestArchBuffer.class.getName(),1,true));
        
        ancillaryAgents.addAll(launchAgentsIntoLive(Tradespace.class.getName(),1,true));
        ancillaryAgents.addAll(launchAgentsIntoLive(ArchSorter.class.getName(),1)); 
        
                
        //set all agents to have sendProb of 1.0
        this.setSendProb(1.0);
        initSendProb(bufferAgents);
        initSendProb(ancillaryAgents);
        initSendProb(searchAgents);
        
    }
        
    @Override
    protected void live() {

        int n = 0;
        
        //initiate population and send to unevaluated buffer
        ArrayList<Architecture> initPop = ArchitectureGenerator.getInstance().getInitialPopulation(populationSize);
        ArchitectureEvaluator AE = ArchitectureEvaluator.getInstance();
        AE.setPopulation(initPop);
        AE.evaluatePopulation();
        Stack<Result> stackRes =  AE.getResults();
        Iterator<Result> iter = stackRes.iterator();
        AgentAddress evalBufferAddress = findAgent(COMMUNITY, aDesignTeam, evaluatedBuffer);
        while(iter.hasNext()){
            ObjectMessage message = new ObjectMessage(iter.next());
            sendMessageWithRole(evalBufferAddress,message,manager);
        }
        
        
        // launch other design agents
        try {
//            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.ADDSYNERGY,ManagerMode.ATEAM));
//            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.ADDTOSMALLSAT,ManagerMode.ATEAM));
            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.ASKUSER,ManagerMode.ATEAM));
            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.BESTNEIGHBOR,ManagerMode.ATEAM));
//            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.CROSSOVER,ManagerMode.ATEAM));
//            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.IMPROVEORBIT,ManagerMode.ATEAM));
            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.MUTATION,ManagerMode.ATEAM));
//            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.RANDOMSEARCH,ManagerMode.ATEAM));
//            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.REMOVEFROMBIGSAT,ManagerMode.ATEAM));
//            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.REMOVEINTERFERENCE,ManagerMode.ATEAM));
//            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.REMOVESUPERFLUOUS,ManagerMode.ATEAM));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(ATeamsManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        logger.info("All agents initiated. Starting search...");
        
        HashMap<String,Integer> agentEval = new HashMap();
        while(!isDone(n)){            
            Message mail = waitNextMessage();
            switch(mail.getSender().getRole()){
                case modifier: 
                    //to keep count of evaluations
                    String agent = (String)((ObjectMessage)mail).getContent();
                    if(agentEval.containsKey(agent))
                        agentEval.put(agent,agentEval.get(agent)+1);
                    else
                        agentEval.put(agent, 1);
                    n++;
                    break;
                default: logger.warning("unsupported senderp: " + mail.getSender().getRole());
            }
        }
        System.out.println("Done");
        System.out.println(agentEval);
    }
        
    @Override
    protected void end(){
        killAgentsInList(searchAgents);
        killAgentsInList(bufferAgents);
        killAgentsInList(ancillaryAgents);
        
        AbstractAgent.ReturnCode returnCode = leaveRole(COMMUNITY, aDesignTeam, manager);
        if (returnCode == AbstractAgent.ReturnCode.SUCCESS){
            if(logger != null){
                logger.info("I am leaving the artificial society");
            }
        }
    }

    private boolean isDone(int n){
        int iters = 1000;
    return n>=iters;
    }
    
    private Collection<AbstractAgent> launchAgentsIntoLive(String agentClass,int n){
        Collection<AbstractAgent> agentList = new ArrayList<>();
        for(int i=1;i<=n;i++){
            agentList.add(launchAgent(agentClass,false));
        }
        logger.info("Launching "+n+ " agents of class: "+agentClass);
        return agentList;
    }
    
    private Collection<AbstractAgent> launchAgentsIntoLive(String agentClass,int n,boolean gui){
        Collection<AbstractAgent> agentList = new ArrayList<>();
        for(int i=1;i<=n;i++){
            agentList.add(launchAgent(agentClass,gui));
        }
        logger.info("Launching "+n+ " agents of class: "+agentClass);
        return agentList;
    }
    
    /**
     * If agent has a mode then use this constructor to enter mode
     * @param agentClass class name of the agent
     * @param n number of agents to launch
     * @param modMode the mode to launch the agent in
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException If no such mode exists, IllegalArguementException is thrown
     * @throws InvocationTargetException 
     */
    private Collection<AbstractAgent> launchAgentsIntoLive(Class agentClass,int n,ModifyMode modMode,ManagerMode manMode) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        Collection<AbstractAgent> agentList = new ArrayList<>();
        for(int i=1;i<=n;i++){
            Constructor cotr = agentClass.getConstructor(new Class[]{ModifyMode.class,ManagerMode.class});
            AbstractAgent agent = (AbstractAgent)cotr.newInstance(modMode,manMode);
            launchAgent(agent);
            agentList.add(agent);
        }
        logger.info("Launching "+n+ " agents of class: "+agentClass);
        return agentList;
    }
    
    private void initSendProb(Collection<AbstractAgent> agents){
        Iterator i = agents.iterator();
        while(i.hasNext()){
            DesignAgent agent = (DesignAgent)i.next();
            agent.setSendProb(1.0);
        }
    }
    
    
    private void killAgentsInList(Collection<AbstractAgent> agents){
        Iterator i = agents.iterator();
        while(i.hasNext()){
            DesignAgent agentToKill = (DesignAgent)i.next();
            agentToKill.suicide();
        }
    }
    

}

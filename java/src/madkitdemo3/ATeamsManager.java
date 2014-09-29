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
    
    private static final Collection<AbstractAgent> searchAgents = new ArrayList();
    private static final Collection<AbstractAgent> bufferAgents = new ArrayList();
    private static final Collection<AbstractAgent> ancillaryAgents = new ArrayList();
    private final int populationSize = 100;
    
    @Override
    protected void activate() {
        //Everything will be automatically logged
//        setLogLevel(Level.FINEST);

        createGroupIfAbsent(COMMUNITY, aDesignTeam);
        requestRole(COMMUNITY, aDesignTeam, manager);
       
//        ancillaryAgents.addAll(launchAgentsIntoLive(Tradespace.class.getName(),1,true));
                
        //set all agents to have sendProb of 1.0
        this.setSendProb(1.0);
        initSendProb(bufferAgents);
        initSendProb(ancillaryAgents);
        initSendProb(searchAgents);
        
    }
        
    @Override
    protected void live() {
        
        for(int i=0;i<20;i++){
            // initialize buffer agents
            bufferAgents.addAll(launchAgentsIntoLive(EvaluatedBuffer.class.getName(),1,true));
            ancillaryAgents.addAll(launchAgentsIntoLive(ArchSorter.class.getName(),1));
            AgentEvaluationCounter.getInstance();
            
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
                searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.ADDSYNERGY,ManagerMode.ATEAM));
                searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.ADDTOSMALLSAT,ManagerMode.ATEAM));
//            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.ASKUSER,ManagerMode.ATEAM));
                searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.BESTNEIGHBOR,ManagerMode.ATEAM));
                searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.CROSSOVER,ManagerMode.ATEAM));
                searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.IMPROVEORBIT,ManagerMode.ATEAM));
                searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.MUTATION,ManagerMode.ATEAM));
                searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.RANDOMSEARCH,ManagerMode.ATEAM));
                searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.REMOVEFROMBIGSAT,ManagerMode.ATEAM));
                searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.REMOVEINTERFERENCE,ManagerMode.ATEAM));
                searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.REMOVESUPERFLUOUS,ManagerMode.ATEAM));
            } catch (Exception ex) {
                Logger.getLogger(ATeamsManager.class.getName()).log(Level.SEVERE, null, ex);
            }
            logger.info("All agents initiated. Starting search...");
            
            while(!isDone(AgentEvaluationCounter.getTotalEvals())){
            }
            System.out.println("Done");
            System.out.println(AgentEvaluationCounter.getHashMap());
            AgentEvaluationCounter.saveAgentStats(i);
            
            killAgentsInList(searchAgents);
            killAgentsInList(bufferAgents);
            killAgentsInList(ancillaryAgents);
            
            while(getAgentWithRole(COMMUNITY, aDesignTeam, evaluatedBuffer)!=null)
                    pause(10);
            Iterator<AbstractAgent> agentIter = searchAgents.iterator();
            while(agentIter.hasNext()){
                AbstractAgent agent = agentIter.next();
                while(agent.isAlive())
                    killAgent(agent);
                        pause(10);
            }
            pause(5000);
        }
    }
        
    @Override
    protected void end(){
              
        AbstractAgent.ReturnCode returnCode = leaveRole(COMMUNITY, aDesignTeam, manager);
        if (returnCode == AbstractAgent.ReturnCode.SUCCESS){
            if(logger != null){
                logger.info("I am leaving the artificial society");
            }
        }
    }

    private boolean isDone(int n){
        int evals = 100;
    return n>=evals;
    }
    
    private Collection<AbstractAgent> launchAgentsIntoLive(String agentClass,int n){
        Collection<AbstractAgent> agentList = new ArrayList();
        for(int i=1;i<=n;i++){
            agentList.add(launchAgent(agentClass,false));
        }
        logger.info("Launching "+n+ " agents of class: "+agentClass);
        return agentList;
    }
    
    private Collection<AbstractAgent> launchAgentsIntoLive(String agentClass,int n,boolean gui){
        Collection<AbstractAgent> agentList = new ArrayList();
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
        Collection<AbstractAgent> agentList = new ArrayList();
        for(int i=1;i<=n;i++){
            Constructor cotr = agentClass.getConstructor(new Class[]{ModifyMode.class,ManagerMode.class});
            AbstractAgent agent = (AbstractAgent)cotr.newInstance(modMode,manMode);
            launchAgent(agent,true);
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

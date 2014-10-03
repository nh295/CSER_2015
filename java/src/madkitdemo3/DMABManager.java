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
import java.util.Random;
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
import rbsa.eoss.ResultCollection;
import rbsa.eoss.ResultManager;
import rbsa.eoss.local.Params;


/**
 *
 * @author nozomihitomi
 */
public class DMABManager extends DesignAgent{
    private final int c = 5; //scaling between exploitation and exploration
    private static final Collection<AbstractAgent> bufferAgents = new ArrayList();
    private static final Collection<AbstractAgent> ancillaryAgents = new ArrayList();
    private static Collection<AbstractAgent> searchAgents = new ArrayList();
    private final boolean continueFromHumanMode = false;
    private ResultManager RM;
    private final int populationSize = 200;
    private final int maxEvals = 5000;
    private Random rand = new Random();
    
    @Override
    protected void activate() {
        //Everything will be automatically logged
//        setLogLevel(Level.FINEST);

        createGroupIfAbsent(COMMUNITY, aDesignTeam);
        requestRole(COMMUNITY, aDesignTeam, manager);
        
//        ancillaryAgents.addAll(launchAgentsIntoLive(Tradespace.class.getName(),1,true));
        
        //set up parameters for DMAB
        ArrayList<ModifyMode> modes = new ArrayList();
        for(ModifyMode mod:ModifyMode.values()){
            modes.add(mod);
        }
        MultiAgentArms.init(modes,0.1,3,5);
        RM = ResultManager.getInstance();
    }
        
    @Override
    protected void live() {
        ArchitectureEvaluator AE = ArchitectureEvaluator.getInstance();
        AE.init(1);
        System.out.println("Evaluating Min Max");
        AE.evalMinMax();
        AE.clear();
        for(int i=0;i<10;i++){
            AE.init(3);
             
            AgentEvaluationCounter.getInstance();
            AgentSelectionHistory.getInstance();
            // initialize buffer agents
            bufferAgents.addAll(launchAgentsIntoLive(EvaluatedBuffer.class.getName(),1,true));
            ancillaryAgents.addAll(launchAgentsIntoLive(ArchSorter.class.getName(),1,true));
            //set all agents to have sendProb of 1.0
            this.setSendProb(1.0);
            initSendProb(bufferAgents);
            initSendProb(ancillaryAgents);
            
            Stack<Result> stackRes;
            if(continueFromHumanMode){
                String path = "C:\\Users\\Nozomi\\Documents\\CSER_2015\\Monica DMAB\\";
                String dmabFile = "DMABHistory0_2014-10-02--17-16-42.rs";
                String resultFile = "2014-10-02_17-15-52_test.rs";
                String statFile = "stat0_2014-10-02--17-16-42.rs";
                ResultCollection rescol = RM.loadResultCollectionFromFile(path+resultFile);
                stackRes = rescol.getResults();
                AgentEvaluationCounter.loadAgentStatFromFile(path+statFile);
                AgentSelectionHistory.loadSelectionHistoryFromFile(path+dmabFile);
            }else{
                //initiate population and send to unevaluated buffer
                ArrayList<Architecture> initPop = ArchitectureGenerator.getInstance().getInitialPopulation(populationSize);
                AE.setPopulation(initPop);
                System.out.println("Evaluating Initial Population");
                AE.evaluatePopulation();
                stackRes =  AE.getResults();
            }
            
            Iterator<Result> iter = stackRes.iterator();
            AgentAddress evalBufferAddress = findAgent(COMMUNITY, aDesignTeam, evaluatedBuffer);
            while(iter.hasNext()){
                ObjectMessage message = new ObjectMessage(iter.next());
                sendMessageWithRole(evalBufferAddress,message,manager);
            }
            
            while(!isDone(AgentEvaluationCounter.getTotalEvals())){
                ModifyMode modMode = selectOperator(AgentEvaluationCounter.getTotalEvals());
                AgentSelectionHistory.addSelection(modMode);
                MultiAgentArms.setReady(false);
                
                try {
                    searchAgents = launchAgentsIntoLive(ModifyAgent.class,1,modMode,ManagerMode.DMABBANDIT);
                } catch (Exception ex) {
                    Logger.getLogger(DMABManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                while(!MultiAgentArms.isReady()){
                    pause(1);
                    //do nothing
                }
                if(MultiAgentArms.PHtest(modMode)) //do PH test
                    AgentSelectionHistory.incResetNum();
            }
            
            purgeMailbox();
//            killAgent(bufferAgents.iterator().next(),1000);
            killAgentsInList(bufferAgents);
            waitNextMessage();
            killAgentsInList(ancillaryAgents);
//            killAgent(ancillaryAgents.iterator().next(),1000);
            pause(10000);
            
            System.out.println("Done");
            System.out.println(AgentEvaluationCounter.getHashMap());
            
            AgentEvaluationCounter.saveAgentStats(i);
            AgentSelectionHistory.saveSelectionHistory(i);
            AgentEvaluationCounter.reset();
            AgentSelectionHistory.reset();
            AE.clear();
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
    return n>=maxEvals;
    }
    
    private ModifyMode selectOperator(int totalPlays){
        ArrayList<ModifyMode> potentialModes = new ArrayList();
        HashMap<ModifyMode,Double> scores = new HashMap();
        double val = 0;
        double max = 0;
        double totalPlayCount = totalPlays;
        if(totalPlayCount == 0 || totalPlayCount==1)
            totalPlayCount = 1.000001;
        for(ModifyMode mode:ModifyMode.values()){
//            if(mode!=ModifyMode.ASKUSER&&mode!=ModifyMode.BESTNEIGHBOR){
                double p = MultiAgentArms.getAvgValues(mode);
                if(Double.isNaN(p))
                    p=0;
                val = p+c*Math.sqrt(Math.log10(totalPlayCount)/MultiAgentArms.getPlayCount(mode));
                scores.put(mode, val);
                if(val>=max){
                    max = val;
                }
//            }
        }
        for(ModifyMode mode:ModifyMode.values()){
//            if(mode==ModifyMode.ASKUSER || mode==ModifyMode.BESTNEIGHBOR)
//                break;
            if(scores.get(mode)==max){
                potentialModes.add(mode);
            }
        }
        
        //returns a random mode if there are multiple modes that maximize function
        return potentialModes.get(rand.nextInt(potentialModes.size()));
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
     * @param mode the mode to launch the agent in
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

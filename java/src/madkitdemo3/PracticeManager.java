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
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import madkit.kernel.AbstractAgent;
import madkit.kernel.AgentAddress;
import madkit.message.ObjectMessage;
import madkitdemo3.ATeamsManager;
import madkitdemo3.AgentEvaluationCounter;
import madkitdemo3.ArchSorter;
import madkitdemo3.DesignAgent;
import madkitdemo3.EvaluatedBuffer;
import madkitdemo3.ManagerMode;
import madkitdemo3.ModifyAgent;
import static madkitdemo3.Society.COMMUNITY;
import static madkitdemo3.Society.aDesignTeam;
import static madkitdemo3.Society.evaluatedBuffer;
import static madkitdemo3.Society.manager;
import rbsa.eoss.Architecture;
import rbsa.eoss.ArchitectureEvaluator;
import rbsa.eoss.ArchitectureGenerator;
import rbsa.eoss.Result;

/**
 *
 * @author SEAK1
 */
public class PracticeManager extends DesignAgent{
    
    private static final Collection<AbstractAgent> searchAgents = new ArrayList();
    private static final Collection<AbstractAgent> bufferAgents = new ArrayList();
    int maxEval = 10;
    
    @Override
    protected void activate() {
        //Everything will be automatically logged
//        setLogLevel(Level.FINEST);

        createGroupIfAbsent(COMMUNITY, aDesignTeam);
        requestRole(COMMUNITY, aDesignTeam, manager);
        bufferAgents.addAll(launchAgentsIntoLive(EvaluatedBuffer.class.getName(),1,true));
    }
        
    @Override
    protected void live() {
        ArchitectureEvaluator AE = ArchitectureEvaluator.getInstance();
        AE.init(1);
        
        //initiate population and send to unevaluated buffer
        Architecture testArch = ArchitectureGenerator.getInstance().getRandomArch();
        Result res = AE.evaluateArchitecture(testArch,"Slow");
        AgentAddress evalBufferAddress = findAgent(COMMUNITY, aDesignTeam, evaluatedBuffer);
        ObjectMessage message = new ObjectMessage(res);
        sendMessageWithRole(evalBufferAddress,message,manager);

        // launch other design agents
        try {
            searchAgents.addAll(launchAgentsIntoLive(ModifyAgent.class,1,ModifyAgent.ModifyMode.ASKUSER,ManagerMode.ATEAM));
        } catch (Exception ex) {
            Logger.getLogger(ATeamsManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        logger.info("All agents initiated. Starting search...");

        while(!isDone(AgentEvaluationCounter.getTotalEvals())){
            pause(10);
        }

        for(AbstractAgent agent:searchAgents){
            AbstractAgent.ReturnCode returnCode = AbstractAgent.ReturnCode.TIMEOUT;
            while(returnCode!=AbstractAgent.ReturnCode.ALREADY_KILLED && returnCode!=AbstractAgent.ReturnCode.SUCCESS){
                returnCode = killAgent(agent,100);
            }
        }
        purgeMailbox();
//            killAgent(bufferAgents.iterator().next(),1000);
        killAgentsInList(bufferAgents);

        System.out.println("Done");
        System.out.println(AgentEvaluationCounter.getHashMap());

        AE.clear();
        
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
    
    private Collection<AbstractAgent> launchAgentsIntoLive(Class agentClass,int n,ModifyAgent.ModifyMode modMode,ManagerMode manMode) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        Collection<AbstractAgent> agentList = new ArrayList();
        for(int i=1;i<=n;i++){
            Constructor cotr = agentClass.getConstructor(new Class[]{ModifyAgent.ModifyMode.class,ManagerMode.class});
            AbstractAgent agent = (AbstractAgent)cotr.newInstance(modMode,manMode);
            launchAgent(agent,true);
            agentList.add(agent);
        }
        logger.info("Launching "+n+ " agents of class: "+agentClass);
        return agentList;
    }
     
    private void killAgentsInList(Collection<AbstractAgent> agents){
        Iterator i = agents.iterator();
        while(i.hasNext()){
            DesignAgent agentToKill = (DesignAgent)i.next();
            agentToKill.suicide();
        }
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
    
    private boolean isDone(int n){
        return n>=maxEval;
    }
    
}

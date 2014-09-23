/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
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
public class ModifyAgent extends DesignAgent{
    ModifyMode modMode;
    ManagerMode manMode;
    
    public ModifyAgent(ModifyMode modMode, ManagerMode manMode){
        this.modMode = modMode;
        this.manMode = manMode;
        resetNumExec();
    }
    
    public enum ModifyMode{
        RANDOMSEARCH,MUTATION,CROSSOVER,ADDSYNERGY,REMOVEINTERFERENCE,
        IMPROVEORBIT,REMOVESUPERFLUOUS,ADDTOSMALLSAT,REMOVEFROMBIGSAT,
        BESTNEIGHBOR,ASKUSER
    }

    
    @Override
    protected void activate() {
//        setLogLevel(Level.FINEST);
        createGroupIfAbsent(COMMUNITY, aDesignTeam);
        requestRole(COMMUNITY, aDesignTeam, modifier);
        logger.info(modMode + " Modifier " + this.getNetworkID() + " activating");
    }
    
    @Override
    protected void live(){
        
        logger.info(modMode + " Modifier " + this.getNetworkID() + " running in live");
        while(isAlive() && !endLive){
            //find finished architecture pool to retrieve finished design
            AgentAddress bufferAddress = findAgent(COMMUNITY, aDesignTeam, evaluatedBuffer);

            Collection<Architecture> modifiedArch;
            Architecture unmodifiedArch = getRandArchFromBufferWithRole(bufferAddress);
            Architecture unmodifiedArch2 = null;
            switch(modMode){
                case RANDOMSEARCH:  modifiedArch = randSearch(unmodifiedArch);
                    break;
                case MUTATION: modifiedArch = mutate(unmodifiedArch);
                    break;
                case CROSSOVER: unmodifiedArch2 = getRandArchFromBufferWithRole(bufferAddress);
                    modifiedArch = crossover(unmodifiedArch,unmodifiedArch2);
                    break;
                case ADDSYNERGY: modifiedArch = addSynergy(unmodifiedArch);
                    break;
                case REMOVEINTERFERENCE: modifiedArch = removeInter(unmodifiedArch);
                    break;
                case IMPROVEORBIT: modifiedArch = improveOrbit(unmodifiedArch);
                    break;
                case REMOVESUPERFLUOUS: modifiedArch = removeSuperfluous(unmodifiedArch);
                    break;
                case ADDTOSMALLSAT: modifiedArch = addRand2SmallSat(unmodifiedArch);
                    break;
                case REMOVEFROMBIGSAT: modifiedArch = removeRandLargeSat(unmodifiedArch);
                    break;
                case BESTNEIGHBOR: modifiedArch = bestNeighbor(unmodifiedArch);
                    break;
                case ASKUSER: modifiedArch = askUser(unmodifiedArch);
                    break;
                default: throw new IllegalArgumentException("No such modifier "+this.modMode);
            }
            
            Architecture ref = null;
            //k-arm bandit mode allows only one eval
            if(manMode==ManagerMode.DMABBANDIT){
                suicide(); //flag to terminate after this iteration
                ref = unmodifiedArch;
                //used for assigning credits to operators
                if(unmodifiedArch2!=null){
                    int test = unmodifiedArch.getResult().dominates(unmodifiedArch2.getResult());
                    switch(test){
                        case -1:
                            break;
                        case 0: Random rand = new Random(); //if neither dominates, choose a random parent as ref
                        if(rand.nextBoolean())
                            ref = unmodifiedArch2;
                        break;
                        case 1: ref = unmodifiedArch2;
                        break;
                        default: System.out.println("Something is wrong. Results.dominate didn't return a -1,0, or 1");
                        break;
                    }
                }
            }
            
            //evaluate all architectures that have been created and send to evaluatedBuffer.
            //Also send a message to manager to keep count of evaluations
            Iterator<Architecture> iter = modifiedArch.iterator();
            while(iter.hasNext()){
                Result res =  evaluate(iter.next());
                sendResultToAgentWithRole(COMMUNITY,aDesignTeam,evaluatedBuffer,res,modifier);
                sendMessage(findAgent(COMMUNITY,aDesignTeam,manager),new ObjectMessage(modMode.toString()));                
                if(manMode==ManagerMode.DMABBANDIT){
                    AgentArmCredit data = new AgentArmCredit(ref.getResult(),res);
                    sendMessage(findAgent(COMMUNITY,aDesignTeam,manager),new ObjectMessage(data));
                }
            }
            
            
        }
    }
    
    @Override
    protected void end(){
        System.out.println("Modifier dying");
        if(!endLive)
            throw new IllegalStateException(this.modMode+" agent died but wasn't supposed to");
    }
    
    private Result evaluate(Architecture arch){
        ArchitectureEvaluator AE = ArchitectureEvaluator.getInstance();
        return AE.evaluateArchitecture(arch,"Slow");
    }
    
    /**
     * This method is a one-bit mutation. Finds a random decision to mutate and flips the bit/boolean.
     * @param bufferAddress
     * @return 
     */
    private Collection<Architecture> mutate(Architecture orig){
        Architecture modifiedArch = orig.mutate1bit();
        Collection<Architecture> out = new ArrayList<>();
        out.add(modifiedArch);
        return out;
    }    
    private Collection<Architecture> addRand2SmallSat(Architecture orig){
        Architecture modifiedArch = orig.addRandomToSmallSat();
        Collection<Architecture> out = new ArrayList<>();
        out.add(modifiedArch);
        return out;
    }
    private Collection<Architecture> addSynergy(Architecture orig){
        Architecture modifiedArch = orig.addSynergy();
        Collection<Architecture> out = new ArrayList<>();
        out.add(modifiedArch);
        return out;
    }
    private Collection<Architecture> removeInter(Architecture orig){
        Architecture modifiedArch = orig.removeInterference();
        Collection<Architecture> out = new ArrayList<>();
        out.add(modifiedArch);
        return out;
    }
    private Collection<Architecture> removeRandLargeSat(Architecture orig){
        Architecture modifiedArch = orig.removeRandomFromLoadedSat();
        Collection<Architecture> out = new ArrayList<>();
        out.add(modifiedArch);
        return out;
    }
    private Collection<Architecture> bestNeighbor(Architecture orig){
        Architecture modifiedArch = orig.bestNeighbor();
        Collection<Architecture> out = new ArrayList<>();
        out.add(modifiedArch);
        return out;
    }
    private Collection<Architecture> removeSuperfluous(Architecture orig){
        Architecture modifiedArch = orig.removeSuperfluous();
        Collection<Architecture> out = new ArrayList<>();
        out.add(modifiedArch);
        return out;
    }
    private Collection<Architecture> randSearch(Architecture orig){
        Architecture modifiedArch = orig.randomSearch();
        Collection<Architecture> out = new ArrayList<>();
        out.add(modifiedArch);
        return out;
    }
    private Collection<Architecture> askUser(Architecture orig){
        Architecture modifiedArch = orig.askUserToImprove();
        Collection<Architecture> out = new ArrayList<>();
        out.add(modifiedArch);
        return out;
    }
    private Collection<Architecture> improveOrbit(Architecture orig){
        Architecture modifiedArch = orig.improveOrbit();
        Collection<Architecture> out = new ArrayList<>();
        out.add(modifiedArch);
        return out;
    }
    private Collection<Architecture> crossover(Architecture mother,Architecture father){
        Architecture modifiedArch = mother.crossover1point(father);
        Collection<Architecture> out = new ArrayList<>();
        out.add(modifiedArch);
        return out;
    }
    
    private int archDominates(Architecture a1,Architecture a2) {
        double x1 = a1.getResult().getScience()- a2.getResult().getScience();
        double x2 = a1.getResult().getCost() - a2.getResult().getCost();
        if((x1>=0 && x2<=0) && !(x1==0 && x2==0)) 
            return 1; //a1 dominates a2
        else if((x1<=0 && x2>=0) && !(x1==0 && x2==0))
            return -1; //a2 dominates a1
        else return 0; //neither architecture dominates the other
    }
}

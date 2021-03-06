/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import rbsa.eoss.local.Params;



/**
 *
 * @author nozomihitomi
 */
public class ModifyAgent extends DesignAgent{
    private final ModifyMode modMode;
    private final ManagerMode manMode;
    
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

            ArrayList<Architecture> modifiedArch;
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
            }
            
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
            Collections.shuffle(modifiedArch);
            if(modMode==ModifyMode.BESTNEIGHBOR){ 
                Architecture best_arch;
                Result best_result = new Result(null, -1,-1,-1);
                for(Architecture neighbor:modifiedArch){
                    Result res = evaluate(neighbor);
                    AgentEvaluationCounter.addStat(modMode,ref.getResult(),res);
                    if(res.getScience() > best_result.getScience()) {
                        best_result = res;
                    }
                }
                best_arch = best_result.getArch();
                if(best_arch!=null){
                    sendResultToAgentWithRole(COMMUNITY,aDesignTeam,evaluatedBuffer,best_arch.getResult(),modifier);
                    if(manMode==ManagerMode.DMABBANDIT){
                        AgentArmCredit data = new AgentArmCredit(ref.getResult(),best_arch.getResult());
                        MultiAgentArms.updateArm(modMode, data);
                    }
                }
                else{
                    logger.info("No good neighbors");
                    if(manMode==ManagerMode.DMABBANDIT){
                        AgentArmCredit data = new AgentArmCredit(ref.getResult(),ref.getResult());
                        MultiAgentArms.updateArm(modMode, data);
                    }
                }
                    
            }else{
            
                //evaluate all architectures that have been created and send to evaluatedBuffer.
                //Also send a message to manager to keep count of evaluations
                Iterator<Architecture> iter = modifiedArch.iterator();
                while(iter.hasNext()){
                    Result res =  evaluate(iter.next());
                    sendResultToAgentWithRole(COMMUNITY,aDesignTeam,evaluatedBuffer,res,modifier);
                    AgentEvaluationCounter.addStat(modMode,ref.getResult(),res);        
                    AgentArmCredit data = new AgentArmCredit(ref.getResult(),res);
                    if(manMode==ManagerMode.DMABBANDIT){
                        MultiAgentArms.updateArm(modMode, data);
                    }
                    if(modMode == ModifyMode.ASKUSER){
                        logger.info("\n\nUser asked to improve arch:");
                        logger.info(ref.toString());
                        logger.info("Before improvement had science: " + ref.getResult().getScience() + " and cost: " + ref.getResult().getCost());
                        logger.info("User modified arch:");
                        logger.info(res.getArch().toString());
                        logger.info("After improving, science: " + res.getScience() + " and cost: " + res.getCost());
                        int dom = res.dominates(ref.getResult());
                        if(dom==-1)
                            logger.info("WORSE: Improved architecture is dominated by original\n\n");
                        else if(dom==1)
                            logger.info("BETTER: Improved architecture dominates original\n\n");
                        else
                            logger.info("SAME: Improved architecture neither dominates or is dominated by original\n\n");
                    }
                }
            }
        }
    }
    
    @Override
    protected void end(){
        System.out.println("Modifier dying");
        if(manMode == ManagerMode.DMABBANDIT)
            MultiAgentArms.setReady(true);
//        if(!endLive){
//            System.out.println(this.modMode+" agent died but wasn't supposed to");
//            System.exit(1);
//        }
    }
    
    private Result evaluate(Architecture arch){
        return ArchitectureEvaluator.getInstance().evaluateArchitecture(arch, "Slow");
    }
    
    /**
     * This method is a one-bit mutation. Finds a random decision to mutate and flips the bit/boolean.
     * @param bufferAddress
     * @return 
     */
    private ArrayList<Architecture> mutate(Architecture orig){
        Architecture modifiedArch = orig.mutate1bit();
        ArrayList<Architecture> out = new ArrayList();
        out.add(modifiedArch);
        return out;
    }    
    private ArrayList<Architecture> addRand2SmallSat(Architecture orig){
        Architecture modifiedArch = orig.addRandomToSmallSat();
        ArrayList<Architecture> out = new ArrayList();
        out.add(modifiedArch);
        return out;
    }
    private ArrayList<Architecture> addSynergy(Architecture orig){
        Architecture modifiedArch = orig.addSynergy();
        ArrayList<Architecture> out = new ArrayList();
        out.add(modifiedArch);
        return out;
    }
    private ArrayList<Architecture> removeInter(Architecture orig){
        Architecture modifiedArch = orig.removeInterference();
        ArrayList<Architecture> out = new ArrayList();
        out.add(modifiedArch);
        return out;
    }
    private ArrayList<Architecture> removeRandLargeSat(Architecture orig){
        Architecture modifiedArch = orig.removeRandomFromLoadedSat();
        ArrayList<Architecture> out = new ArrayList();
        out.add(modifiedArch);
        return out;
    }
    private ArrayList<Architecture> bestNeighbor(Architecture orig){
        return orig.bestNeighbor();
    }
    private ArrayList<Architecture> removeSuperfluous(Architecture orig){
        Architecture modifiedArch = orig.removeSuperfluous();
        ArrayList<Architecture> out = new ArrayList();
        out.add(modifiedArch);
        return out;
    }
    private ArrayList<Architecture> randSearch(Architecture orig){
        Architecture modifiedArch = orig.randomSearch();
        ArrayList<Architecture> out = new ArrayList();
        out.add(modifiedArch);
        return out;
    }
    private ArrayList<Architecture> askUser(Architecture orig){
        Toolkit.getDefaultToolkit().beep();
        Architecture modifiedArch = askUserToImprove(orig);
        ArrayList<Architecture> out = new ArrayList();
        out.add(modifiedArch);
        return out;
    }
    private ArrayList<Architecture> improveOrbit(Architecture orig){
        Architecture modifiedArch = orig.improveOrbit();
        ArrayList<Architecture> out = new ArrayList();
        out.add(modifiedArch);
        return out;
    }
    private ArrayList<Architecture> crossover(Architecture mother,Architecture father){
        Architecture modifiedArch = mother.crossover1point(father);
        ArrayList<Architecture> out = new ArrayList();
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
    
    private Architecture askUserToImprove(Architecture orig) {
        //System.out.println("askUserToImprove");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        logger.info("Current arch is " + orig.toString() + ".\n"
                + "Has science score: " + orig.getResult().getScience() +
                "and has cost: " + orig.getResult().getCost() + ".\n How can I improve this architecture?");
        HashMap<String,String[]> mapping= new HashMap<String,String[]>();
        int numInsAdded = 0;
        for (String orb:Params.orbit_list) {    
            try {
                boolean valid = false;
                String input = "";
                while(!valid) {
                    logger.info("Added "+numInsAdded+" instruments");
                    logger.info("New payload in " + orb + "? ");
                    input = bufferedReader.readLine();
                    String[] instruments = input.split(" ");
                    ArrayList<String> validInstruments = new ArrayList<String>();
                    validInstruments.addAll(Arrays.asList(Params.instrument_list));
                    valid = true;
                    for (int i = 0;i<instruments.length;i++) {
                        String instr= instruments[i];
                        if(instr.equalsIgnoreCase("")) {
                            valid = true;
                            break;
                        }
                        if(!validInstruments.contains(instr)) {
                            valid = false;
                            break;
                        }
                    }
                    logger.info(input);
                }    
                if(!input.equalsIgnoreCase(""))
                    numInsAdded+=input.split(" ").length;
                mapping.put(orb,input.split(" "));
            } catch (Exception e) {
                System.out.println("EXC in askUserToImprove" + e.getMessage() + " " + e.getClass());
            }           
        }
        return new Architecture(mapping,orig.getNsats());
    }
}

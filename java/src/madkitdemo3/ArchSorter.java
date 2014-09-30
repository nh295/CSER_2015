/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import madkit.kernel.AgentAddress;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;
import static madkitdemo3.Society.*;
import rbsa.eoss.Architecture;
import rbsa.eoss.Result;
import rbsa.eoss.ResultCollection;
import rbsa.eoss.ResultManager;
import rbsa.eoss.SearchPerformance;
import rbsa.eoss.SearchPerformanceComparator;
import rbsa.eoss.SearchPerformanceManager;
import rbsa.eoss.local.Params;

/**
 *
 * @author nozomihitomi
 */
public class ArchSorter extends DesignAgent{
    // This agent sorts the evaluated architectures into high scoreing 
    // architectures (score is defined by utility function) and top ranking
    // architectures in the fuzzy parteo front. These sorted architectures get 
    // sent to their respective buffers
    
    private ArchPopulation currentPopulation;
    private final int fuzzyParetoArchsWanted = 200;
    private static SearchPerformance sp;
    private SearchPerformanceManager spm;
    private int iteration = 0;
    private ArrayList<SearchPerformance> perfs;
    private ResultManager RM;
    private Stack<Result> results2Save;
    
    @Override
    protected void activate(){
//        setLogLevel(Level.FINEST);
        createGroupIfAbsent(COMMUNITY, aDesignTeam);
        requestRole(COMMUNITY, aDesignTeam, archSorter);
        currentPopulation = new ArchPopulation();
        spm = SearchPerformanceManager.getInstance();
        perfs = new ArrayList();
        RM =  ResultManager.getInstance();
        results2Save = new Stack();
    }
    
    @Override
    protected void live(){
        ArchPopulation fuzzyParetoArchs;
//        AgentAddress tradespaceAddress = findAgent(COMMUNITY, aDesignTeam, tradespace);
        
        while(isAlive() && !endLive){
            Message mail = waitNextMessage(100);
            if(mail!=null){
                if(mail.getSender().getRole().equalsIgnoreCase(evaluatedBuffer)){
                    sp = new SearchPerformance();
                    
                    currentPopulation.clearPopulation();
                    currentPopulation = (ArchPopulation)((ObjectMessage)mail).getContent();
                    
                    // Sort out top pareto ranked architecture
                    fuzzyParetoArchs = selection_NSGA2();
                    
                    //send pareto front to tradespace agent to plot paretoFront
                    ObjectMessage fuzzyParetoArchMessage = new ObjectMessage(fuzzyParetoArchs.copyPopulation());
                    
                    sendReply(mail,fuzzyParetoArchMessage);
//                    sendMessage(tradespaceAddress,fuzzyParetoArchMessage); //get tradespace to plot after every sort
                    
                    RM.saveResultCollection(new ResultCollection(results2Save));
                    
                    iteration++;
                    sp.updateSearchPerformance(results2Save, iteration);
                    SearchPerformance spTemp = new SearchPerformance(sp);
                    spm.saveSearchPerformance(spTemp);
                    perfs.add(spTemp);
                    
                    AgentEvaluationCounter.incrementSPSave();
                }else
                    logger.warning("unsupported sender: " + mail.getSender().getRole());
            }
        }
    }
    
    @Override
    protected void end(){
        long time = System.currentTimeMillis();
        logger.info(Long.toString(time)+": evals="+AgentEvaluationCounter.getTotalEvals());
        SearchPerformanceComparator spc = new SearchPerformanceComparator(perfs);
        spm.saveSearchPerformanceComparator(spc);
        System.out.println("ArchSorter saving and dying");
    }
//    /**
//     * 
//     * @param numberToGet
//     * @return the top n highest scoring architectures.
//     */
//    public ArchPopulation getTopScoringArchs (int numberToGet){
//        List<Architecture> archList = currentPopulation.asList();
//        if(archList.size()<numberToGet)
//            throw new ArrayIndexOutOfBoundsException("Desired number of top scoring arhcs is less than population size");
//        Collections.sort(archList, Architecture.ArchUtilityComparator);
//        ArchPopulation highScoringArchs = new ArchPopulation();
//        int counter = 0;
//        while(counter<numberToGet){
//            highScoringArchs.addArch(archList.get(counter));
//            counter++;
//        }
//        return highScoringArchs;
//    }
    
    private ArchPopulation selection_NSGA2() {
        //From Dani Selva        
        ArrayList<Architecture> newPopList = new ArrayList();
        //non-dominated sorting, returns fronts
        HashMap<Integer,ArrayList<Architecture>> fronts = nonDominatedSorting(currentPopulation,true);
        
        //take n first fronts so as to leave some room
        int i = 1;
        while(newPopList.size() + fronts.get(i).size() <= fuzzyParetoArchsWanted && i < fronts.size()) {
            newPopList.addAll(fronts.get(i));
            i++;
        }
        
        //Take remaining archs from sorted next front
        int NA = fuzzyParetoArchsWanted - newPopList.size();
        if (NA>0) {
            ArrayList<Architecture> sorted_last_front = new ArrayList();
            sorted_last_front.addAll(fronts.get(i));
            computeCrowdingDistance(sorted_last_front);
            Collections.sort(sorted_last_front,Architecture.ArchCrowdDistComparator);
            ArrayList<Architecture> partial_sorted_last_front = new ArrayList(sorted_last_front.subList(0, NA));
            newPopList.addAll(partial_sorted_last_front);
        }
        
        // retrieve results
        results2Save.clear();
        for(Architecture arch:newPopList)
            results2Save.add(arch.getResult());
        
        //Update population
        return new ArchPopulation(newPopList);
    }
    
    public HashMap<Integer,ArrayList<Architecture>> nonDominatedSorting(ArchPopulation currentPopulation,boolean compute_all_fronts) {

        HashMap<Integer,ArrayList<Architecture>> fronts = new HashMap<Integer,ArrayList<Architecture>>();//archs in front i
        HashMap<Architecture,ArrayList<Integer>> dominates = new HashMap<Architecture,ArrayList<Integer>>();//indexes of archs that arch dominates
        int[] dominationCounters = new int[currentPopulation.getSize()];//number of archs that dominate arch i
        for (int i = 0;i<dominationCounters.length;i++)
            dominationCounters[i] = 0;
        
        for (int i = 0;i<currentPopulation.getSize();i++) {
            Architecture a1 = currentPopulation.get(i);
            Result r1 = a1.getResult();
            for (int j = 0;j<currentPopulation.getSize();j++) {
                Architecture a2 = currentPopulation.get(j);
                Result r2 = a2.getResult();
                int r1domr2 = dominates(r1,r2);
                if(r1domr2==1) {//if a1 dominates a2
                    ArrayList<Integer> existing = dominates.get(a1);
                    if(existing == null)
                        existing = new ArrayList<Integer>();
                    existing.add(j);//add j to indexes of archs that arch a1 dominates
                    dominates.put(a1,existing);
                } else if(r1domr2==-1) {
                    dominationCounters[i]++;//increment counter of archs that dominate a1
                }
            }
            if(dominationCounters[i]==0) {//no one dominates arch i
                ArrayList<Architecture> existing = fronts.get(1);
                if(existing == null)
                    existing = new ArrayList<Architecture>();
                existing.add(a1);
                //System.out.println("Arch " + i + " added to Front 1");
                fronts.put(1,existing);//add a1 to first front
            }
        }
        if(!compute_all_fronts)
            return fronts;
        int i = 1;
        ArrayList<Architecture> nextFront = fronts.get(i);
        while(!nextFront.isEmpty()) {
            nextFront = new ArrayList<Architecture>();
            for (int j = 0;j<fronts.get(i).size();j++) {//iterate over archs of front i
                Architecture a1 = fronts.get(i).get(j);//arch j of front i
                ArrayList<Integer> doms = dominates.get(a1);//set of solutions dominated by a1
                if (doms!=null)  {
                    for (int k = 0;k<doms.size();k++) {
                        Architecture a2 = currentPopulation.get(doms.get(k));
                        dominationCounters[doms.get(k)]--;//decrease domination counter of arch a2 since a1 is removed from tradespace
                        if( dominationCounters[doms.get(k)] <= 0) {
                            nextFront.add(a2);
                            //System.out.println("Arch " + doms.get(k) + " added to Front " + (i+1));
                        }    
                    }
                }
            }
            i++;
            if (!nextFront.isEmpty())
                fronts.put(i,nextFront);
        }
        return fronts;
    }
    
    public int dominates(Result r1,Result r2) {
        // Feasibility before fitness
        if (r1.getArch().isFeasibleAssignment() && !r2.getArch().isFeasibleAssignment())
            return 1;
        if (!r1.getArch().isFeasibleAssignment() && r2.getArch().isFeasibleAssignment())
            return -1;
        if (!r1.getArch().isFeasibleAssignment() && !r2.getArch().isFeasibleAssignment())
            if(r1.getArch().getTotalInstruments() < r2.getArch().getTotalInstruments())
                return 1;
            else if(r1.getArch().getTotalInstruments() > r2.getArch().getTotalInstruments()) 
                return -1;
            else //Both are infeasible, and both to teh same degree (i.e., both have the same number of total instruments)
                return 0;
        
        //Both feasible ==> Sorting by fitness
        double x1 = r1.getScience() - r2.getScience();
        double x2 = r1.getCost() - r2.getCost();
        if((x1>=0 && x2<=0) && !(x1==0 && x2==0)) 
            return 1;
        else if((x1<=0 && x2>=0) && !(x1==0 && x2==0))
            return -1;
        else return 0;
    } 
    
    public void computeCrowdingDistance(ArrayList<Architecture> front) {
        
        int nsol = front.size();

        //Science
        Collections.sort(front,Architecture.ArchScienceComparator);
        front.get(0).getResult().setCrowdingDistance(1000);
        front.get(front.size()-1).getResult().setCrowdingDistance(1000);
        for (int i = 1;i<nsol-1;i++) 
            front.get(i).getResult().setCrowdingDistance(
                    front.get(i).getResult().getCrowdingDistance() + Math.abs(
                    (front.get(i+1).getResult().getScience() - front.get(i-1).getResult().getScience())/(Params.max_science-Params.min_science))) ;
        
        //Cost
        Collections.sort(front,Architecture.ArchCostComparator);
        front.get(0).getResult().setCrowdingDistance(1000);
        front.get(front.size()-1).getResult().setCrowdingDistance(1000);
        for (int i = 1;i<nsol-1;i++) 
            front.get(i).getResult().setCrowdingDistance(
                    front.get(i).getResult().getCrowdingDistance() + Math.abs(
                    (front.get(i+1).getResult().getCost() - front.get(i-1).getResult().getCost())/(Params.max_cost-Params.min_cost))) ;
    }
    
    /**
     * Finds union set between high scoring architectures and fuzzy pareto front
     * architectures. 
     * @param highScorePop population of high scoring architectures
     * @param fuzzyParetoPop population of architectures on the fuzzy pareto front
     * @return All architectures in the set returned will be unique
     */
    private ArchPopulation findBestArchs(ArchPopulation highScorePop,ArchPopulation fuzzyParetoPop){
        ArchPopulation bestPopulation = new ArchPopulation();
        
        bestPopulation.mergePopulation(highScorePop);
        bestPopulation.mergePopulation(fuzzyParetoPop);
        return bestPopulation;
    }
    
    public static SearchPerformance getSp(){
        return sp;
    }
}

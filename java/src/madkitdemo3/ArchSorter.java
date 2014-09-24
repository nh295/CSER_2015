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
    private final int fuzzyParetoArchsWanted = 400;
    private static SearchPerformance sp;
    private SearchPerformanceManager spm;
    private int iteration = 0;
    private ArrayList<SearchPerformance> perfs;
    private ResultManager RM;
    private SearchPerformance bestPerf;
    
    @Override
    protected void activate(){
//        setLogLevel(Level.FINEST);
        createGroupIfAbsent(COMMUNITY, aDesignTeam);
        requestRole(COMMUNITY, aDesignTeam, archSorter);
        currentPopulation = new ArchPopulation();
        sp = new SearchPerformance();
        spm = SearchPerformanceManager.getInstance();
        perfs = new ArrayList();
        RM =  ResultManager.getInstance();
        bestPerf = new SearchPerformance();
    }
    
    @Override
    protected void live(){
        ArchPopulation fuzzyParetoArchs;
        ArchPopulation newParetoArchs;
        
        AgentAddress paretoOptimalAddress = findAgent(COMMUNITY, aDesignTeam, fuzzyParetoBuffer);
        AgentAddress tradespaceAddress = findAgent(COMMUNITY, aDesignTeam, tradespace);
        
        while(isAlive() && !endLive){
            Message mail = waitNextMessage();
            switch(mail.getSender().getRole()){
                case evaluatedBuffer:
                    currentPopulation.clearPopulation();
                    currentPopulation = (ArchPopulation)((ObjectMessage)mail).getContent();
                    
                    // Sort out top pareto ranked architecture
                    fuzzyParetoArchs = selection_NSGA2();
                    //send pareto front to tradespace agent to plot paretoFront
                    ObjectMessage fuzzyParetoArchMessage = new ObjectMessage(fuzzyParetoArchs.copyPopulation());
                    
                    sendMessage(paretoOptimalAddress,fuzzyParetoArchMessage);
                    sendMessage(tradespaceAddress,new Message()); //get tradespace to plot after every sort
                    
                    break;
                case bestArchBuffer:
                    currentPopulation.clearPopulation();
                    List<ArchPopulation> popToSort= (List<ArchPopulation>)((ObjectMessage)mail).getContent();

                    fuzzyParetoArchs = popToSort.get(0);
                    
                    //merge the current best and the previous best
                    currentPopulation.mergePopulation(fuzzyParetoArchs);
                    currentPopulation.mergePopulation(popToSort.get(1));
                    
                    newParetoArchs = selection_NSGA2();
                    
                    ObjectMessage bestArchMessage = new ObjectMessage(newParetoArchs.copyPopulation());
                    sendReply(mail,bestArchMessage);
                    
                    break;
                default: logger.warning("unsupported sender: " + mail.getSender().getRole());
            }
        }
    }
    
    @Override
    protected void end(){
        
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
        ArrayList<Architecture> newPopList = new ArrayList<>();
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
            ArrayList<Architecture> sorted_last_front = new ArrayList<>();
            sorted_last_front.addAll(fronts.get(i));
            computeCrowdingDistance(sorted_last_front);
            Collections.sort(sorted_last_front,Architecture.ArchCrowdDistComparator);
            ArrayList<Architecture> partial_sorted_last_front = new ArrayList<> (sorted_last_front.subList(0, NA));
            newPopList.addAll(partial_sorted_last_front);
        }
        
        // retrieve results
        Stack<Result> results = new Stack<>();
        for(Architecture arch:newPopList)
            results.add(arch.getResult());
        RM.saveResultCollection(new ResultCollection(results));
        
        iteration++;
        sp.updateSearchPerformance(results, iteration);
        SearchPerformance spTemp = new SearchPerformance(sp);
        if(spTemp.getCheapest_max_benefit_arch()==null)
            pause(10);
        spm.saveSearchPerformance(spTemp);
        perfs.add(spTemp);
        
        int best = spTemp.compareTo(bestPerf);
        if (best == 1) {
            bestPerf = new SearchPerformance(spTemp);
        }
        
        SearchPerformanceComparator spc = new SearchPerformanceComparator(Long.toString(System.currentTimeMillis()),perfs);
        spm.saveSearchPerformanceComparator(spc);
        
        //Update population
        return new ArchPopulation(newPopList);
    }
    
    private HashMap<Integer,ArrayList<Architecture>> nonDominatedSorting(ArchPopulation pop, boolean compute_all_fronts) {
        List<Architecture>archList =  pop.asList();
        HashMap<Integer,ArrayList<Architecture>> fronts = new HashMap<>();//archs in front i
        HashMap<Architecture,ArrayList<Integer>> dominates = new HashMap<>();//indexes of archs that arch dominates
        int[] dominationCounters = new int[archList.size()];//number of archs that dominate arch i
        for (int i = 0;i<dominationCounters.length;i++)
            dominationCounters[i] = 0;
        
        for (int i = 0;i<archList.size();i++) {
            Architecture a1 = archList.get(i);
            for (int j = 0;j<archList.size();j++) {
                Architecture a2 = archList.get(j);
                int r1domr2 = archDominates(a1,a2);
                if(r1domr2==1) {//if a1 dominates a2
                    ArrayList<Integer> existing = dominates.get(a1);
                    if(existing == null)
                        existing = new ArrayList<>();
                    existing.add(j);//add j to indexes of archs that arch a1 dominates
                    dominates.put(a1,existing);
                } else if(r1domr2==-1) {//if a2 dominates a1
                    dominationCounters[i]++;//increment counter of archs that dominate a1
                }
            }
            if(dominationCounters[i]==0) {//no one dominates arch i
                ArrayList<Architecture> existing = fronts.get(1);
                if(existing == null)
                    existing = new ArrayList<>();
                existing.add(a1);
                //System.out.println("Arch " + i + " added to Front 1");
                fronts.put(1,existing);//add a1 to first front. Index of first front is 1 not 0
            }
        }
       
        if(!compute_all_fronts)
            return fronts;
        int i = 1;
        
        while(!fronts.get(i).isEmpty()) {
            ArrayList<Architecture> nextFront = new ArrayList<>();
            for (int j = 0;j<fronts.get(i).size();j++) {//iterate over archs of front i
                Architecture a1 = fronts.get(i).get(j);//arch j of front i
                ArrayList<Integer> doms = dominates.get(a1);//set of solutions dominated by a1
                if (doms!=null)  {
                    for (int k = 0;k<doms.size();k++) {
                        Architecture a2 = archList.get(doms.get(k));
                        dominationCounters[doms.get(k)]--;//decrease domination counter of arch a2 since a1 is removed from tradespace
                        if( dominationCounters[doms.get(k)] <= 0) {
                            nextFront.add(a2);
                            //System.out.println("Arch " + doms.get(k) + " added to Front " + (i+1));
                        }    
                    }
                }
            }
            i++;
            fronts.put(i,nextFront);
        }
        return fronts;
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
    
    private void computeCrowdingDistance(ArrayList<Architecture> front)
    {
        int nsol = front.size();

        //Science
        Collections.sort(front,Architecture.ArchScienceComparator);
        front.get(0).getResult().setCrowdingDistance(1000.0);
        front.get(front.size()-1).getResult().setCrowdingDistance(1000.0);
        for (int i = 1;i<nsol-1;i++) 
            front.get(i).getResult().setCrowdingDistance(
                    front.get(i).getResult().getCrowdingDistance() + Math.abs(
                    (front.get(i+1).getResult().getScience()- front.get(i-1).getResult().getScience())/(5-0))) ;
        
        //Cost
        Collections.sort(front,Architecture.ArchCostComparator);
        front.get(0).getResult().setCrowdingDistance(1000.0);
        front.get(front.size()-1).getResult().setCrowdingDistance(1000.0);
        for (int i = 1;i<nsol-1;i++) 
            front.get(i).getResult().setCrowdingDistance(
                    front.get(i).getResult().getCrowdingDistance() + Math.abs(
                    (front.get(i+1).getResult().getCost() - front.get(i-1).getResult().getCost())/(23675-0))) ;
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

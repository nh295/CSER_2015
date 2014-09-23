/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import rbsa.eoss.Architecture;

/**
 * The ArchPopulation will maintain its population such that the architectures 
 * in the population are all unique. There are methods to merge architectures, a1 and a2
 * if a1.equals(a2).
 * @author nozomihitomi
 */
public final class ArchPopulation {
    private ArrayList<Architecture> population; //use hashcode from Architeture
    
    public ArchPopulation(){
        population = new ArrayList<>();
    }
    
    public ArchPopulation(List<Architecture> archList){
        this();
        addAllArchs(archList);
    }
    
    public Architecture getRandArch(){
        if(population.isEmpty())
            return null;
        else{
            Random random = new Random();
            Architecture randArch = population.get(random.nextInt(population.size()));
            return randArch;
        }
    }
    
    public Architecture getNextArchAndRemoveIt(){
        Architecture arch = getRandArch();
        if(arch!=null)
            removeArch(arch); //only remove arch if it exists            
        return arch;
    }
    
    public void mergePopulation(ArchPopulation otherPop){
        population.addAll(otherPop.asList());
    }
    
    public List<Architecture> asList(){
        return population;
    }
    
    public void addArch(Architecture arch){
        population.add(arch);
    }
    
    public void addAllArchs(List<Architecture> archList){
        archList.stream().forEach((arch) -> {
            addArch(arch);
        });
    }
    
    public void clearPopulation(){
        population.clear();
    }
    
    public int getSize(){
        return population.size();
    }
    
    private void removeArch(Architecture arch){
        population.remove(arch);
    }
    
    public ArchPopulation copyPopulation(){
        ArchPopulation copyPop = new ArchPopulation();
        Iterator<Architecture> iter = population.iterator();
        while(iter.hasNext()){
            copyPop.addArch(iter.next().copy());
        }
        return copyPop;
    }
}

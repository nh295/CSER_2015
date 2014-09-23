/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package madkitdemo3;

import java.util.List;
import madkit.kernel.Message;
import madkit.message.ObjectMessage;

import static madkitdemo3.Society.*;
import org.jfree.data.xy.XYSeries;
import rbsa.eoss.Architecture;

/**
 *
 * @author nozomihitomi
 */
public class Tradespace extends DesignAgent{
    XYSeries paretoFront1;
    XYSeries paretoFront2;
    XYSeries paretoFront3;
    XYSeries highScoringArchs;
    PlotChart chart;
    
    @Override
    protected void activate() {
        //Everything will be automatically logged
//        setLogLevel(Level.FINEST);
        createGroupIfAbsent(COMMUNITY, aDesignTeam);
        requestRole(COMMUNITY, aDesignTeam, tradespace);
        
        paretoFront1 = new XYSeries("Pareto Optimal 1");
        paretoFront2 = new XYSeries("Pareto Optimal 2");
        paretoFront3 = new XYSeries("Pareto Optimal 3");
        
        chart = new PlotChart("Tradespace",paretoFront1,paretoFront2,paretoFront3);
    }
    
    @Override
    protected void live(){
        while(isAlive()){
            Message mail = waitNextMessage();
            switch(mail.getSender().getRole()){
                case archSorter:
                    plotChart();
//                    logger.info("plotting tradespace");
                    break;
                case fuzzyParetoBuffer:
                    ObjectMessage paretoMessage = (ObjectMessage)mail;
                    updatePareto((ArchPopulation)paretoMessage.getContent());
                    break;
                default: logger.warning("unknown sender");
            }
        }
    }
    
    @Override
    protected void end(){
        System.out.println("Tradespace Agent dying");
    }
    
    private void updatePareto(ArchPopulation paretoArchs){
        paretoFront3.clear();
        for(int i =  0;i<paretoFront2.getItemCount();i++){
            //0~19 because we're grabbing 20 archs to create fuzzy pareto front
            paretoFront3.add(paretoFront2.getX(i), paretoFront2.getY(i));
        }
        paretoFront2.clear();
        for(int i =  0;i<paretoFront1.getItemCount();i++){
            //0~19 because we're grabbing 20 archs to create fuzzy pareto front
            paretoFront2.add(paretoFront1.getX(i), paretoFront1.getY(i));
        }
        
        paretoFront1.clear();
        List<Architecture> paretoArchList= paretoArchs.asList();
        paretoArchList.stream().forEach((arch) -> {
            paretoFront1.add(arch.getResult().getScience(),arch.getResult().getCost());
        });
    }
    
    private void updateHighScoringArch(ArchPopulation highScoreArchs){
        highScoringArchs.clear();
        List<Architecture> highScoreArchList= highScoreArchs.asList();
        highScoreArchList.stream().forEach((arch) -> {
            highScoringArchs.add(arch.getResult().getScience(),arch.getResult().getCost());
        });
    }
    
    private void plotChart(){
        chart.pack();
        chart.setVisible(true);
    }
    
}

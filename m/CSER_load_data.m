import rbsa.eoss.*
import rbsa.eoss.local.*
import madkitdemo3.*
import agentIterfaces.*
import java.io.*;

[filename, pathname, filterindex] = uigetfile('*.rs', 'Pick a results file','MultiSelect','on');

finalDist = 0;
finalLowestCost = 0;
finalLowestCostScience = 0;
spm = SearchPerformanceManager.getInstance;

for i=1:length(filename)
    spc = spm.loadSearchPerformanceComparatorFromFile(strcat(pathname,filename{i}));
    
    distSize = spc.getAvg_pareto_distances.size;
    finalDist = finalDist + spc.getAvg_pareto_distances.get(distSize-1);

    costSize= spc.getLowest_cost_max_science_arch.size;
    finalLowestCost = finalLowestCost + spc.getLowest_cost_max_science_arch.get(costSize-1).getResult.getCost;
    finalLowestCostScience = finalLowestCostScience + spc.getLowest_cost_max_science_arch.get(costSize-1).getResult.getScience;
end

avgFinalDist = finalDist/length(filename);
avgFinalCost = finalLowestCost/length(filename);
avgFinalScience = finalLowestCostScience/length(filename);
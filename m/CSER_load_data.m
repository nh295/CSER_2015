import rbsa.eoss.*
import rbsa.eoss.local.*
import madkitdemo3.*
import agentIterfaces.*
import java.io.*;

[filename, pathname, filterindex] = uigetfile('*.rs', 'Pick a results file','MultiSelect','on');

finalDist = zeros(length(filename),1);
finalLowestCost =  zeros(length(filename),1);
finalLowestCostScience = zeros(length(filename),1);
spm = SearchPerformanceManager.getInstance;

for i=1:length(filename)
    spc = spm.loadSearchPerformanceComparatorFromFile(strcat(pathname,filename{i}));
    
    distSize = spc.getAvg_pareto_distances.size;
    finalDist(i) = spc.getAvg_pareto_distances.get(distSize-1);

    costSize= spc.getLowest_cost_max_science_arch.size;
    finalLowestCost(i) = spc.getLowest_cost_max_science_arch.get(costSize-1).getResult.getCost;
    finalLowestCostScience(i) = spc.getLowest_cost_max_science_arch.get(costSize-1).getResult.getScience;
end

avgFinalDist = mean(finalDist)
stdDevFinalDist = std(finalDist)
avgFinalCost = mean(finalLowestCost)
stdDevFinalCost = std(finalLowestCost)
avgFinalScience = mean(finalLowestCostScience)
stdDevFinalScience = std(finalLowestCostScience)

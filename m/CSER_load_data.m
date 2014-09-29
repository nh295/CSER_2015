import rbsa.eoss.*
import rbsa.eoss.local.*
import madkitdemo3.*
import agentIterfaces.*
import java.io.*;

[filename, pathname, filterindex] = uigetfile('*.rs', 'Pick a results file','MultiSelect','on');
spm = SearchPerformanceManager.getInstance;
spc = spm.loadSearchPerformanceComparatorFromFile(strcat(pathname,filename));

distIter = spc.getAvg_pareto_distances.iterator;
distHist = zeros(spc.getAvg_pareto_distances.size,1);
j = 1;
while(distIter.hasNext)
    distHist(j)=distIter.next;
    j = j + 1;
end

figure(1)
plot(distHist);


lowestCostArchIter = spc.getLowest_cost_max_science_arch.iterator;
lowestCostHist = zeros(spc.getLowest_cost_max_science_arch.size,1);
j = 1;
while(lowestCostArchIter.hasNext)
    lowestCostHist(j)=lowestCostArchIter.next.getResult.getCost;
    j = j + 1;
end

figure(2)
plot(lowestCostHist);
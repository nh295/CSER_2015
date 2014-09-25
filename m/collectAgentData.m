
javaaddpath('.\java\jxl.jar');
javaaddpath('./java/combinatoricslib-2.0.jar');
javaaddpath('./java/commons-lang3-3.1.jar');
javaaddpath('./java/matlabcontrol-4.0.0.jar');
javaaddpath( '.\java\CSER2015.jar' );
import rbsa.eoss.*
import rbsa.eoss.local.*
import madkitdemo3.*
import java.io.*;

AEC = AgentEvaluationCounter.getInstance();
stats = AEC.loadAgentStatFromFile('C:\Users\SEAK1\Nozomi\CSER_2015\results\stat_2014-09-24--21-28-37.rs');

keys = stats.keySet();
iter = keys.iterator();

numAgents = keys.size();
data = cell(numAgents,1);
agent_names = cell(numAgents,1);
i=1;
while(iter.hasNext())
    name = iter.next();
    agent_names{i}=char(name);
    javaArray = stats.get(name);
    arrayIter = javaArray.iterator;
    array = zeros(javaArray.size(),1);
    j=1;
    while(arrayIter.hasNext())
        array(j) = arrayIter.next();
        j=j+1;
    end
    data{i} = array;
    i=i+1;
end

%all on one plot
figure(1);
hold on;
pattern = {'-b','-g','-r','-c','-m','-k',...
           ':b',':g',':r',':c',':m',':k'};
for(i=1:length(data))
    plot(data{i},pattern{i});
end

iter = keys.iterator();
labels = cell(numAgents,1);
i=1;
while(iter.hasNext())
    key = iter.next();
    labels{i}=strcat(char(key),' (',num2str(stats.get(key).size()),')');
    i=i+1;
end
legend(labels);

%plot on subplot
figure(2);
for(i=1:length(data))
    subplot(numAgents,1,i)
    plot(data{i});
    legend(agent_names{i})
    axis([1,length(data{i}),-1,1]);
end

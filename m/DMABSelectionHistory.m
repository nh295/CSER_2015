
javaaddpath('.\java\jxl.jar');
javaaddpath('./java/combinatoricslib-2.0.jar');
javaaddpath('./java/commons-lang3-3.1.jar');
javaaddpath('./java/matlabcontrol-4.0.0.jar');
javaaddpath( '.\java\CSER2015.jar' );
import rbsa.eoss.*
import rbsa.eoss.local.*
import madkitdemo3.*
import java.io.*;


% file = FileInputStream('C:\Users\SEAK1\Nozomi\CSER_2015\results\DMABHistory_2014-09-25--01-59-46.rs' );
% is = ObjectInputStream( file );
% history = is.readObject();
% is.close();
% file.close();
% selectionHistory = history;




hist = madkitdemo3.DMABManager.getInstance.loadAgentStatFromFile('C:\Users\SEAK1\Nozomi\CSER_2015\results\DMABHistory_2014-09-25--12-55-24.rs');

iter = madkitdemo3.ModifyMode.values.iterator;
numAgents = iter.size();
labels = cell(numAgents,1);
i=1;
while(iter.hasNext())
    key = iter.next();
    labels{i}=strcat(char(key),' (',num2str(hist.get(key).size()),')');
    i=i+1;
end

iter = hist.iterator();
agentHistory = cell(hist.size(),numAgents);
i=1;
while(iter.hasNext())
    name = iter.next();
    if(name==ModifyMode)
    end
end



keys = hist.keySet();
iter = keys.iterator();

numAgents = keys.size();
data = cell(numAgents,1);
agent_names = cell(numAgents,1);
i=1;
while(iter.hasNext())
    name = iter.next();
    agent_names{i}=char(name);
    javaArray = hist.get(name);
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
%plot on subplot
figure(2);
for(i=1:length(data))
    subplot(numAgents,1,i)
    plot(data{i});
    legend(agent_names{i})
    axis([1,length(data{i}),-1,1]);
end

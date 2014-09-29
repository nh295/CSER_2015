function CSER_analyze_DMAB_data

path = 'C:\Users\Nozomi\Documents\CSER_2015';
cd(path);

javaaddpath('.\java\jxl.jar');
javaaddpath('./java/combinatoricslib-2.0.jar');
javaaddpath('./java/commons-lang3-3.1.jar');
javaaddpath('./java/matlabcontrol-4.0.0.jar');
javaaddpath( '.\java\CSER2015.jar' );
import rbsa.eoss.*
import rbsa.eoss.local.*
import madkitdemo3.*
import agentIterfaces.*
import java.io.*;

%load agent stat files
disp('Choose agent stat files');
[filename, pathname, filterindex] = uigetfile('*.rs', 'Pick a results file','MultiSelect','on');

ASH = AgentSelectionHistory.getInstance;

if(~iscell(filename))
    agentData = ASH.loadAgentStatFromFile(strcat(pathname,filename));
else
    agentData = cell(length(filename),1);
    for i=1:length(filename)
        agentData{i} = ASH.loadAgentStatFromFile(strcat(pathname,filename{i}));
    end
end

modes = ASH.getModes;
numAgents = modes.length;
avg_history = zeros(agentData{1}.size,numAgents);

for i=1:length(filename)
    history = agentData{i};
    for j = 1:history.size
        for k=1:numAgents
            if history.get(j-1).equals(modes(k))
                avg_history(j,k) = avg_history(j,k)+ 1;
            end
        end
    end
end

avg_history = avg_history/length(filename);

%all on one plot
figure(1);
hold on;
pattern = {'-b','-g','-r','-c','-m','-k',...
           ':b',':g',':r',':c',':m',':k'};
for i=1:numAgents
    plot(avg_history(:,i),pattern{i});
    labels{i}=char(modes(i));
end
legend(labels);

%plot on subplot
figure(2);
for i=1:numAgents
    subplot(numAgents,1,i)
     plot(avg_history(:,i))
    legend(char(modes(i)));
end


end
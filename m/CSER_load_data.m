import rbsa.eoss.*
import rbsa.eoss.local.*
import madkitdemo3.*
import agentIterfaces.*
import java.io.*;

[filename, pathname, filterindex] = uigetfile('*.rs', 'Pick a results file','MultiSelect','on');
spm = SearchPerformanceManager.getInstance;
spc = spm.loadSearchPerformanceComparatorFromFile(strcat(pathname,filename));
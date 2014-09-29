import rbsa.eoss.*
import rbsa.eoss.local.*
import madkitdemo3.*
import agentIterfaces.*
import java.io.*;

[filename, pathname, filterindex] = uigetfile('*.rs', 'Pick a results file','MultiSelect','on');
% file =  FileInputStream( strcat(pathname,filename) );
% is = ObjectInputStream( file );
% is.close();
% file.close();
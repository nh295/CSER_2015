function [j] = global_jess_engine()
%Returns a new jess.Rete object
%   It is reset, can call Matlab, has a beep and a thread functions
%   defined, and a PART module defined.

    persistent engine;
    
    persistent first_call;
    if isempty(first_call)
        first_call = false;
        
        engine = jess.Rete();
        initialize(engine);
    end
    j = engine;
    
    function [] = initialize(~)
        %% Reset the RETE network, and maybe debug

        % jess watch all;
        jess reset;


        %% Define some utility functions

        jess deffunction beep ()...
                ((call java.awt.Toolkit getDefaultToolkit) beep);

        jess deffunction thread (?f)...
                ((new Thread (implement Runnable using ?f))...
                    start);


        %% Teach it how to command Matlab

        jess defglobal...
                ?*matlab* = ((new matlabcontrol.MatlabProxyFactory) getProxy);

        jess deffunction matlab ($?code)...
                ...(?*matlab* eval ?code);
                ...(printout t (str-cat $?code) crlf)...
                ...(printout t (sym-cat $?code) crlf)...
                ...(printout t (length$ ?code) crlf)...
                ...(printout t $?code crlf)...
                (matlabf0 evalin base (str-cat $?code));


        jess deffunction matlabf0 (?f $?argv)...
                (?*matlab* feval ?f ?argv);

        jess deffunction matlabfn (?f ?nout $?argv)...
                ... the return values need conversion!
                (?*matlab* returningFeval ?f ?nout ?argv);

        jess deffunction jess-value (?v)...
                (if (not (java-objectp ?v)) then...
                    ... already a Jess value
                    (return ?v)...
                elif (not ((?v getClass) isArray)) then ...
                    ... normal Java object
                    (return ?v) ...
                else ...
                    ... Java array of doubles
                    (bind ?l (as-list ?v)) ...
                    (if (> (length$ ?l) 1) then ...
                        ... many values
                        (return ?l) ...
                    else ...
                        ... one value
                        (return (nth$ 1 ?l))));

        jess deffunction matlabf1 (?f $?argv) ...
                (jess-value (nth$ 1 (matlabfn ?f 1 $?argv)));

        jess deffunction matlab-nargout (?f) ...
                (bind ?result (matlabf1 nargout ?f)) ...
                (if (>= ?result 0) then ...
                    ?result ...
                else ... last one is varargout
                    (- 0 ?result 1));

        jess deffunction matlabf (?f $?argv)...
                (bind ?nout (matlab-nargout ?f))...
                (if (= ?nout 0) then...
                    (matlabf0 ?f $?argv)...
                elif (= ?nout 1) then...
                    (matlabf1 ?f $?argv)...
                else... doesn't work yet
                    (matlabfn ?f ?nout $?argv));


        %% Jess module for the parts database

        jess defmodule PART;
        jess set-current-module MAIN;
    end
end
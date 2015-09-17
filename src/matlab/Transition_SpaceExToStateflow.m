function Transition_SpaceExToStateflow(sF,chart,printer,ha,opt_display_guard)
%
% ------------------------------------------------------------------------------
% author: Luan Viet Nguyen
% ------------------------------------------------------------------------------

 
    width = 150;
    heigth = 150;
    % parsing transitions from hybidautomaton into SLSF
    trs = ha.transitions;
    modeNamesToIds = printer.getID(ha);
    
    % iterate over transitions    
    for i_trans = 1 : trs.size()
        trans = trs.get(i_trans-1);
        % parsing guard condition, reset from hybidautomaton into SLSF
        guard = printer.getGuardString(trans);
        reset = printer.getAssignmentString(trans);
        transitions(i_trans) = trans;
        % print the guard
        if opt_display_guard
            ['Guard: ', guard(i_trans)];
        end
        transSource =  modeNamesToIds.get(trans.from.name);
        transTarget =  modeNamesToIds.get(trans.to.name);
        % from location
        TransitionPointer = Stateflow.Transition(chart);
        TransitionPointer.Source = sF(transSource);
        % to location
        TransitionPointer.Destination = sF(transTarget);

        % set the position of transition
        if trs.size() == 1
            TransitionPointer.SourceOClock = 0;
            TransitionPointer.DestinationOClock = 3;
            TransitionPointer.MidPoint = [500 125];
            TransitionPointer.LabelPosition = [450 100 0 0]; 
        else
            % add random number to make sure transitions do not overlap
            % each other
            if (transSource == transTarget)
                TransitionPointer.SourceOClock = (i_trans - 1)*(3+rand);
                TransitionPointer.DestinationOClock = (i_trans)*(6+rand);
                TransitionPointer.MidPoint = [550+ i_trans*50 175];
                TransitionPointer.LabelPosition = [500 150-i_trans*50 0 0]; 
            else    
                TransitionPointer.SourceOClock = (i_trans + 1)*6+rand;
                TransitionPointer.DestinationOClock = (i_trans + 1)*6+rand;
                
            end       
        end        
        Guard_Label = '';
        % add the guard into SLSF model
        if ~isempty(char(guard)) && ~isempty(char(reset)) 
            Guard_Label = strcat('[', char(guard),']',10,'{', char(reset) , '}');
        elseif ~isempty(guard) && isempty(char(reset))
            Guard_Label = strcat('[',char(guard),']');
        elseif isempty(guard) && ~isempty(char(reset))
            Guard_Label = strcat('{', char(reset), '}');     
        end
        TransitionPointer.LabelString = Guard_Label;
        pos = TransitionPointer.LabelPosition;
        pos(1) = pos(1)+ 0.5*width;
        pos(2) = pos(2)- heigth/5;
        TransitionPointer.LabelPosition = pos;
    end
end
function [inputVars, outputVars, sF] = nonsemanticTranslation (isContinuous,model,chart,name,ha,config, componentIdx, isAddSignals, inputVars, outputVars)
    % ------------------------------------------------------------------------------
    % author: Luan Viet Nguyen
    % ------------------------------------------------------------------------------

    % number of outputs to scope
    num_port = 0;
    % initial location
    initLoc = config.init.keySet().toString();
    initLoc  = strrep(char(strrep(char(initLoc),']','')),'[','');
    % for network system, only label one initial condition
    %netIntLabel = true;
    modeTotal = 0;
    %
    % Get all constants directly mapped in SpaceEx and constants specified by cfg files  
    template_constants = config.root.getAllConstants();
    oldkeyList = template_constants.keySet().toArray;
    const = java.util.HashMap();
    constIncludingGlobals = java.util.HashMap();
    for j = 1:  oldkeyList.length
        oldkey = oldkeyList(j);
        if strfind(oldkey,name)
            newkey = strrep(oldkey,strcat(name,'.'),'');
            % template_constants.put(newkey, value);
        elseif (isempty(strfind(oldkey, '.')));
            if (componentIdx <= 2)
                % add global constants to first and second component 
                newkey = oldkey;
            else
                newkey = [];
                constIncludingGlobals.put(oldkey, template_constants.get(oldkey));
            end
        else
            newkey = [];
        end
        
        if (~isempty(newkey))
            const.put(newkey, template_constants.get(java.lang.String(oldkey)));
        end
    end
    constIncludingGlobals.putAll(const);
    %
    %position parmameter;
    x = 120;
    y = 300;
    w = 150;
    h = 150;
    % iterate over mode
    position_x = 0;
    position_y = 0;
    y_count = 0;
    % Stateflow printer
    printer = com.verivital.hyst.printers.SimulinkStateflowPrinter;
    printer.setConfig(config);
    printer.ha = ha;
    printer.isSP = false;
    printer.PREFIX_ASSIGNMENT ='';
    printer.PREFIX_VARIABLE ='';
    % variable
    var = ha.variables.toArray();
    numVar = var.length(); 
    %
    % create local, output, and input variables in SLSF model
    % Store guard for plant model, key == input, value == invariant of input
    guard_of_plant = java.util.HashMap();
    guard_of_plant_key = java.util.LinkedList;
    nInputPorts = 0;
    for i = 1: numVar

        % output variable handling
        if (isOutputVar(var(i), ha))
            isOutputVariable = true;
            [num_port] = addOutput(num_port,strcat(var(i),'_out'),chart);
            outputVars.put(var(i), [componentIdx; num_port]);
        else
            isOutputVariable = false;
        end

        % input variable handling (ignored if already output variable)
        if (isInputVar(var(i), ha) && (~ isOutputVariable))
            nInputPorts = nInputPorts + 1;
            isInputVariable = true;
            oldInputComponents = inputVars.get(var(i));
            newEntry = [componentIdx; nInputPorts];
            if (isempty(oldInputComponents)) % Java null check
                % first component where this variable is an input for
                inputVars.put(var(i), newEntry);
                guard_of_plant_key.add(var(i));
            else
                % variable is also input for other components, add component
                oldInputComponents(:, end + 1) = newEntry;
                inputVars.put(var(i), oldInputComponents);
            end
        else
            isInputVariable = false;
        end

        addVariable(var(i), chart, isInputVariable);
    end
    % link the outputs to scope
   % add_line(model.Name,[chart.Name,'/',num2str(i)],['Scope/',num2str(i)],'autorouting','on');     
    [num_port] = addOutput(num_port,[name,'_location'],chart);
    outputVars.put([name,'_location'], [componentIdx; num_port]);
    %
    % initial = printer.getInitialConditionNoSp();
    modeNames = ha.modes.keySet().toArray;
    %basecomponent
    for i_mode = 1 : ha.modes.size()
        modeTotal = modeTotal + 1;
        %clear mode mode binds transitions;
        mode = ha.modes.get(modeNames(i_mode));
        invariant = printer.getInvariantString(mode);
        guard_of_plant = addPlantGuard(modeNames(i_mode),guard_of_plant,invariant,guard_of_plant_key);
        %modes(i_mode) = mode;
        % create corresponding location in SLSF model, 
        % need to be improved
        sF(i_mode) = Stateflow.State(chart);
        if (x+2*(position_x+1)*w < 2000)
            sF(i_mode).position = [x+2*(position_x+1)*w y+2*(position_y)*h w h] ; 
            position_x = position_x + 1;
        else 
            sF(i_mode).position = [x+2*(position_x+1)*w y+2*(position_y)*h  w h];
            position_x = 0;
            position_y = position_y + 1;
            y_count = y_count+ position_y;
        end
        y_count = y_count + 1;

        % create a map from location ids (from SpaceEx) to location objects (for creating transitions
        % print the flow
        flow = printer.getStateDwellLabel(mode);  
        OutputDescription = '';
        for i = 1 : var.length()
            % parse local variables and output
            if (isOutputVar(var(i), ha))
                OutputDescription = [char(OutputDescription),10,strcat(var(i),'_out'),'=',var(i),';'];
            end
        end
        % display each mode in SLSF
        if isContinuous
            ModeDescription = strcat(char(flow),OutputDescription,10,name,'_location =',num2str(i_mode),';');
        else
            % set sampling time
            %set_param(strcat(model.name,'/',name), 'Sampletime', sampletime);
            ModeDescription = strcat(name,num2str(i_mode),10,'entry:',10,OutputDescription,10,name,'_location  =',num2str(i_mode),';');
        end
        % add location ouput

        % add the flow into SLSF model
        sF(i_mode).LabelString = char(ModeDescription);

        % create a default transition
        % set defaut transtion acquired from configuration field
        if (ha.modes.size() >= 1) 
            if strfind(initLoc, modeNames(i_mode))
               xsource = sF(i_mode).Position(1)+sF(i_mode).Position(3)/2;
               ysource = sF(i_mode).Position(2)-30;
               [defaultTransistion] = addInittransistion(chart,sF(i_mode),xsource,ysource);
               % TODO(X) Instead of 'initLoc' one should pass the current
               % mode name, but HyST does not know it in the network setting.
               defaultTransistion.LabelString = filterInitialCondition(...
                  char(printer.getTransitionInit2inLabel(initLoc,1,constIncludingGlobals)), ...
                  inputVars, var, componentIdx);
            end
        end
    end

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
        % FIX: equality checks in SpaceEx (=) look different from Simulink (==)
        guard = strrep(char(guard), ' = ', ' == ');
        reset = printer.getAssignmentString(trans);
        % transistions(i_trans) = trans;
        transSource =  modeNamesToIds.get(trans.from.name);
        transTarget =  modeNamesToIds.get(trans.to.name);
        % from location
        TransitionPointer = Stateflow.Transition(chart);
        TransitionPointer.Source = sF(transSource);
        % to location
        TransitionPointer.Destination = sF(transTarget);

        % set the position of transition
        if trs.size() == 1
            [TransitionPointer] = transistionPos(TransitionPointer,0,3,500,125,450,100);
        else
            % add random number to make sure transitions do not overlap
            % each other
            if (transSource == transTarget)
                [TransitionPointer] = transistionPos(TransitionPointer,(i_trans - 1)*(3+rand),(i_trans)* (6+rand),...
                                                    550+ i_trans*50,175,500,150-i_trans*50);
            else    
                TransitionPointer.SourceOClock = (i_trans + 1)*6+rand;
                TransitionPointer.DestinationOClock = (i_trans + 1)*6+rand;
            end       
        end        
        Guard_Label = '';
        % add the guard into SLSF model
        if (strcmp(char(guard),'true') && isempty(char(reset)))
            guard = guard_of_plant.get(trans.to.name);
            if ~isempty(guard)
                   guard = strrep(char(guard), ' = ', ' == ');
                   Guard_Label = strcat('[',char(guard),']');
            end       
        elseif ~isempty(char(guard)) && ~isempty(char(reset)) 
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
    % Parsing constant from hybidautomaton into SLSF    
    constKey = const.keySet().toArray;
    if (const.containsKey('tmax') || const.containsKey('Tmax'))
        if const.containsKey('Tmax')
            stopTime = [const.get('Tmax').middle()];
        else 
            stopTime = [const.get('tmax').middle()];
        end    
       % set simulation time to tmax
        set_param(model.Name, 'StopTime', num2str(stopTime));
    end
    if ~isempty(const)
        % add constant into SLSF without tmax 
        constValue = const.values.toArray;
        constKey = const.keySet().toArray;
        % add constants
        for i_const = 1: const.size
            sF_cons = Stateflow.Data(chart); 
            sF_cons.Name = constKey(i_const);
            sF_cons.Scope = 'Constant';
            sF_cons.Props.InitialValue = num2str(constValue(i_const).middle());
        end
    end
    if (isAddSignals && (componentIdx == 1)) % only add scope for first component
        addScope(num_port,model,chart);
    end
end    
function [out] = addState(chart,x,y,w,h,name)
    state = Stateflow.State(chart);
    state.position = [x y w h];
    state.Name = name;
    out = state ;
end
function [out] = addInittransistion(chart,state,xsource,ysource)
    defaultTransition = Stateflow.Transition(chart);
    defaultTransition.Destination = state;
    defaultTransition.DestinationOClock = 0;
    defaultTransition.SourceEndPoint = [xsource ysource];
    defaultTransition.MidPoint = [xsource ysource+15];
    defaultTransition.LabelPosition = [xsource-50 ysource-50 0 0];
    out = defaultTransition;
end

function [out] = transistionPos(TransitionPointer,src,dst,midx,midy,labelx,labely)
    TransitionPointer.SourceOClock = src;
    TransitionPointer.DestinationOClock = dst;
    TransitionPointer.MidPoint = [midx midy];
    TransitionPointer.LabelPosition = [labelx labely 0 0]; 
    out = TransitionPointer;
end
function [out] = addOutput(num_port,name,chart)
    sF_output = Stateflow.Data(chart); 
    sF_output.Name = name;
    sF_output.Scope = 'Output';
    sF_output.Update = 'DISCRETE';
    out = num_port + 1;
end

function addVariable(name, chart, isInputVariable)
    sF_var = Stateflow.Data(chart); 
    sF_var.Name = name;
    if (isInputVariable)
        sF_var.Scope = 'Input';
    else
        sF_var.Scope = 'Local';
    end
    sF_var.Update = 'CONTINUOUS';
end

function addScope(num_port,model,chart)
    %add scope to see the outputs
    add_block('built-in/Scope',[model.Name '/Scope'],'NumInputPorts',num2str(num_port),'Position',[350, 25,  400, 100],...
        'SaveToWorkspace', 'on', ...
        'SaveName', 'ScopeData', ...
        'DataFormat', 'Structure', ...
        'LimitDataPoints', 'off');
    for i = 1:num_port
        add_line(model.Name,[chart.Name,'/',num2str(i)],['Scope/',num2str(i)],'autorouting','on');
    end
end

function [ answer ] = isInputVar( var, ha )
%% true iff variable is an input variable for this automaton

    %answer = com.verivital.hyst.util.AutomatonUtil.isInputVariable(ha, var);
    
    % TODO(X) temporary solution until HyST provides this information
    % if not output, it is input
    
    %answer = false;
    answer = ~com.verivital.hyst.util.AutomatonUtil.isOutputVariable(ha, var);
    
%     if ((strcmp(var, 'vc')|| strcmp(var, 'il'))&& strcmp(ha.instanceName, 'controller'))
%         answer = true;
%     elseif (strcmp(var, 'mode_out'))
%         answer = true;
%     end
end

function [ answer ] = isOutputVar( var, ha )
%% true iff variable is an output variable for this automaton

    answer = com.verivital.hyst.util.AutomatonUtil.isOutputVariable(ha, var);
end

function [ labelOut ] = filterInitialCondition( labelIn, inputVars, vars, componentIdx )
%% filters the initial condition for variables which actually occur in the automaton
% TODO(X) This should be handled by the Java printer, but in a network it cannot
% find the mode name as it becomes a concatenation.

    lines = strsplit(labelIn,'\n');
    nLines = 0;
    nLinesAdded = 0;
    labelOut = '{';
    for i = 1 : length(lines)
        line = lines{i};
        if (isempty(line));
            continue;
        end
        nLines = nLines + 1;
        
        % remove parentheses
        if (nLines == 1)
            line = line(2 : end);
        end
        if (i == length(lines))
            line = line(1 : end - 1);
        end
        
        % assume deterministic assignments with equality
        assignment = strsplit(line, ' = ');
        lhs = assignment{1};
        
        % do not assign variables which do not occur in the automaton
        isVar = false;
        for j = 1 : length(vars)
            if (strcmp(char(vars(j)), lhs))
                isVar = true;
                break;
            end
        end
        if (~ isVar)
            continue;
        end
        
        % do not assign input variables
        containingComponents = inputVars.get(lhs);
        if (~ isempty(containingComponents) && ...
                (containingComponents(1, end) == componentIdx))
            continue;
        end
        
        % append line again
        if (nLinesAdded > 0)
            labelOut = sprintf('%s\n', labelOut);
        end
        nLinesAdded = nLinesAdded + 1;
        labelOut = [labelOut, line];
    end
    labelOut = [labelOut, '}'];
end
% function used to add guards for plant model when using labels
function [guard_of_plant] = addPlantGuard(modeName,guard_of_plant,invariant,input)
    inv = strsplit(char(invariant),'&');
    for i = 1: length(inv)
        for j = 1: input.size()
            if ~isempty(strfind(inv(i),char(input.get(j-1))))
                guard_of_plant.put(modeName,inv(i));
            end
        end
    end
end
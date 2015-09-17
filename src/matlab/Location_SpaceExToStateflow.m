function [sF] = Location_SpaceExToStateflow(model,chart,printer,config,ha,name,opt_cfg_reader,opt_display_flow,opt_display_invariant)
%function [sF,out_vars,variables,idToLocation,InitialConstant] = Location_SpaceExToStateflow(model,chart,ha,name,opt_cfg_reader,opt_display_flow,opt_display_invariant)
% ------------------------------------------------------------------------------
% author: Luan Viet Nguyen
% ------------------------------------------------------------------------------

    %position parmameter;
    x = 20;
    y = 120;
    w = 150;
    h = 150;
    % iterate over mode   
    position_x = 0;
    position_y = 0;
    % Stateflow printer
    
    % variable
    var = ha.variables.toArray();
    numVar = var.length();
    % initial location 
    initLoc = config.init.keySet().toString();   
    % initial = printer.getInitialConditionNoSp();
    modeName = ha.modes.keySet().toArray;
    %basecompone
    for i_mode = 1 : ha.modes.size()
        %clear mode mode binds transitions;
        mode = ha.modes.get(modeName(i_mode));
        modes(i_mode) = mode;
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
        end

        % create a map from location ids (from SpaceEx) to location objects (for creating transitions
        % print the flow
        flow = printer.getStateDwellLabel(mode);
        OutputDescription = '';
        for i = 1 : var.length()
            % parse local variables and output
            OutputDescription = [char(OutputDescription),10,strcat(var(i),'_out'),'=',var(i),';'];
        end
        % display each mode in SLSF
        ModeDescription = strcat(char(flow),OutputDescription,10,'Location_out =',num2str(i_mode),';');

        % add the flow into SLSF model
        sF(i_mode).LabelString = char(ModeDescription);

        % create a default transition
        % set defaut transtion acquired from configuration field 
        if opt_cfg_reader
            if strcmp(initLoc, strcat('[',modeName(i_mode),']'))
               defaultTransition = Stateflow.Transition(chart);
               defaultTransition.Destination = sF(i_mode);
               dtA.DestinationOClock = 0;
               xsource = sF(i_mode).Position(1)+sF(i_mode).Position(3)/2;
               ysource = sF(i_mode).Position(2)-30;
               dtA.SourceEndPoint = [xsource ysource];
               dtA.MidPoint = [xsource ysource+15];
               defaultTransition.LabelString = char(printer.getTransitionInit2inLabel(mode,1,ha.constants));
            end
        end
    end

    % Parsing constant from hybidautomaton into SLSF
    const = ha.constants;
    constKey = const.keySet().toArray;
    if (const.containsKey('tmax') || const.containsKey('Tmax'))
        if const.containsKey('Tmax')
            stopTime = [const.get('Tmax').middle()];
        else 
            stopTime = [const.get('tmax').middle()];
        end    
       % set simulation time to tmax
        set_param(model.Name, 'StopTime', num2str(stopTime));
        const.remove('tmax');
        const.remove('Tmax');
    end
    if ~isempty(const)
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
    % add scope to see the outputs
    add_block('built-in/Scope',[name '/Scope'],'NumInputPorts',num2str(numVar+1),'Position',[350, 25, 400, 100],...
            'SaveToWorkspace', 'on', ...
        'SaveName', 'ScopeData', ...
        'DataFormat', 'Structure', ...
        'LimitDataPoints', 'off');
    
    %
    % create local, output, and input variables in SLSF model
    for i = 1: numVar
        sF_local = Stateflow.Data(chart);      
        sF_output = Stateflow.Data(chart); 
        sF_local.Name = var(i);
        sF_local.Scope = 'Local';
        sF_local.Update = 'CONTINUOUS'; % Change update method of local variable to continuous
        sF_output.Name = strcat(var(i),'_out');
        sF_output.Scope = 'Output';
        sF_output.Update = 'DISCRETE';	% Change update method of ouput to discrete
        % link the outputs to scope
        add_line(model.Name,[chart.Name,'/',num2str(i)],['Scope/',num2str(i)],'autorouting','on');     
    end
    % ouput location number
    sF_output = Stateflow.Data(chart); 
    sF_output.Name = 'Location_out';
    sF_output.Scope = 'Output';
    sF_output.Update = 'DISCRETE';
    add_line(model.Name,[chart.Name,'/',num2str(numVar+1)],['Scope/',num2str(numVar+1)],'autorouting','on');
end
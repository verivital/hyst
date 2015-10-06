function [sF] = nonsemanticTranslation(isNetwork,model,chart,config,opt_cfg_reader)
    % ------------------------------------------------------------------------------
    % author: Luan Viet Nguyen
    % ------------------------------------------------------------------------------
    components = config.root.template.children;
    comp_name = components.keySet().toArray();
    comp_size = components.size();
    % number of outputs to scope
    num_port = 0; 
    if comp_size > 1
        isNetwork = true;
    end
    % initial location
    initLoc = config.init.keySet().toString();
    initLoc  = strrep(char(strrep(char(initLoc),']','')),'[','');
    % for network system, only label one initial condition
    %netIntLabel = true;
    modeTotal = 0;
    % using list of variable, constants to eliminate duplicated variables and
    % outputs declaration
    varList = java.util.LinkedList();
    %
    % Get all constants directly mapped in SpaceEx and constants specified by cfg files  
    template_constants = config.root.getAllConstants();
    oldkeyList = template_constants.keySet().toArray;
    for i = 1: comp_size
        template_name = comp_name(i);
        for j = 1:  template_constants.size
            oldkey = oldkeyList(j);
            if strfind(oldkey,template_name)
                newkey = strrep(oldkey,strcat(template_name,'.'),'');
                template_constants.put(newkey, template_constants.get(oldkey));
                template_constants.remove(oldkey);
            end    
        end
    end
    %root_constants = config.root.constants
    const = java.util.HashMap;
    const.putAll(template_constants);
    %const.putAll(root_constants)
    %
    for i_comp = 1: comp_size
        ha = components.get(comp_name(i_comp)).child;
        %position parmameter;
        x = 120;
        y = 300*i_comp;
        w = 150;
        h = 150;
        % iterate over mode   
        position_x = 0;
        position_y = 0;
        y_count = 0;
        % Stateflow printer
        printer = com.verivital.hyst.printers.StateflowSpPrinter;
        printer.setConfig(config);
        printer.ha = ha;
        printer.isSP = false;
        printer.PREFIX_ASSIGNMENT ='';
        printer.PREFIX_VARIABLE ='';
        % variable
        var = ha.variables.toArray();
        numVar = var.length(); 
        % initial = printer.getInitialConditionNoSp();
        modeName = ha.modes.keySet().toArray;
        %basecompone
        for i_mode = 1 : ha.modes.size()
            modeTotal = modeTotal + 1;
            %clear mode mode binds transitions;
            mode = ha.modes.get(modeName(i_mode));
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
                OutputDescription = [char(OutputDescription),10,strcat(var(i),'_out'),'=',var(i),';'];
            end
            % display each mode in SLSF
            ModeDescription = strcat(char(flow),OutputDescription,10,comp_name(i_comp),'_location =',num2str(i_mode),';');
            % add location ouput

            % add the flow into SLSF model
            sF(i_mode).LabelString = char(ModeDescription);

            % create a default transition
            % set defaut transtion acquired from configuration field 
            if opt_cfg_reader 
                if ~isNetwork || (ha.modes.size() > 1 && isNetwork) 
                    if strfind(initLoc, modeName(i_mode))
                       xsource = sF(i_mode).Position(1)+sF(i_mode).Position(3)/2;
                       ysource = sF(i_mode).Position(2)-30;
                       [defaultTransistion] = addInittransistion(chart,sF(i_mode),xsource,ysource);
                       if ~isNetwork
                           defaultTransistion.LabelString = char(printer.getTransitionInit2inLabel(mode,1,ha.constants));
                       end
                    end
                end
            end
        end
        %
        % create local, output, and input variables in SLSF model
        for i = 1: numVar
            if ~varList.contains(var(i))
                varList.add(var(i));
                addVariable(var(i),chart);
                [num_port] = addOutput(num_port,strcat(var(i),'_out'),chart);
            end    
            % link the outputs to scope
           % add_line(model.Name,[chart.Name,'/',num2str(i)],['Scope/',num2str(i)],'autorouting','on');     
        end
        [num_port] = addOutput(num_port,[comp_name(i_comp),'_location'],chart);
        if ha.modes.size() > 1 && isNetwork
            % add state outside each components
            [bagState] = addState(chart,x+3/2*w,y-h/4,w*2*position_x+10,h*(position_y+3/2),comp_name(i_comp));
        end
        if i_comp == comp_size && comp_size > 1
            % add system state
            [system] = addState(chart,x+w,150,2000,2*i_comp*h*(position_y+3/2),model.name);
            set(system,'Decomposition','PARALLEL_AND');
            xsource = system.Position(1)+sF(i_mode).Position(3)/2;
            ysource = system.Position(2)-30;
            [defaultTransistion] = addInittransistion(chart,system,xsource,ysource);
            intmode = ha.createMode(initLoc);
            defaultTransistion.LabelString = char(printer.getTransitionInit2inLabel(intmode,1,const));
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
                    [TransitionPointer] = transistionPos(TransitionPointer,(i_trans - 1)*(3+rand),(i_trans)*(6+rand),...
                                                        550+ i_trans*50,175,500,150-i_trans*50);
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
        const.remove('tmax');
        const.remove('Tmax');
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
    addScope(num_port,model,chart);
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

function addVariable(name,chart)
    sF_var = Stateflow.Data(chart); 
    sF_var.Name = name;
    sF_var.Scope = 'Local';
    sF_var.Update = 'CONTINUOUS';
end

function addScope(num_port,model,chart)
    %add scope to see the outputs
    add_block('built-in/Scope',[model.Name '/Scope'],'NumInputPorts',num2str(num_port),'Position',[350, 25, 400, 100],...
        'SaveToWorkspace', 'on', ...
        'SaveName', 'ScopeData', ...
        'DataFormat', 'Structure', ...
        'LimitDataPoints', 'off');
    for i = 1:num_port
        add_line(model.Name,[chart.Name,'/',num2str(i)],['Scope/',num2str(i)],'autorouting','on');
    end
end
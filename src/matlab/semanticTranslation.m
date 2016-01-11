function semanticTranslation( chart, config, ha, modelName, optEagerViolation )
%SEMANTICTRANSLATION Model translation with semantics preservation.
% Each location instantiates a location pattern for the Stateflow model.
% Similarly, each transition instantiates a transition pattern. First the
% location patterns are created, since (chicken or the egg dilemma) the
% transitions connect the resulting states/junctions.
%
% ------------------------------------------------------------------------------
% author: Christian Schilling
% ------------------------------------------------------------------------------
    % Set this option for extra modification of the chart arrangements (size,
    % labels) aka "pretty printing mode".
    global G_IS_PRETTY_PRINT;
    G_IS_PRETTY_PRINT = true;
    
    % pretty printing threshold for expressions to be split
    if (G_IS_PRETTY_PRINT)
        prettyPrintThreshold = 15;
    else
        prettyPrintThreshold = 0;
    end
    
    % Set this option to add epsilons to bounds to prevent intersections only at
    % a point. This case is not handled correctly by Stateflow, as it is very
    % unlikely to find this point.
    % The size of epsilon is a model parameter and can be controlled by the
    % user.
    IS_ADD_EPS = true;
    
    % position parameters for locations and junctions
    POS = struct('X0', 50, 'Y0', 50, 'W', 200, 'H', 150, 'J_H', 0, 'WS', 200);
    POS.J_H = 0.3 * POS.H;
    
    % variables, locations, transitions
    VARS = ha.variables;
    LOCS = ha.modes;
    TRANS = ha.transitions;
    CONSTS = ha.constants;
    
    % Java printer
    printer = com.verivital.hyst.printers.SimulinkStateflowPrinter(...
        VARS, IS_ADD_EPS, prettyPrintThreshold);
    
    printer.setConfig(config);
     
    printer.isSP = true;
    
    newLineChar = char(printer.lineSeparator);
    
    % maximum number of outgoing transitions for any state (pdated later)
    numTransMax = 0;
    
    % maximum number of random numbers needed
    % We need at least one random number and at most one for each variable in
    % the case that there is a transition where each variable is reset randomly.
    numRandMax = 1;
    
    % mapping from location name to location ID
    % NOTE: The ID is assigned here, so there is no correllation with the
    % SpaceEx ID.
    loc2id = java.util.LinkedHashMap(LOCS.size());
    
    % mapping from location ID to the following data:
    % - location name
    % - entry state
    % - junction for leaving dwell-state
    % - outgoing transitions (possibly empty)
    % - initial condition (possibly empty)
    id2data = struct('state', [], 'junction', [], 'trans', [], 'name', []);
    id2data.state = cell(LOCS.size(), 1);
    id2data.junction = cell(LOCS.size(), 1);
    id2data.trans = cell(LOCS.size(), 1);
    id2data.name = cell(LOCS.size(), 1);
    % NOTE: Mapping to the original location's name is currently not needed for
    % the translation, but can be helpful for testing purposes.
    
    % fill loc2id and names
    locId = 0;
    itLoc = LOCS.values().iterator();
    while (itLoc.hasNext())
        locName = itLoc.next().name;
        locId = locId + 1;
        loc2id.put(locName, locId);
        id2data.name{locId} = locName;
        id2data.trans{locId} = java.util.LinkedList();
    end
    % fill outgoing transitions
    itTrans = TRANS.iterator();
    while (itTrans.hasNext())
        trans = itTrans.next();
        sourceId = loc2id.get(trans.from.name);
        id2data.trans{sourceId}.add(trans);
    end
    
    %ha.modeNamesToIds = loc2id;
    
    % --- add chart components (states, junctions, and transitions) ---
    
    % add initial state
    state_init = addStateInit();
    initIdx = 1;
    
    % iterate over all locations
    locId = 0;
    previousStates = [];
    itLoc = LOCS.values().iterator();
    while (itLoc.hasNext())
        % get location
        loc = itLoc.next();
        locId = locId + 1;
        
        % get outgoing transitions
        numTransOut = id2data.trans{locId}.size();
        numTransMax = max(numTransMax, numTransOut);
        
        % add entry state
        state_in = addStateIn(loc, locId, numTransOut, previousStates);
        
        % put state into mapping
        id2data.state{locId} = state_in;
        
        % add transition 'initial state -> entry state' (if location is initial)
        initIdx = ...
            addTransitionInit2in(loc, state_init, state_in, initIdx, CONSTS);
        
        % add junction for choosing transitions
        junction_in2dwell = addJunctionIn2dwell(state_in.Position);
        
        % add transition 'entry state -> transition choosing junction'
        addTransitionIn2choose(state_in, junction_in2dwell);
        
        % extra handling in case there are outgoing transitions
        if (numTransOut > 0)
            % add dwelling time choosing state
            state_choose = addStateChoose(loc, state_in.Position);
        else
            state_choose = state_in;
        end
        
        % add dwelling state
        state_dwell = addStateDwell(loc, state_choose.Position);
        
        % add junction for leaving dwell state
        junction_dwell2next = addJunctionDwell2next(state_dwell.Position);
        id2data.junction{locId} = junction_dwell2next;
        
        % add transition 'dwell state -> leave junction'
        addTransitionDwell2leave(state_dwell, junction_dwell2next);
        
        % add junction for backtracking
        junction_dwell2choose = addJunctionDwell2choose(...
            state_choose.Position, state_dwell.Position);
        
        % add transition 'dwell state -> backtrack junction'
        addTransitionDwell2backtrack(state_dwell, junction_dwell2choose, ...
            loc, optEagerViolation);
        
        % add transition 'leave junction -> dwell state'
        addTransitionLeave2dwell(junction_dwell2next, state_dwell);
        
        % extra handling in case there are outgoing transitions
        if (numTransOut > 0)
            % add transition 'choosing state -> dwelling state'
            addTransitionChoose2dwell(state_choose, state_dwell);
            
            % add transition 'transition choosing junction -> choosing state'
            addTransitionChooseJ2choose(junction_in2dwell, state_choose);
            
            % add transition 'choosing state -> transition choosing junction'
            addTransitionChoose2chooseJ(state_choose, junction_in2dwell);
            
            % add transition 'time choosing junction -> dwelling state'
            addTransitionChooseTime2dwell(junction_dwell2choose, state_choose);
            
            % store states for next iteration
            previousStates = [state_in, state_choose, state_dwell];
        else
            % store states for next iteration
            previousStates = [state_in, state_dwell];
        end
        
        % add transition 'entry junction -> dwelling state'
        addTransitionInJ2dwell(junction_in2dwell, state_dwell);
        
        % add transition 'backtracking junction -> dwelling state'
        addTransitionBacktrack2dwell(junction_dwell2choose, state_dwell);
    end
    
    % subtract 1, as it was incremented k times for k initial transitions, but
    % started with value '1'
    initIdx = initIdx - 1;
    
    % For adding the original transitions, we need to have all states created.
    % So we start a new loop here to add the regular transitions.
    for locId = 1 : LOCS.size()
        junction_dwell2next = id2data.junction{locId};
        juncX = junction_dwell2next.Position.Center(1);
        juncY = junction_dwell2next.Position.Center(2);
        
        % add a junction for each (original) transition
        iTrans = 0;
        transIt = id2data.trans{locId}.iterator();
        while (transIt.hasNext())
            iTrans = iTrans + 1;
            % add junction for taking (original) transition
            junction_trans = addJunctionTrans(juncX, juncY, iTrans);
            
            % add transition 'leaving junction -> transition junction'
            addTransitionLeave2trans(junction_dwell2next, junction_trans, iTrans);
            
            % add transition 'transition junction -> entry state'
            % = original transition
            trans = transIt.next();
            targetLoc = trans.to;
            targetId = loc2id.get(targetLoc.name);
            state_in = id2data.state{targetId};
            addTransitionTrans2in(junction_trans, state_in, trans, juncX);
        end
    end
    
    % add initial transition
    addTransitionInitial(state_init, numTransMax, initIdx);
    
    % --- add outer components and variables ---
    
    CHART_NAME = chart.Name;
    
    numRandMax = max(numRandMax, printer.getRandomNumber());
    
    % add (translation-related) auxiliary variables
    addAuxiliaryVariables(chart, printer, numTransMax, numRandMax);
    
    % add stop block
    add_block('built-in/Stop', [modelName '/Stop'], ...
        'Position', [450, 20, 500, 70]);
    line = ...
        add_line(modelName, [CHART_NAME, '/1'], 'Stop/1', 'autorouting', 'on');
    set_param(line, 'Name', 'stop');
    
    % add random number block
    % This is used for choosing a random number.
    if (numRandMax > 1)
        % only add ability to choose several random numbers at a time if needed
        minString = '[0';
        minAddString = ', 0';
        maxString = '[1';
        maxAddString = ', 1';
        for i = 2 : numRandMax
            minString = [minString, minAddString];
            maxString = [maxString, maxAddString];
        end
        minString = [minString, ']'];
        maxString = [maxString, ']'];
    else
        % here we only need one random number at a time
        minString = '0';
        maxString = '1';
    end
    addAndLinkRandomBlock(printer, modelName, 'RandNum', [25, 125, 75, 175], ...
        minString, maxString, CHART_NAME, 1);
    
    % add random array block
    % This is used for choosing the next outgoing transition.
    % It is only added in case there are any outgoing transitions in the model.
    if (numTransMax > 0)
        total = num2str(numTransMax + 1);
        minString = '[1';
        minAddString = ', 1';
        maxString = ['[', total];
        maxAddString = [', ', total];
        for i = 2 : numTransMax
            minString = [minString, minAddString];
            maxString = [maxString, maxAddString];
        end
        minString = [minString, ']'];
        maxString = [maxString, ']'];
        
        addAndLinkRandomBlock(printer, modelName, 'RandArr', ...
            [25, 200, 75, 250], minString, maxString, CHART_NAME, 2);
    end
    
    % number of auxiliary variables linked to scope
    SCOPE_OFFSET = 3;
    
    % add scope block
    add_block('built-in/Scope', [modelName '/Scope'], ...
        'NumInputPorts', num2str(VARS.size() + SCOPE_OFFSET), ...
        'Position', [450, 100, 500, 175], ...
        'SaveToWorkspace', 'on', ...
        'SaveName', 'ScopeData', ...
        'DataFormat', 'Structure', ...
        'LimitDataPoints', 'off');
    
    % link time variable and backtracking information to scope
    linkToScope(modelName, CHART_NAME, 2, 1, 'time');
    linkToScope(modelName, CHART_NAME, 3, 2, 'backtrack');
    linkToScope(modelName, CHART_NAME, 4, 3, 'location');
    
    % string constants for variables
    PREFIX_VARIABLE = char(printer.PREFIX_VARIABLE);
    PREFIX_ASSIGNMENT = char(printer.PREFIX_ASSIGNMENT);
    PREFIX_OUTPUT = char(printer.PREFIX_OUTPUT);
    
    % create local and output variables
    i = SCOPE_OFFSET;
    itVar = VARS.iterator();
    while (itVar.hasNext())
        variable = itVar.next();
        i = i + 1;
        
        % original variable
        var = addVariable(chart, [PREFIX_VARIABLE, variable], 'Local', ...
            'CONTINUOUS');
        var.Props.Array.Size = '';
        
        % temporary variable
        addVariable(chart, [PREFIX_ASSIGNMENT, variable], 'Local', 'DISCRETE');
        
        % output variable
        addVariable(chart, [PREFIX_OUTPUT, variable], 'Output', 'DISCRETE');
        
        % link the output to scope
        linkToScope(modelName, CHART_NAME, i + 1, i, variable);
    end
    
    % add constants
    addConstants(chart, CONSTS, PREFIX_VARIABLE);
    
    % set simulation time to infinity
    set_param(modelName, 'StopTime', 'inf');
    
    % assign standard values to model parameters
    assignStandardModelParameters(printer);
    
    
    % ---
    % --- nested functions for adding states, junctions, and transitions ---
    % ---
    
    
    function [ state ] = addStateInit()
        state = addState(chart, 'init', [0, 0, 30, 20], newLineChar);
    end
    
    function [ state ] = addStateIn( loc, locId, numTransOut, lastStates )
    % adds an entry state
    % - stores variables to temporary variables
    % - constructs new transition array
    % - resets transition array index
    
        if (~ isempty(lastStates))
            % find rightmost border of the previous location cluster
            posX = 0;
            for state = lastStates
                posX = max(posX, state.Position(3));
            end
            posX = posX + lastStates(1).Position(1);
        else
            posX = POS.X0;
        end
        label = char(printer.getStateInLabel(loc, locId, numTransOut));
        state = addState(chart, label, ...
            [posX + POS.WS, POS.Y0, POS.W, POS.H], newLineChar);
    end
    
    function [ state ] = addStateDwell( loc, state_inPos )
    % adds a dwell-state
    % - dwell until either the dwelling time is reached and a transition is
    %   enabled or until the invariant is violated
        
        label = char(printer.getStateDwellLabel(loc));
        state = addState(chart, label, ...
            [state_inPos(1), state_inPos(2) + state_inPos(4) + 2 * POS.H, ...
            POS.W, POS.H], newLineChar);
    end
    
    function [ state ] = addStateChoose( loc, state_inPos )
    % adds a state choosing the dwelling time
    % - restores the variables (backtracking)
    % - chooses the next dwelling time
    
        label = char(printer.getStateChooseLabel(loc));
        state = addState(chart, label, ...
            [state_inPos(1), ...
            state_inPos(2) + state_inPos(4) + POS.J_H + POS.H, ...
            POS.W, POS.H], newLineChar);
    end
    
    function [ junction ] = addJunctionIn2dwell( state_inPos )
    % adds a junction for choosing transitions
    % - goes to choose-state when there are transitions left
    % - goes to dwell-state when there are no transitions left
    
        junction = addJunction(chart, ...
            [state_inPos(1) + state_inPos(3) * 0.5, ...
            state_inPos(2) + state_inPos(4) + POS.J_H]);
    end
    
    function [ junction ] = addJunctionDwell2next( state_dwellPos )
    % adds a junction for leaving the dwell-state
    
        junction = addJunction(chart, ...
            [state_dwellPos(1) + state_dwellPos(3) * 0.5, ...
                state_dwellPos(2) + state_dwellPos(4) + POS.J_H]);
    end
    
    function [ junction ] = addJunctionDwell2choose(state_choosePos, state_dwellPos)
    % adds a junction connecting a dwell state and a choose state (backtracking)
    
        chooseBottom = state_choosePos(2) + state_choosePos(4);
        junction = addJunction(chart, ...
            [state_dwellPos(1) + state_dwellPos(3) * 0.9, ...
                chooseBottom + ...
                (state_dwellPos(2) - chooseBottom - POS.J_H) * 0.8]);
    end
    
    function [ junction ] = addJunctionTrans( baseX, baseY, iTrans )
    % adds a junction for a transition (one for each)
    
        junction = addJunction(chart, [baseX, baseY + (iTrans * POS.J_H)]);
    end
    
    function [ initIdx ] = addTransitionInit2in( loc, state_init, state_in, initIdx, constants )
    % adds a transition connecting the initial state and an entry state
    % (if the location is initial)
    
        % get label from Java
        label = char(printer.getTransitionInit2inLabel(loc, initIdx, constants));
        
        % the label is non-empty if and only if the location is initial
        if (~ isempty(label))
            initIdx = initIdx + 1;
            transition = ...
                addTransition(chart, state_init, state_in, label, 2, 11);
            transition.LabelPosition(1) = transition.DestinationEndPoint(1) + 5;
            transition.LabelPosition(2) = transition.DestinationEndPoint(2) - ...
                transition.LabelPosition(4) - 5;
        end
    end
    
    function addTransitionIn2choose( state_in, junction_in2dwell )
    % adds a transition 'entry state -> transition choosing junction'
    
        addTransition(chart, state_in, junction_in2dwell, '', 6, 0);
    end
    
    function addTransitionDwell2leave( state_dwell, junction_dwell2next )
    % add transition 'dwell state -> leave junction'
    % = dwelling successful
    
        label = char(printer.getTransitionDwell2leaveLabel());
        transition = addTransition(chart, state_dwell, ...
            junction_dwell2next, label, 6, 0);
        midPt = transition.MidPoint;
        transition.LabelPosition(1) = midPt(1) + 5;
        transition.LabelPosition(2) = midPt(2) - 5;
    end
    
    function addTransitionDwell2backtrack( state_dwell, junction_dwell2choose, loc, optEagerViolation )
    % adds a transition 'dwell state -> backtrack junction'
    % = invariant violation
    
        label = char(printer.getTransitionDwell2backtrackLabel(loc));
        transition = addTransition(chart, state_dwell, ...
            junction_dwell2choose, label, 1, 6);
        transition.MidPoint(1) = transition.MidPoint(1) - 10;
        transition.DestinationEndPoint(1) = ...
            transition.DestinationEndPoint(1) - 5;
        midPt = transition.MidPoint;
        transition.LabelPosition(1) = ...
            midPt(1) - transition.LabelPosition(3) - 5;
        transition.LabelPosition(2) = midPt(2) - 5;
        
        if (optEagerViolation)
            % execute this transition first if option is set accordingly
            transition.ExecutionOrder = 1;
        end
    end
    
    function addTransitionLeave2dwell( junction_dwell2next, state_dwell )
    % adds a transition 'leave junction -> dwell state'
    % = maximum simulation time passed: stop simulation
    
        label = char(printer.getTransitionLeave2dwellLabel());
        transition = addTransition(chart, junction_dwell2next, ...
            state_dwell, label, 9, 7);
        endPt = transition.DestinationEndPoint;
        transition.LabelPosition(1) = ...
            endPt(1) - transition.LabelPosition(3) - 5;
        transition.LabelPosition(2) = endPt(2) + 5;
    end
    
    function addTransitionChoose2dwell( state_choose, state_dwell )
    % adds a transition 'choose state -> dwell state'
    % = try the next time choice
    
        label = char(printer.getTransitionChoose2dwellLabel());
        transition = addTransition(chart, state_choose, state_dwell, ...
            label, 7, 11);
        midPt = transition.MidPoint;
        transition.LabelPosition(1) = ...
            midPt(1) - transition.LabelPosition(3) - 5;
        transition.LabelPosition(2) = midPt(2) - 10;
    end
    
    function addTransitionChooseJ2choose( junction_in2dwell, state_choose )
    % adds a transition 'transition choose junction -> choose state'
    % = try the next (original) transition
    
        label = char(printer.getTransitionChooseJ2chooseLabel());
        transition = addTransition(chart, junction_in2dwell, ...
            state_choose, label, 6, 0.5);
        transition.MidPoint(1) = transition.MidPoint(1) + 10;
        transition.DestinationEndPoint(1) = ...
            transition.DestinationEndPoint(1) + 5;
        midPt = transition.MidPoint;
        transition.LabelPosition(1) = midPt(1) + 5;
        transition.LabelPosition(2) = midPt(2) - 25;
    end
    
    function addTransitionChoose2chooseJ( state_choose, junction_in2dwell )
    % adds a transition 'choose state -> transition choose junction'
    % = backtracking (to choose the next transition)
    
        label = char(printer.getTransitionChoose2chooseJLabel());
        transition = addTransition(chart, state_choose, ...
            junction_in2dwell, label, 11.5, 6);
        transition.MidPoint(1) = transition.MidPoint(1) - 10;
        transition.DestinationEndPoint(1) = ...
            transition.DestinationEndPoint(1) - 5;
        midPt = transition.MidPoint;
        transition.LabelPosition(1) = ...
            midPt(1) - transition.LabelPosition(3) - 5;
        transition.LabelPosition(2) = midPt(2) - 10;
    end
    
    function addTransitionChooseTime2dwell( junction_dwell2choose, state_choose )
    % adds a transition 'time choose junction -> dwell state'
    % = choose new dwelling time
    
        label = char(printer.getTransitionChooseTime2dwellLabel());
        transition = addTransition(chart, junction_dwell2choose, ...
            state_choose, label, 0, 5);
        endPt = transition.DestinationEndpoint;
        transition.LabelPosition(1) = endPt(1) + 5;
        transition.LabelPosition(2) = endPt(2) + 5;
    end
    
    function addTransitionInJ2dwell( junction_in2dwell, state_dwell )
    % adds a transition 'entry junction -> dwell state'
    % = no transitions are left
    
        label = char(printer.getTransitionInJ2dwellLabel());
        transition = addTransition(chart, junction_in2dwell, state_dwell, ...
            label, 9, 9);
        state_dwellPos = state_dwell.Position;
        transition.DestinationEndPoint(1) = state_dwellPos(1);
        transition.DestinationEndPoint(2) = ...
            state_dwellPos(2) + (state_dwellPos(4) * 0.5);
        transition.SourceOClock = 9;
        transition.MidPoint(1) = transition.MidPoint(1) + 30;
        midPt = transition.MidPoint;
        transition.LabelPosition(1) = ...
            midPt(1) - transition.LabelPosition(3) - 5;
        transition.LabelPosition(2) = midPt(2) - 25;
    end
    
    function addTransitionBacktrack2dwell( junction_dwell2choose, state_dwell )
    % adds a transition 'backtrack junction -> dwell state'
    % = deadlock: stop simulation
    
        label = char(printer.getTransitionBacktrack2dwellLabel());
        transition = addTransition(chart, junction_dwell2choose, ...
            state_dwell, label, 6, 0);
        transition.MidPoint(1) = transition.MidPoint(1) + 10;
        transition.DestinationEndPoint(1) = ...
            transition.DestinationEndPoint(1) + 5;
        midPt = transition.MidPoint;
        transition.LabelPosition(1) = midPt(1) + 5;
        transition.LabelPosition(2) = midPt(2) - 10;
    end
    
    function addTransitionLeave2trans( junction_dwell2next, junction_trans, iTrans )
    % adds a transition 'leave junction -> transition junction'
    
        label = char(printer.getTransitionLeave2transLabel(iTrans));
        if (mod(iTrans, 2) == 0)
            % go right
            transition = addTransition(chart, junction_dwell2next, ...
                junction_trans, label, 3, 3);
            midPt = transition.MidPoint;
            transition.LabelPosition(1) = midPt(1) + 5;
        else
            % go left
            transition = addTransition(chart, junction_dwell2next, ...
                junction_trans, label, 9, 9);
            midPt = transition.MidPoint;
            transition.LabelPosition(1) = ...
                midPt(1) - transition.LabelPosition(3) - 5;
        end
        transition.LabelPosition(2) = midPt(2) - 5;
    end
    
    function addTransitionTrans2in( junction_trans, state_in, trans, juncX )
    % adds a transition 'transition junction -> entry state'
    % = original transition
    
        label = char(printer.getTransitionTrans2inLabel(trans));
        state_inPos = state_in.Position;
        if (state_inPos(1) > juncX)
            % go right
            transition = addTransition(chart, junction_trans, state_in, ...
                label, 3, 9);
            transition.DestinationOClock = 9;
            transition.SourceOClock = 3;
            transition.DestinationEndPoint(1) = state_inPos(1);
        elseif (state_inPos(1) + state_inPos(3) > juncX)
            % self-loop
            transition = addTransition(chart, junction_trans, state_in, ...
                label, 9, 9);
            transition.DestinationOClock = 9;
            transition.SourceOClock = 9;
            transition.DestinationEndPoint(1) = state_inPos(1);
        else
            % go left
            transition = addTransition(chart, junction_trans, state_in, ...
                label, 9, 3);
            transition.DestinationOClock = 3;
            transition.SourceOClock = 9;
            transition.DestinationEndPoint(1) = state_inPos(1) + state_inPos(3);
        end
        transition.DestinationEndPoint(2) = POS.Y0 + (state_inPos(4) * 0.5);
        midPt = transition.MidPoint;
        transition.LabelPosition(1) = midPt(1) + 5;
        transition.LabelPosition(2) = midPt(2);
    end
    
    function addTransitionInitial( state_init, numTransMax, initIdx )
    % adds a transition to the initial state
    % NOTE: This depends on the maximum number of outgoing transitions and the
    % number of initial locations.
    
        label = char(printer.getTransitionInitLabel(numTransMax, initIdx));
        transition = addTransition(chart, [], state_init, label, 6, 0);
        srcX = state_init.Position(1) + state_init.Position(3) / 2;
        transition.SourceEndPoint = [srcX, state_init.Position(2) - 20];
        transition.SourceOClock = 6;
        transition.LabelPosition(1) = srcX + 5;
        transition.LabelPosition(2) = ...
            transition.DestinationEndPoint(2) - transition.LabelPosition(4);
    end

% end of main function
end


function addAuxiliaryVariables( chart, printer, numTransMax, numRandMax )
% adds the auxiliary variables
% These are the variables not being part of the model, but used for controlling
% the simulation (like the dwelling time, backtracking, etc.).

    varIt = printer.getVariables(numTransMax, numRandMax);
    % add variables
    while (varIt.hasNext())
        var = varIt.next();
        sfVar = Stateflow.Data(chart);
        sfVar.Name = char(var.name);
        propIt = var.props.iterator();
        while (propIt.hasNext())
            prop = propIt.next();
            sfVar.setPropValue(char(prop.name), char(prop.value));
        end
    end
end


function addConstants( chart, constants, PREFIX_VARIABLE )
% adds constants and initializes them

    itConst = constants.entrySet().iterator();
    while (itConst.hasNext())
        constant = itConst.next();
        var = addVariable(chart, [PREFIX_VARIABLE, constant.getKey()], ...
            'Constant');
        assert(~ isempty(constant.getValue()), 'Constant not defined.');
        var.Props.InitialValue = num2str(constant.getValue().middle());
    end
end


% --- functions for adding Stateflow components ---


function [ state ] = addState( chart, label, position, newLineChar )
% adds a state with a label and a size/position

    % fit state size to fully contain the string
    global G_IS_PRETTY_PRINT;
    if (G_IS_PRETTY_PRINT)
        charWidth = 6;
        charHeight = 17;

        lineCount = 1;
        columnCount = 0;
        lastReset = 0;
        for i = 1 : length(label)
            if (strcmp(label(i), newLineChar))
                lineCount = lineCount + 1;
                columnCount = max(columnCount, i - lastReset);
                lastReset = i;
            end
        end
        columnCount = max(columnCount, length(label) - lastReset);

        position(3) = charWidth * columnCount;
        position(4) = charHeight * lineCount;
    end

    state = Stateflow.State(chart);
    state.LabelString = label;
    state.Position = position;
end


function [ junction ] = addJunction( chart, center )
% adds a junctions at a center position

    junction = Stateflow.Junction(chart);
    junction.Position.Center = center;
    junction.Position.Radius = 2;
end


function [ transition ] = addTransition( chart, src, dest, label, srcO, dstO )
% adds a new transition with a label and a source/destination clock angle

    transition = Stateflow.Transition(chart);
    transition.Source = src;
    transition.Destination = dest;
    transition.LabelString = label;
    transition.SourceOClock = srcO;
    transition.DestinationOClock = dstO;
end


function [ var ] = addVariable( chart, name, scope, update )
% adds a variable
    var = Stateflow.Data(chart);
    var.Name = name;
    var.Scope = scope;
    if (exist('update', 'var'))
        var.Update = update;
    end
end


function addAndLinkRandomBlock( printer, modelName, blockName, pos, minString, maxString, chartName, chartInputPort )
% adds a random block and connects it to the chart

    add_block('dspsrcs4/Random Source', [modelName, '/', blockName], ...
        'Position', pos, 'SrcType', 'Uniform', 'SampMode', 'Continuous', ...
        'MinVal', minString, 'MaxVal', maxString, 'RepMode', ...
        'Specify seed', 'rawSeed', char(printer.V_seed.name));
    line = add_line(modelName, [blockName, '/1'], ...
        [chartName, '/', num2str(chartInputPort)], 'autorouting', 'on');
    set_param(line, 'Name', blockName);
end


function linkToScope( modelName, chartName, outPort, inPort, lineName )
% connects the chart with the scope wrt. given ports

    line = add_line(modelName, [chartName, '/', num2str(outPort)], ...
        ['Scope/', num2str(inPort)], 'autorouting', 'on');
    set_param(line, 'Name', lineName);
end


function assignStandardModelParameters( printer )
% assigns standard values to model parameters to compile it directly
% NOTE: Especially the maximum simulation time parameter is model dependent and
% hence should be overwritten by a user.

    rng('default');
    assignin('base', char(printer.V_maxT.name), 1); % maximum simulation time
    assignin('base', char(printer.V_eps.name), 0.0001); % epsilon
    assignin('base', char(printer.V_maxResets.name), 3); % # backtrackings
    assignin('base', char(printer.V_seed.name), randi(1e6)); % random seed
end
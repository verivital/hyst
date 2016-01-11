function translateAutomaton( model, config, options )
%%TRANSLATEAUTOMATON translates a hybrid automaton or a network thereof

    % true: let the component translation add a scope block and signals
    % false: add the scope block and signals after component translation
    % TODO(X) For now, we use network features only for the nonsemantics version
    isAddSignals = options.semantics;
    
    % maps input and output variables to respective component index
    inputVars = java.util.HashMap();
    outputVars = java.util.HashMap();
    
    % translate each component normally
    if isa(config.root.template, 'com.verivital.hyst.ir.network.NetworkComponent')
        componentsMap = config.root.template.children;
        componentsIt = componentsMap.entrySet().iterator();
        idx = 1;
        while (componentsIt.hasNext())
            component = componentsIt.next();
            [charts(idx), inputVars, outputVars] = ...
                addNetworkComponent(model, component, options, config, idx, ...
                    isAddSignals, inputVars, outputVars);
            idx = idx + 1;
        end
    elseif isa(config.root.template, 'com.verivital.hyst.ir.base.BaseComponent')
        % TODO: need to fix models with only base components (e.g., cruise
        % control), as the changes to support networks has broken this.
        %
        %[charts(1), inputVars, outputVars] = ...
        %    addNetworkComponent(model, config.root, options, config, 1, ...
        %        isAddSignals, inputVars, outputVars);
    end
    
    % post-processing: add a scope block and signals if activated
    if (~ isAddSignals)
        addScope(outputVars.size(), model);
        nScopePort = 0;
        nDelayBlocks = 0;
        modelName = model.Name;
        
        outputsIt = outputVars.entrySet().iterator();
        while (outputsIt.hasNext())
            output = outputsIt.next();
            outputName = output.getKey();
            outputInfo = output.getValue();
            inputs = inputVars.get(outputName);
            
            % connect port to scope
            nScopePort = linkPortToScope(modelName, outputName, outputInfo, ...
                    charts, nScopePort);
            
            if (~ isempty(inputs))
                % variable is also used as input for other components
                
                % add unit delay block
                nDelayBlocks = addUnitDelayBlock(modelName, nDelayBlocks);
                
                % connect port to unit delay
                linkPortToUnitDelay(modelName, outputName, outputInfo, ...
                    charts, nDelayBlocks);
                
                % connect unit delay to input ports
                for i = 1 : size(inputs, 2)
                    linkUnitDelayToPort(modelName, outputName, nDelayBlocks, ...
                        charts(inputs(1, i)).Name, inputs(2, i));
                end
            end
        end
    end
end


function [ chart, inputVars, outputVars ] = addNetworkComponent( model, component, options, config,  componentIdx, isAddSignals, inputVars, outputVars )
%% adds an automaton network component

    componentName = component.getKey();
    componentHa = component.getValue().child;
    dynamicType = com.verivital.hyst.util.Classification.classifyAutomaton(componentHa);
    if (componentIdx == 1)
        % first chart is already present (the only one)
        chart = model.find('-isa', 'Stateflow.Chart');
        chart.Name = char(componentName);
%         chartPos = [25, 20, 85, 70]; % standard position
    else
        % add new chart
        yOffset = (componentIdx - 1) * 100;
        chartPos = [25, yOffset + 20, 85, yOffset + 70];
        add_block('sflib/Chart', [model.Name, '/', componentName], ...
            'Position', chartPos);
        chart = model.find('-isa', 'Stateflow.Chart', '-and', ...
            'Name', componentName);
    end
    
    % update method of the chart
    % mathworks.com/help/stateflow/ug/setting-the-stateflow-block-update-method.html?searchHighlight=Update  method
    % INHERITED, DISCRETE, CONTINUOUS
    if (isContinuous(dynamicType))
        % update chart method type to be continuous
        chart.ChartUpdate = 'CONTINUOUS';
    else
        chart.ChartUpdate = 'DISCRETE';
        chart.SampleTime ='1/50000'; % TODO: why is this being set to a magic number?
    end
    % LUAN TODO next: refactor and merge these, all of this should be the same
    % for the semantics vs. non-semantics preserving converters
    %basecomponent
    if (options.semantics)
        com.verivital.hyst.passes.flatten.FlattenAutomatonPass.flattenAndOptimize(config);
        %ha = ... flatten here
        ha = config.root;
        
        % TODO: MERGE THESE ALREADY; use the SAME functions with options to
        % keep it simple for yourself, e.g., pass in something like
        % opt_semantics to all the functions, and have them just print the
        % different things based on the opt_semantics value...
        % to make it explicit: there should only be one function, you need
        % to merge semanticTranslation and nonsemanticTranslation

        semanticTranslation(chart, config, ha, model.Name, options.eager_violation);
    else     
        %[sF] = nonsemanticTranslation(model, chart, config, options.cfg);
        [inputVars, outputVars, ~] = nonsemanticTranslation(isContinuous(dynamicType),model, chart, ...
            componentName, componentHa, config, componentIdx, ...
            isAddSignals, inputVars, outputVars);
    end
end


function [ answer ] = isContinuous(dynamicType)
%% determines whether the component is continuous or not

    % TODO(X) currently, only components which contain the name 'controller' are
    % considered discrete 
    %answer = isempty(strfind(char(component.getKey()), 'controller'));
    if strfind(dynamicType,'DISCRETE')
        answer = false;
    else
       answer = true;
    end
end

function addScope(num_port, model)
%% adds a scope block to see the outputs

    add_block('built-in/Scope', [model.Name '/Scope'],...
        'NumInputPorts', num2str(num_port), ...
        'Position' ,[350, 25, 400, 100],...
        'SaveToWorkspace', 'on', ...
        'SaveName', 'ScopeData', ...
        'DataFormat', 'Structure', ...
        'LimitDataPoints', 'off');
end


function link( modelName, outputName, source, target )
%% connects a source to the scope block

    line = add_line(modelName, source, target, 'autorouting', 'on');
    if (~ isempty(outputName))
        set_param(line, 'Name', outputName);
    end
end


function [ nScopePort ] = linkPortToScope( modelName, outputName, outputInfo, charts, nScopePort )
%% connects an ouput port to the scope block

    nScopePort = nScopePort + 1;
    source = [charts(outputInfo(1, 1)).Name, '/', num2str(outputInfo(2, 1))];
    target = ['Scope/', num2str(nScopePort)];
    link( modelName, outputName, source, target );
end


function [ nDelayBlocks ] = addUnitDelayBlock( modelName, nDelayBlocks )
%% adds a unit delay block

    yOffset = nDelayBlocks * 40;
    nDelayBlocks = nDelayBlocks + 1;
    add_block('built-in/UnitDelay', ...
        [modelName '/UnitDelay', num2str(nDelayBlocks)], ...
        'Position',[150, 25 + yOffset, 180, 55 + yOffset], ...
        'SampleTime', '1/500000');
end


function linkPortToUnitDelay( modelName, outputName, outputInfo, charts, nUnitDelay )
%% connects an ouput port to a unit delay block

    source = [charts(outputInfo(1, 1)).Name, '/', num2str(outputInfo(2, 1))];
    target = ['UnitDelay', num2str(nUnitDelay), '/1'];
    link( modelName, outputName, source, target );
end


function linkUnitDelayToPort( modelName, outputName, nUnitDelay, inputName, inputPort )
%% connects a unit delay block to an input port

    source = ['UnitDelay', num2str(nUnitDelay), '/1'];
    target = [inputName, '/', num2str(inputPort)];
    link( modelName, outputName, source, target );
end
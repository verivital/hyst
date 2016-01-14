function [time, valuesALL, labels] = simulationLoop(model, iterations, maxTime, maxResets, epsilon, filter, seed_option, plot_option, plotdata)
%SIMULATIONLOOP Iteratively simulates single trajectories and plots them all
%
% simulationLoop(model, iterations, maxTime, maxResets, epsilon, filter, seed_option, plot_option, plotdata)
%
% example usage: simulationLoop('mymodel', 10, 20, 5, 0.1, {}, 1, 1)
%
% seed_option:
% 1 = always shuffle the random seed in each iteration
% 2 = make results reproducible
%
% plot_option:
% 0 = one plot
% 1 = subplots for each variable
%
% ------------------------------------------------------------------------------
% authors: Christian Schilling, Taylor T. Johnson
% ------------------------------------------------------------------------------
%    global time values labels ScopeData;    

    % add java library
    javaaddpath(['..', filesep, '.', filesep, 'lib', filesep, 'Hyst.jar']);
    % add functions subfolders to path
    addpath(genpath('functions'));
    
    % open model
%     open_system(['./output/' model]);
    if isequal(exist(['./output_slsf_models/' model '.mdl'], 'file'),2) 
        % if the model file exists
        load_system(['./output_slsf_models/' model])
        %load_system(['./output/' model])
        % plot_option enum cases
        plot_phase = 2; % show phase portrait
        opt_plot = 1; % 0 = do not plot, 1 = do plot

        plot_paper = 3; %do comparision between SLSF plot and Flow*, SpaceEx, etc
        count = 1;
        useFastRestart = 1;

        opt_config_change = 0;
        opt_time = 1;

        if opt_time
            tic;
        end

        % holds names of model parameters
        printer = com.verivital.hyst.printers.SimulinkStateflowPrinter();

        % set standard values
        if ((nargin < 8) || (plot_option == -1))
            plot_option = 1;
        end
        if ((nargin < 7) || (seed_option == -1))
            seed_option = 1;
        end
        if (nargin < 6)
            filter = cell(1, 0);
        end
        if ((nargin < 5) || (epsilon == -1))
            epsilon = 0.0001;
        end
        if ((nargin < 4) || (maxResets == -1))
            maxResets = 3;
        end

        if ((nargin < 3) || (maxTime == -1))
            % try to load maximum simulation time from a model constant
            rt = sfroot;
            modelInstance = rt.find('-isa', 'Simulink.BlockDiagram');
            chart = modelInstance.find('-isa', 'Stateflow.Chart', ...
                '-and', 'Name', ['SF_', model]);
            for i = 1 : 4
                switch i
                    % TODO: we should handle this max time detection in Hyst,
                    % as this shows up as a common problem in translation
                    % between tool formats
                    %
                    % Solution idea: search like as done here
                    % Set a hyst parameter somewhere that corresponds with the
                    % maximum time (e.g., as a field)
                    % Perhaps this should be done in the configuration options.
                    %
                    % SURE: JUST PLEASE DO IT THERE AS OTHERWISE WE HAVE TO DO IT IN EACH OUTPUT FORMAT INSTEAD OF AT INTERMEDIATE LEVEL.

                    case 1
                        x = chart.find('Name', [char(printer.PREFIX_VARIABLE), 'Tmax']);
                    case 2
                        x = chart.find('Name', [char(printer.PREFIX_VARIABLE), 'maxT']);
                    case 3
                        x = chart.find('Name', [char(printer.PREFIX_VARIABLE), 'tmax']);
                    case 4
                        x = chart.find('Name', [char(printer.PREFIX_VARIABLE), 'maxt']);
                end
                if (~ isempty(x))
                    break;
                end
            end
            if (~ isempty(x))
                maxTime = str2double(x.Props.InitialValue);
            else
                fprintf('Error: not able to infer a maximum simulation time');
                return;
            end
        end
        
        ['Seed option: ', num2str(seed_option)]

        % deactivate zero crossing detection
    %     set_param(model, 'MaxConsecutiveZCsMsg', 'none');

        % initialize plot related data
        %figure();
        hold on; % activate several plots in one figure

        % set colors
        %colors = ['rgbcmyk'];
        colors = ['gbcmk'];
        %colors = 'k';

        % set random seed to default for reproducing results
        if (seed_option)
            rng('default');
        end

        % set model parameters
        assignin('base', char(printer.V_maxT.name), maxTime); % maximum simulation time
        assignin('base', char(printer.V_eps.name), epsilon); % epsilon for equality-interval
        assignin('base', char(printer.V_maxResets.name), max(maxResets, 1)); % # backtrackings

        % additional configuration
        % simulation_config(model,maxTime,1);
        % TODO, load data from specifice directory


        for i = 1 : iterations
            if opt_time
                fprintf(['start iteration: ', num2str(i), ' at ', num2str(toc), '\n']);
            end

            % set model parameter: fresh random seed
            assignin('base', char(printer.V_seed.name), randi(1e6));

             if opt_config_change
    %             configSet_mdl = configSet.copy;
    %             configSetNames = get_param(configSet_mdl, 'ObjectParameters');
    % 
    %             cfg_name = get_param(configSet_mdl, 'Name')
    %             try
    %                 cfg_max_timestep = get_param(configSet_mdl, 'MaxStep')
    %                 set_param(configSet_mdl, 'MaxStep', num2str(rand(1,1) * maxTime / 1000));
    %             end
    %             set_param(configSet_mdl, 'Name', ['rand', cfg_name, num2str(i)]);
    %             cfg_name = get_param(configSet_mdl, 'Name')
    %             try
    %                 cfg_max_timestep = get_param(configSet_mdl, 'MaxStep');
    %             end
    %             attachConfigSet(model, configSet_mdl, false);
    %             setActiveConfigSet(model, cfg_name);

                 simulation_config(model,maxTime,rand(1,1));
            end


            % simulate model
            if (useFastRestart)
                % works with fast restart
                set_param(model, 'SimulationCommand', 'start');
                while (strcmp(get_param(model, 'SimulationStatus'), 'running'))
                    pause(0.05);
                end
                ScopeData = evalin('base', 'ScopeData');
            else
                % standard way (but recompiles each time)
                [~] = sim(model);

                % problem with scopedata if we change parameters here; maybe set
                % active config set elsewhere before simulation loop? doesn't seem
                % to be a problem to set this in general (e.g., manually in the
                % model), just something strange with this particular command
                %[~] = sim(model, 'MaxStep', '1e-4');
                %[~] = sim(model, configSet);
            end

            % read data from scope block
            [time, values, labels] = resultsToMatrix(ScopeData, filter, 1);

            valuesALL(i).values = values;

            NUM_PLOTS = size(labels, 2);
            if opt_plot
                if (plot_option == plot_paper)
                        if (count == 1)
                            try
                                plot_2d_vertices(plotdata, 'y');
                                count = 0;
                            catch
                                disp('cant not find the reachable data set');
                            end
                        end    
                        color = colors(1 + mod(i, length(colors)));  
                        hold on;
                        % plot the simulation traces and reachable set of specified variable in a single diagram
                        %scatter(time', values(1, :)', color,'o');
                        plot(time', values(2, :)', color);
                        xlabel('time');
                        ylabel(labels{2});                       
                else
                    if (i == 1)
                        % initialize plot structure
                        p = zeros(1, NUM_PLOTS);
                        for j = 1 : NUM_PLOTS
                        p(j) = subplot(NUM_PLOTS, 1, j);
                        xlabel(p(j), 'time');
                        ylabel(p(j), labels{j});
                        hold(p(j), 'on');
                        end
                    end
                    plotSimulationLoop(time, values, labels, plot_option, plot_phase, p, colors, i);
                end
            end
        end

        % deactivate several plots again
        hold off;
    else
        disp('The model file does not exist!!! Please try again with another');
    end
end

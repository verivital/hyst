function [ timeVector, dataMatrix, labels ] = resultsToMatrix( scopeData, filter, timePort )
% reads scope data and writes them to a time vector and a data matrix
%
% ------------------------------------------------------------------------------
% author: Christian Schilling
% ------------------------------------------------------------------------------

    % number of scope ports
    numScopePorts = length(scopeData.signals);
    
    % read data from scope
    dataCell = cell(numScopePorts, 1);
    [dataCell{:, 1}] = deal(scopeData.signals.values);
    data = dataCell(:, 1);
    
    % number of auxiliary variables ignored in the scope block
    % currently: 'backtrack' and 'time'
    if timePort == 1 % from simulationCommand
        OFFSET = 2;
    else % from non-semantics preserving converter
        OFFSET = 0;
    end
    
    % initialize data structures
    columns = length(dataCell{1, 1});
    
    if timePort == 1
        backtrackingVector = zeros(1, columns);
        backtrackingVector(1, :) = data{2};

        % find relevant columns (i.e., forget wrong data due to backtracking)
        oldBacktrack = 0;
        frames = zeros(0, 2);
        startIdx = 1;
        newColumns = 0;
        for i = 1 : columns
            newBacktrack = backtrackingVector(1, i);
            if (newBacktrack ~= oldBacktrack)
                if (newBacktrack < oldBacktrack)
                    % transition was taken successfully
                    endIdx = i - 1;
                    frames(size(frames, 1) + 1, :) = [startIdx, endIdx];
                    newColumns = newColumns + endIdx - startIdx + 1;
                end
                startIdx = i;
                oldBacktrack = newBacktrack;
            end
        end
        endIdx = i;
        frames(size(frames, 1) + 1, :) = [startIdx, endIdx];
        newColumns = newColumns + endIdx - startIdx + 1;

        from = 1;
        timeVector = zeros(1, newColumns);
        dataMatrix = zeros(numScopePorts - OFFSET, newColumns);
        for i = 1 : size(frames, 1);
            frame = frames(i, :);
            lower = frame(1, 1);
            upper = frame(1, 2);
            to = from + upper - lower;

            % copy values
            timeVector(1, from : to) = data{timePort}(lower : upper, 1);
            for k = (1 + OFFSET) : size(data, 1)
                dataMatrix(k - OFFSET, from : to) = data{k}(lower : upper, 1);
            end

            from = to + 1;
        end
    else
        timeVector(1, :) = data{timePort}(:, 1);
        for k = (1 + OFFSET) : size(data, 1)
            dataMatrix(k - OFFSET, :) = data{k}(:, 1);
        end

    end
    
    % find labels and filter away unwanted variables
    VARS = size(dataCell, 1) - OFFSET;
    FILTER_SIZE = size(filter, 2);
    if (FILTER_SIZE == 0)
        labels = cell(1, VARS);
        for i = 1 : VARS
            labels{1, i} = scopeData.signals(i + OFFSET).label;
        end
    else
        labels = cell(1, min(VARS, FILTER_SIZE));
        removed = 0;
        for i = 1 : VARS
            label = scopeData.signals(i + OFFSET).label;
            found = (FILTER_SIZE == 0);
            for k = 1 : FILTER_SIZE
                % search for wanted variables if specified
                if (strcmp(label, filter{1, k}))
                    % variable
                    labels{1, i - removed} = label;
                    found = true;
                    break;
                end
            end
            if (~ found)
                % remove this variable
                dataMatrix(i - removed, :) = [];
                removed = removed + 1;
            end
        end
    end
end
function [ T ] = chooseT( currentTime, T, tFail, resets, maxResets, randomNumber )
%CHOOSET Chooses the next dwelling time
% Each time a new location (i.e., state cluster) is visited, a new dwelling time
% is generated. The dwelling time is the point in time the dwelling can end.
% The first time it should lie between the current time and the maximum
% simulation time.
% When the first choice did not work, this means the invariant was violated.
% That information is used for further time choices: we need a dwelling time
% between the current time and the minimum of both the time the invariant was
% violated and the old choice (it may be less than the invariant violation) -
% choosing a time greater than that will result in violation again.
%
% ------------------------------------------------------------------------------
% author: Christian Schilling
% ------------------------------------------------------------------------------

    if (resets == maxResets - 1)
        % the last time a transition is tested we choose T = 0
        T = 0;
    else
        % usual randomization in interval
        % [current time, min(last time choice, fail time)]
        T = random(randomNumber, currentTime, min(T, tFail));
    end
end
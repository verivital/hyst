function [ randomNumber ] = random( randomNumber, min, max )
%RANDOM Returns a random floating point number in the interval (min, max).
% The intended behavior is to return a random number in the closed interval
% [min, max]. Unfortunately, this seems very hard to do in Matlab.
%
% ------------------------------------------------------------------------------
% author: Christian Schilling
% ------------------------------------------------------------------------------

    randomNumber = min + (max - min) * randomNumber;
end
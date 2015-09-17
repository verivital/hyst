function [ A ] = permuteA( A, n, randomArray )
%PERMUTEA Initializes a given array with integers from 1 to n permuted
% Given a positive integer n and an array of size m, the first n entries in the
% array are set to the integer values from [1, n]. The values are permuted in
% order to implement random element choice by stepping through it.
% The entries from (n + 1) to m are of no interest.
%
% ------------------------------------------------------------------------------
% author: Christian Schilling
% ------------------------------------------------------------------------------

    assert(n > 0, ...
        'The location should have at least one outgoing transition.');
    assert(n <= size(A, 2), 'The array should contain at least n entries.');
    
    % permutation of all entries
    len = size(A, 2);
    for i = 1 : n
        j = min(floor(randomArray(i)), len);
        tmp = A(i);
        A(i) = A(j);
        A(j) = tmp;
    end
    
    % post-processing: move smallest elements to front
    j = n;
    for i = 1 : n
        elem1 = A(i);
        if (elem1 > n)
            while (j < len)
                j = j + 1;
                elem2 = A(j);
                if (elem2 <= n)
                    % swap elements
                    A(i) = elem2;
                    A(j) = elem1;
                    break;
                end
            end
            
            % optimization
            if (j == len)
                break;
            end
        end
    end
end
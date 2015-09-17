function [ constants ] = semanticTranslationNetwork( constantList, constants )
%SEMANTICTRANSLATIONNETWORK Semantic translation of network components
% The only thing that happens here is reading constants.
%
% ------------------------------------------------------------------------------
% author: Christian Schilling
% ------------------------------------------------------------------------------

    NUM_CONST = constantList.size();
    for i_const = 0 : NUM_CONST-1
        constant = constantList.getConstantByIndex(i_const);
        constantName = constant.getName();
        constantName = regexprep(char(constantName), 'null', '');
        
        for i = 1: length(constants)
            if (strcmp(constantName, constants{i, 1}))
                if (isempty(constants{i, 2}))
                    % no value so far
                    constants{i, 2} = num2str(constant.getValue());
                end
                break;
            end
        end
    end
end
function [Guard_Label] = Guard_SpaceExToStateflow(ha)
%
% ------------------------------------------------------------------------------
% author: Luan Viet Nguyen
% ------------------------------------------------------------------------------

    ResetAction = '';
    tmp_vars_asgn = '';
    if ~isempty(Trans_Assignment)
        assignment = strsplit(expr_SpaceExToStateflow(Trans_Assignment, EEqualityHandling.singleEq),'&&');
        for i = 1: length(assignment)
            matchAssignment = 0;
            for j = 1: length(variables)
                if strcmp(assignment{i}, strcat(variables{j},'''','=',variables{j}))
                   matchAssignment = 1;
                   break;
                end
            end
            if matchAssignment == 0
                asgn = assignment{i};
                ResetAction = strcat(ResetAction,asgn,';');
            end
        end
        Assignment = regexprep(ResetAction,'''','');
        if (~ isempty(tmp_vars_asgn))
            for i = 1 : length(out_vars)
                tmp_vars_asgn = strcat(tmp_vars_asgn, 10, out_vars{i}, ' = ', variables{i}, '; ');
            end
            Assignment = [Assignment, 10, tmp_vars_asgn];
        end
        if ~isempty(Guard)

            %end
            Guard_Label = [char(strcat('[',Guard,']')),10,'{',Assignment,'}'];
        else
            Guard_Label = ['{',Assignment,'}'];
        end
        
    else
        Assignment = '';
        if ~isempty(Guard)
            Guard_Label = char(strcat('[',Guard,']'));
        else
            Guard_Label = '';
        end
    end
end
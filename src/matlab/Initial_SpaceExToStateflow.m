function [ initialVariables, initialConstants, iniLoc ] = Initial_SpaceExToStateflow(initial, var)
%
% ------------------------------------------------------------------------------
% author: Luan Viet Nguyen
% ------------------------------------------------------------------------------

    initialVariables = ''; %initial states of variables set in cfg file
    initialConstants = ''; %initial states of constants set in cfg file
    i_key = initial.keySet().toArray;
    i_values = initial.values.toArray;
    iniLoc = '';
    if initial.size() > 2
        iniLoc = i_key(1);
        for i_init = 2 : initial.size()
            % matching variables to initial value
            varMatch = 0;
            for j = 1: var.length()
                if strcmp(i_key(i_init), var(j))
                    varMatch = 1;
                    initialVariables = [initialVariables, i_key(i_init),' = ' i_values(i_init) '; '];         
                end
            end
            if varMatch == 0;
                initialConstants = [initialConstants, i_key(i_init),' = ' i_values(i_init) '; '];
            end
        end
    end
end
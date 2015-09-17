function [opt_xml, opt_cfg, opt_semantics, opt_flow, opt_guard, opt_invariant, opt_eager_violation, path_name, xml_name, cfg_name] = option_SpaceExToStateflow(argument)
%
% ------------------------------------------------------------------------------
% author: Luan Viet Nguyen
% ------------------------------------------------------------------------------

   %add xml, cfg option flag
    opt_xml = 0;
    opt_cfg = 0;
    %add debugging option flag
    opt_flow = 0;
    opt_invariant = 0;
    opt_guard = 0;
    opt_semantics = 0;
    opt_eager_violation = 0;
    path_name = ['..', filesep, 'examples', filesep];
    xml_name = '';
    cfg_name = '';
    for i_opt= 1: length(argument)
        if strfind(argument{i_opt},'.xml') 
            opt_xml = 1;   %
            xml_name = argument{i_opt};
            %xml_filepath = ['..', filesep, 'examples', filesep, xml_filename];
        elseif strfind(argument{i_opt},'.cfg') 
            opt_cfg = 1;   %
            cfg_name = argument{i_opt}; 
            %cfg_filepath =  ['..', filesep, 'examples', filesep, cfg_filename];
        elseif strfind(argument{i_opt}, '--folder')
            assert(i_opt < length(argument), 'There must be a folder name following the option.');
            path_name = [path_name, argument{i_opt + 1}, filesep];
        end
    end    
    
    if  find(cellfun(@(s)strcmp(s,'-s'),argument))
        opt_semantics = 1;   % call SpaceExToStateflow('-s') to run with semantics analysis
        fprintf('Converting to Stateflow in a semantics preserving way.\n');
    else
        fprintf('Converting to Stateflow in a direct way.\n');
    end
    
    if  find(cellfun(@(s)strcmp(s,'-f'),argument))
        opt_flow = 1;   % call SpaceExToStateflow('-f') to display flow
    end
    if  find(cellfun(@(s)strcmp(s,'-i'),argument))
        opt_invariant = 1;  % call SpaceExToStateflow('-i') to display Invariant
    end
    if  find(cellfun(@(s)strcmp(s,'-g'),argument))
        opt_guard = 1;      % call SpaceExToStateflow('-g') to display Guard
    end
    if  find(cellfun(@(s)strcmp(s,'--eager_violation'),argument))
        opt_eager_violation = 1; % eager (restrictive) invariant violation checking
    end
    %add a debug mode option (opt_debug) that enables all these displays if 1
    if find(cellfun(@(s)strcmp(s,'debug'),argument))% call SpaceExToStateflow('debug') to enables all these displays
        opt_flow = 1;
        opt_invariant = 1;
        opt_guard = 1;
    end
end
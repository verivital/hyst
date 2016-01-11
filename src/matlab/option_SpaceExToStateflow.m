function [options, path_name, xml_name, cfg_name] = option_SpaceExToStateflow(argument)
%
% ------------------------------------------------------------------------------
% author: Luan Viet Nguyen
% ------------------------------------------------------------------------------

   %add xml, cfg option flag
    %add debugging option flag
    opt_flow = 0;
    opt_invariant = 0;
    opt_guard = 0;
    opt_semantics = 0;
    opt_eager_violation = 0;
    %path_name = ['..', filesep,'..', filesep, 'examples', filesep];
    xml_name = '';
    cfg_name = '';
    path_name='';
    try
        xml_path_name = argument{1};
        xml_path_name = strrep(xml_path_name, '/', filesep);
        xml_path_name = strrep(xml_path_name, '\', filesep);
        if strfind(xml_path_name,'.xml')
            k = strfind(xml_path_name, filesep);
            index = k(length(k));
            path_name = xml_path_name(1:index-1);
            xml_name = xml_path_name(index + 1: end);
        end
        
        if length(argument) > 1 && ~isempty(strfind(argument{2},'.cfg'))
            cfg_path_name = argument{2};
            cfg_path_name = strrep(cfg_path_name, '/', filesep);
            cfg_path_name = strrep(cfg_path_name, '\', filesep);
            cfg_name = cfg_path_name(index + 1: end);
        else
            cfg_name = strrep(xml_name,'.xml','.cfg');
        end
    catch
         throw(MException('hyst:badpath', ['File path ', xml_name,' or ', cfg_name,' is not found.']));
    end
%     for i_opt= 2: length(argument)
%         if strfind(argument{i_opt},'.xml') 
%             opt_xml = 1;   %
%             xml_name = argument{i_opt};
%             %xml_filepath = ['..', filesep, 'examples', filesep, xml_filename];
%         elseif strfind(argument{i_opt},'.cfg') 
%             opt_cfg = 1;   %
%             cfg_name = argument{i_opt}; 
%             %cfg_filepath =  ['..', filesep, 'examples', filesep, cfg_filename];
%         end
%     end    
    
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
    
    options = struct(...
        'semantics', opt_semantics, ...
        'flow', opt_flow, ...
        'guard', opt_guard, ...
        'invariant', opt_invariant, ...
        'eager_violation', opt_eager_violation);
end
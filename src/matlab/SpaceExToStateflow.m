function [out_slsf_model, out_slsf_model_path] = SpaceExToStateflow(varargin)
%SPACEEXTOSTATEFLOW SpaceEx to Stateflow conversion
%
% example call without semantics preservation:
% SpaceExToStateflow('mymodel.xml', 'myconfig.cfg', '--folder', 'myfolder')
% This uses SpaceEx model ..\examples\myfolder\mymodel.xml with myconfig.cfg.
%
% example call with semantics preservation: add '-s' like in the following:
% SpaceExToStateflow('mymodel.xml', 'myconfig.cfg', '--folder', 'myfolder', '-s')
%
% Actual calls on example systems:
%
% 1) Heater Lygeros system
% This model has nondeterminism, so to simulate all executions, we need to
% use the semantics preserving converter. The non-semantics preserving
% converter will work, but it will generate ONE of the infinitely many
% executions (all modulo numeric accuracy of course).
%
% SpaceExToStateflow('heaterLygeros.xml', 'heaterLygeros.cfg', '--folder', 'heaterLygeros') 
% 
% 2) Van Der Pol Oscillator (nonlinear example)
%
% SpaceExToStateflow('vanderpol.xml', 'vanderpol.cfg', '--folder', 'vanderpol')
%
% You can manually add an X-Y graph scope/plot and see that its phase
% portrait looks correct.
%
% ------------------------------------------------------------------------------
% author: Luan Viet Nguyen
% ------------------------------------------------------------------------------

    %Ex: how to instantiate java objects into Matlab
    %add java library for spaceex parsing library (from spaceex2boogie)
    %javaaddpath(['..', filesep, 'lib', filesep, 'Hyst.jar']);
    javaaddpath(['..', filesep,'..', filesep, 'lib', filesep, 'Hyst.jar']);
    addpath(['..', filesep, 'lib', filesep]);
    
    % DO NOT LOAD EXTERNAL LIBRARIES HERE, MUST BE LOADED VIA LINK INTO HYST
    %javaaddpath(['..', filesep, 'lib', filesep, 'commons-cli-1.3.1.jar']);
    
    % check if next works with: javaclasspath
    %javaaddpath(['..', filesep, 'lib', filesep, 'antlr-runtime-4.4.jar']);
    %javaaddpath(['..', filesep, 'lib', filesep, 'junit-4.11.jar']);
    %javaaddpath(['.', filesep, 'lib', filesep, 'spaceex-converter.jar']);
    %import de.uni_freiburg.informatik.swt.spaceexboogieprinter.*;
    
    
    import com.verivital.hyst.automaton.*;
    import com.verivital.hyst.grammar.antlr.*;
    import com.verivital.hyst.grammar.formula.*;
    import com.verivital.hyst.importer.*;
    import com.verivital.hyst.ir.*;
    import com.verivital.hyst.ir.base.*;
    import com.verivital.hyst.ir.network.*;
    import com.verivital.hyst.junit.*;
    import com.verivital.hyst.main.*;
    %import com.verivital.hyst.main.Hyst;
    import com.verivital.hyst.outputparser.*;
    import com.verivital.hyst.passes.*;
    import com.verivital.hyst.passes.basic.*;
    import com.verivital.hyst.passes.complex.*;
    import com.verivital.hyst.passes.flatten.*;
    %import com.verivital.hyst.passes.flatten.FlattenAutomatonPass;
    import com.verivital.hyst.printers.*;
    import com.verivital.hyst.python.*;
    import com.verivital.hyst.simulation.*;
    import com.verivital.hyst.util.*;
    
%     % DO NOT IMPORT THESE
%     import org.apache.commons.cli.*;
% %     import org.apache.commons.cli.CommandLine;
% %     import org.apache.commons.cli.CommandLineParser;
% %     import org.apache.commons.cli.DefaultParser;
% %     import org.apache.commons.cli.HelpFormatter;
% %     import org.apache.commons.cli.Option;
% %     import org.apache.commons.cli.Options;
% %     import org.apache.commons.cli.ParseException;
    
    
    import de.uni_freiburg.informatik.swt.spaceexxmlprinter.*;
    import de.uni_freiburg.informatik.swt.spaxeexxmlreader.*;
    import de.uni_freiburg.informatik.swt.sxhybridautomaton.*;
    
    %
    % add classes and functions subfolders to path
    addpath(genpath('functions'));
    % Specify options from the input argument
    [opt_xml_reader, opt_cfg_reader, opt_semantics, opt_display_flow, ...
        opt_display_guard, opt_display_invariant, opt_eager_violation, ...
        path_name, xml_filename, cfg_filename] = option_SpaceExToStateflow(varargin);
    xml_filepath = [path_name, xml_filename];
    cfg_filepath = [path_name, cfg_filename];
    % add option for using model without .cfg file
    if opt_xml_reader == 1
        try
        % read spaceex XML model file
            xml = xmlread(xml_filepath);  
            [pathstr,name,ext] = fileparts(xml_filepath); 
            
            % matlab functions cannot start with numbers, although some
            % model files may, so replace all these possibilities
            %
            % TODO: put this as a pass / general fix in Hyst in case other tools don't
            % support arbitrary names (I thought we've run into this
            % before...)
            expression = '(^|\.)\d*';
            replace = '${digitToWord($0)}';

            name = regexprep(name,expression,replace);
            
        catch exception
            %if ~exist('..\examples\xml_filename', 'file')
                disp('The xml file does not exist');
                throw(exception)
            %end 
        end
        if opt_cfg_reader == 1 
            try
                cfg_reader = java.io.FileReader(cfg_filepath);
                % create SpaceexDocuments
                % read both spaceex XML model and configuration files
                xml_cfg = de.uni_freiburg.informatik.swt.spaxeexxmlreader.SpaceExXMLReader(xml, cfg_reader);
                doc = xml_cfg.read();
                componentTemplates = com.verivital.hyst.importer.TemplateImporter.createComponentTemplates(doc);
		
                config = com.verivital.hyst.importer.ConfigurationMaker.fromSpaceEx(doc, componentTemplates);
                
                %if opt_debug
                %components = config.root.template.children

                %end
                
            catch exception
               %if ~exist('..\examples\cfg_filename', 'file')
                    disp('The configuration file does not exist');
                    throw(exception);
               %end
            end
        else
            try
                xml_reader = de.uni_freiburg.informatik.swt.spaxeexxmlreader.SpaceExXMLReader(xml);
                %create SpaceexDocuments
                doc = xml_reader.read();
            catch exception
               throw(exception);
            end
        end  
    % create an error message if no xml file found
    else
        throw(MException('ResultChk:BadInput','Argument does not contain xml file, please input the xml file name'));
    end
    
    if isequal(exist(['.', filesep, 'output_slsf_models', filesep], 'dir'),7) == 0
        % if the output directory does not exist, generate it;
        mkdir(['.', filesep, 'output_slsf_models', filesep]);       
    end
    output_path = ['.', filesep, 'output_slsf_models', filesep];
 
    % Import hybidautomaton into SLSF
%     try
%         com.verivital.hyst.main.Hyst.setModeNoValidate();
%     catch ex
%         'Error: cannot set no validate for some reason via com.verivital.hyst.main.Hyst.setModeNoValidate();'
%         ex
%         ex.stack
%     end
%     try
%         com.verivital.hyst.passes.flatten.FlattenAutomatonPass.flattenAndOptimize(config);
%     catch ex
%         'Error: cannot call com.verivital.hyst.passes.flatten.FlattenAutomatonPass.flattenAndOptimize(config);'
%         ex
%         ex.stack
%     end
  
    %com.verivital.hyst.passes.flatten.FlattenAutomatonPass.flattenAndOptimize(config);
    
    %ha = ... flatten here
    %ha = config.root;
    %config;
    
    bdclose(name); % close model (uncoditionally, careful in case this is called and open diagrams are not saved!)
    %
    isNetwork = false;
    % Reference: http://blogs.mathworks.com/seth/2010/01/21/building-models-with-matlab-code/
    % Reference: http://www.mathworks.com/help/stateflow/api/quick-start-for-the-stateflow-api.html
    rt = sfroot;
    m = rt.find('-isa','Simulink.BlockDiagram');
    % chart_ref = m.find('-isa','Stateflow.Chart');
    prev_models = rt.find('-isa','Simulink.BlockDiagram');
    % create new model, and get current models
    sfnew;
    curr_models = rt.find('-isa','Simulink.BlockDiagram');
    % new model is current models - previous models
    m = setdiff(curr_models, prev_models);
    % save model file
    %
    slsf_model_path = [output_path, name, '.mdl'];
    sfsave(m.Name, slsf_model_path);
    %
    
    % Get chart in new model
    ch = m.find('-isa', 'Stateflow.Chart');
    ch.Name = strcat('SF_',name);  
    % Update chart method type to be continuous
    ch.ChartUpdate='CONTINUOUS';
    % LUAN TODO next: refactor and merge these, all of this should be the same
    % for the semantics vs. non-semantics preserving converters
    %basecomponent
    if (opt_semantics)
        com.verivital.hyst.passes.flatten.FlattenAutomatonPass.flattenAndOptimize(config);
        %ha = ... flatten here
        ha = config.root;
        semanticTranslation(ch, config, ha, name, opt_eager_violation);
    else     
        [sF] = nonsemanticTranslation(isNetwork,m,ch,config,opt_cfg_reader);
    end
    
    % Save model file
    %
    sfsave(m.Name, slsf_model_path);
    % output
    out_slsf_model_path = slsf_model_path;
    out_slsf_model = m;
    %out_ha = ha;
end
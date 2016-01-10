function [config, name] = createConfigFromSpaceEx(xml_filepath, cfg_filepath)

    % add option for using model without .cfg file
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
    try
        cfg_reader = java.io.FileReader(cfg_filepath);
        % create SpaceexDocuments
        % read both spaceex XML model and configuration files
        xml_cfg = de.uni_freiburg.informatik.swt.spaxeexxmlreader.SpaceExXMLReader(xml, cfg_reader);
        doc = xml_cfg.read();
        componentTemplates = com.verivital.hyst.importer.TemplateImporter.createComponentTemplates(doc);

        config = com.verivital.hyst.importer.ConfigurationMaker.fromSpaceEx(doc, componentTemplates);

    catch exception
       %if ~exist('..\examples\cfg_filename', 'file')
            disp('The configuration file does not exist');
            throw(exception);
       %end
    end
end
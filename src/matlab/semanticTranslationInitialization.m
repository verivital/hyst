function semanticTranslationInitialization( chart, constants )
%SEMANTICTRANSLATIONINITIALIZATION Initializes constants in the model
% We initialize all constants (possibly defined in several places).
%
% ------------------------------------------------------------------------------
% author: Christian Schilling
% ------------------------------------------------------------------------------

    % create constants and initialize them
    for i = 1: size(constants, 1)
        constant = constants(i, :);
        var = Stateflow.Data(chart);
        var.Name = [AuxVarMap.PREFIX_VARIABLE, constant{1}];
        var.Scope = 'Constant';
        assert(~ isempty(constant{2}), 'Constant not defined.');
        var.Props.InitialValue = constant{2};
    end
end
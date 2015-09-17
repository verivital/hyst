function [flowOut, OutputDescription] = Variable_SpaceExToStateflow(flowIn, var)
%
% ------------------------------------------------------------------------------
% author: Luan Viet Nguyen
% ------------------------------------------------------------------------------

    %Flow = regexprep(regexprep(Flow,'''','_dot'),'&&',';\n');   
    % Create ODEs in SLSF format
    flowOut = '';
    OutputDescription = '';
    output = '';
    for i = 1 : var.length()
        flowOut = [flowOut, flowIn{i},10];
        % parse local variables
        output{i} = strcat(var(i),'_out');
        OutputDescription = [char(OutputDescription),10,output{i},'=',var(i),';'];
    end
end
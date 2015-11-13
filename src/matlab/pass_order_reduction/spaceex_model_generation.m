function [mA,mC,nB,flow,invariant,initialExpression] = spaceex_model_generation(file_name,sys,x_lb,x_ub,u_b,num_loc,varargin)
    % author: LuanNguyen
    % generate hybrid automaton in spaceex format for linear dynamics y = ax
    % + bu
    % input: 
            % file_name: file name
            % sys: the reduced system matrix  
            % x_lb, x_ub: the lower and upper bounds of state variables x
            % u_b: the input bound
            % num_loc: number of discrete locations
            % varargin: options 
   
    % add clock 
    opt_clock = 0;
    if  find(cellfun(@(s)strcmp(s,'-t'),varargin))
        opt_clock = 1;      % added global clock
    end
    [mA,nA] = size(sys.a);
    [mB,nB] = size(sys.b);
    [mC,nC] = size(sys.c);
    
    %% declare variables
    num_var = mA + mC;
    % x variables
    symbolic_variable_x = sym(zeros(mA,1));
    % y variables
    symbolic_variable_y = sym(zeros(mC,1));
    for i = 1: mA
        symbolic_variable_x(i)= sym(['x', num2str(i)], 'real');
    end 
    for i = 1: mC
        symbolic_variable_y(i)= sym(['y', num2str(i)], 'real');
    end 
    % u inputs
    symbolic_input_u = sym(zeros(nB,1));
    for i = 1: nB
        symbolic_input_u(i)= sym(['u', num2str(i)], 'real');
    end 
    %% invariant
    invariant = invariant_generation(sys,symbolic_variable_x,symbolic_variable_y);
    if opt_clock == 1
        if isempty(invariant)
            invariant = ['time <= stoptime'];
        else
            invariant = [invariant,'&&','time <= stoptime'];
        end
    end

    %% generate location with flowdynamics, invariant
    for i_loc = 1 : num_loc
        flow = flow_generation(sys,symbolic_variable_x,symbolic_input_u,opt_clock);
        %locations(i_loc) = loc;  
    end
    
   %% generate initial condition
    % output bound
    y_bound = Initial_output_bound(sys,x_lb,x_ub);
    y_lb = y_bound(1);
    y_ub = y_bound(2);
    
    initialExpression = init_generation(symbolic_variable_x,symbolic_variable_y,symbolic_input_u,...
                       x_lb,x_ub,u_b,y_lb,y_ub);
    if opt_clock == 1
        initialExpression = [initialExpression,'& time == 0 & stoptime = 10'];   
    end    
    
end

%%
function [out] = flow_generation(system_matrix,x,u,opt_clock)
    % generated flow dynamics in SpaceEx format
    flows_all =  system_matrix.a*x(:)+ system_matrix.b*u;
    flow_final = '';
    for i_var = 1: size(x)
        flows = [char(x(i_var)),'=',char(vpa(flows_all(i_var,:),4))];
        if i_var == 1
            flow_final = [flow_final,flows];
        else
            flow_final = [flow_final,'&&',flows];
        end
    end
    if opt_clock == 1
        flow_final = [flow_final,'&&','time = 1'];
    end
    out = flow_final;
end

%%
function [out] = invariant_generation(system_matrix,x,y)
    % generated flow dynamics in SpaceEx format
    invariant_all =  system_matrix.c*x(:);
    inv_final = '';
    for i_var = 1: size(y)
        inv = [char(y(i_var)),'=',char(vpa(invariant_all(i_var,:),4))];
        if i_var == 1
            inv_final = [inv_final,inv];
        else
            inv_final = [inv_final,'&&',inv];
        end
    end
    out = inv_final;
end
%
function [out] = init_generation(x,y,u,x_lb,x_ub,u_b,y_lb,y_ub)
    % generated flow dynamics in SpaceEx format
    init =''; 
    % initial condition of state variables
    for i=1:size(x)
        init = [init printBound(x(i),x_lb(i),x_ub(i))];
        init = [init,'&'];
    end
    for i=1:size(y)
        init = [init printBound(y(i),y_lb(i),y_ub(i))];
        init = [init,'&'];
    end
    for i=1:size(u)
        init = [init printBound(u(i),u_b(i,1),u_b(i,2))];
        if (i < size(u))
            init = [init,'&'];
        end    
    end
    out = init;
end

function [out] = printBound(var,lb,ub)
    str = '';
    if (lb == ub)
        str = [str,char(var),'==',num2str(lb)];
    else
        str = [str,char(var),'>=',num2str(lb),'&',char(var),'<=',num2str(ub)];
    end
    out = str; 
end

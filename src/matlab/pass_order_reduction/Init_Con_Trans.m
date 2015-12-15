function [lb_r, ub_r] = Init_Con_Trans(lb,ub,T,k_order)
%Init_Con_Trans:  This function helps to transform the initial condition from
%full-order model to reduced-order model
% T is balanced transformation matrix
% k_order is the order of the reduced-order model

% author: Hoang-Dung Tran

lb_r = zeros(k_order,1); ub_r = zeros(k_order,1); % upper bound and lower bound of initial conditions for reduced system
for i = 1:k_order
    [v_l_op, lb_r(i)] = linprog(T(i,:),[],[],[],[],lb,ub); % linprog() is a optimization function
    [v_u_op, ub_r(i)] = linprog(-T(i,:),[],[],[],[],lb,ub);
    ub_r(i) = -ub_r(i); 
end

end


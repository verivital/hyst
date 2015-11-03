function y0_bound = Outputbound_for_zero_input_base( sys,lb,ub)
% This function calculate the bound of a SISO system output for the case
% of zero input u = 0; dot(x) = Ax + Bu = Ax; y = Cx;  lb<= x0<= ub

% author: Hoang-Dung Tran

A = sys.a;
C = sys.c;

[mA,nA]= size(A);

P = lyap(A.',eye(mA)); % solve lyapunov equation to find the monotonic transformation matrix
T_mono = P^(-1/2); % monotonic transformation matrix
C_mono = C*T_mono; 
T_new = T_mono^(-1); % matrix for initial condition transform 

lb_mono = zeros(mA,1); ub_mono = zeros(mA,1); % upper bound and lower bound of initial conditions for monotonic original system
for i = 1:mA
    [v_l_op, lb_mono(i)] = linprog(T_new(i,:),[],[],[],[],lb,ub); % linprog() is a optimization function
    [v_u_op, ub_mono(i)] = linprog(-T_new(i,:),[],[],[],[],lb,ub);
    ub_mono(i) = -ub_mono(i); 
end

% calculate 2-norm of x0_mono based on lb_mono and ub_mono
x_temp = zeros(mA,1);
for i = 1:mA 
    if abs(lb_mono(i) >= ub_mono(i))
       x_temp(i) = lb_mono(i);
    else 
        x_temp(i) = ub_mono(i);
    end   
end

x0_mono_2_norm = norm(x_temp,2);
C_mono_2_norm  = norm(C_mono,2);

% calculate e1 
y0_bound = C_mono_2_norm*x0_mono_2_norm;
end




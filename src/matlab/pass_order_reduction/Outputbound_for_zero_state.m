function y_u_bound = Outputbound_for_zero_state(sys,U)
% This function calculate the bound of a system output for the case
% of step reference input u = 1; dot(x) = Ax + Bu; y = Cx;  x0 = 0
% y_u_bound(1) is theoretical bound
% y_u_bound(2) is practical bound based on simulation approach 
% Inputs: 
% 1)  sys: state-space model of the system 
% 2)  U  : vector of control signal inputs u \in R^{mx2}, m = number of
%     control input,    u(m,1) = u_min, u(m,2) = u_max. 
%     for example u = [0.1 0.2; 0.5 0.6; 0.8 1] means the system has 3
%     control inputs u(1) \in [0.1 0.2]; u(2) \in [0.5 0.6]; u(3) \in [0.8 1]
% Outputs: 
% 1)  y_u_bound: is a vector containing the bound of the output
%     y_u_bound(1) is the theorectical results by on theorectical formular
%     y_u_bound(2) is the practical results based on simulation

% author: Hoang-Dung Tran
   
A = sys.a;
B = sys.b;
C = sys.c;
D = sys.d; 

[mC,nC] = size(C);

% calculating theorectical output bound
y_u_theo = zeros(mC,1);

for i = 1:mC
    C_temp = C(i,:);
    sys_new = ss(A,B,C_temp,0);
    [sys_new_b,g,T,Ti] = balreal(sys_new);
    y_u_theo(i) = 2*norm(g,1);
end

% calculating practical output bound

[mU,nU] = size(U);
y_u_prac = zeros(mC,1);
S = stepinfo(sys);
for i = 1:mC
   for j = 1:mU
       y_u_prac(i) = y_u_prac(i) + U(j,2)*S(i,j).Peak;
       
   end
end
y_u_bound = [y_u_theo y_u_prac];
end




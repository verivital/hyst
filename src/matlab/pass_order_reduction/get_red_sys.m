function [sys_r, e2, T] = get_red_sys(sys,k_order)
% This function is used to obtain k-order system from original system using balanced realization.  
% It returns: 
  % 1) k-order model 
  % 2) theretical error bound between the reduced-model and the original model (with zero initial state)
  % 3) the balanced projection matrix used in balanced truncation method
  
  % author : Hoang-Dung Tran

[sysb, g, T, Ti] = balreal(sys);
A_bal = sysb.a;  % get balanced system
B_bal = sysb.b;
C_bal = sysb.c;

[mT,nT] = size(T);

S_A1   = horzcat(eye(k_order),zeros(k_order,mT-k_order));
S_A2   = vertcat(eye(k_order),zeros(mT-k_order,k_order));
S_B  = horzcat(eye(k_order),zeros(k_order,mT-k_order));
S_C = vertcat(eye(k_order),zeros(mT-k_order,k_order));

A_r = S_A1*A_bal*S_A2;
B_r = S_B*B_bal;
C_r = C_bal*S_C;  

sys_r = ss(A_r,B_r,C_r,0);

e2 = 0; % error bound for the case of zero state : x0 = 0; u != 0

for i = 1:(mT - k_order)
    e2 = e2+g(k_order+i);
end 
e2 = 2*e2;
end


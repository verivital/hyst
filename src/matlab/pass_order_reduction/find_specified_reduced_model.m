function [sys_r,lb_r,ub_r,e] = find_specified_reduced_model(sys,lb,ub,U,order)
% This function is used to find a specificed reduced model coressponding to a pre-defined order. 
% This function returns:
% 1) the reduced-model
% 2) the transformed initial set of reduced-model
% 3) the output errors between reduced-model and original model 
%  a) e(1) is theoretical result
%  b) e(2) is mixed theoretical-practical result

% author : Hoang-Dung


% calculate e1
[mC,nC] = size(sys.c);
e1 = zeros(mC,1);
y0 = Outputbound_for_zero_input(sys,lb,ub);
[sys_r, error, T] = get_red_sys(sys,order); 
[lb_r, ub_r] = Init_Con_Trans(lb,ub,T,order);
yr0 = Outputbound_for_zero_input(sys_r,lb_r,ub_r); 
for i = 1:mC
  e1(i) = y0(i)+yr0(i); % e1 is the error caused by initial set  
end

% calculate e2 caused by control inputs u \in U
sys_aug = sys-sys_r;
e2_bound = Outputbound_for_zero_state(sys_aug,U); 

% calculate e = e1 + e2
e_theo = zeros(mC,1);
e_prac = zeros(mC,1); 

for i = 1:mC
    e_theo(i) = e1(i) + e2_bound(i,1);
    e_prac(i) = e1(i) + e2_bound(i,2);
end 

e = [e_theo e_prac e1 e2_bound(:,2)];
end


function y_bound = Outputbound(sys,lb,ub,U)
% This function calculate the bound of a system output under control input U; and initial condition lb<= x0<=ub
% dot(x) = Ax + Bu; y = Cx; u \in U, x0 \in [lb,ub]
% y_bound(1) is theoretical bound
% y_bound(2) is practical bound when yu is determined by step response 

% author: Hoang-Dung Tran


  y0_bound = Outputbound_for_zero_input( sys,lb,ub);
  yu_bound =  Outputbound_for_zero_state(sys,U); 
  
  [mC,nC] = size(sys.c);
  y_bound_theo = zeros(mC,1);
  y_bound_prac = zeros(mC,1);
  
  for i = 1:mC
      y_bound_theo(i) = y0_bound(i) + yu_bound(i,1);
      y_bound_prac(i) = y0_bound(i) + yu_bound(i,2); 
  end
  y_bound = [y_bound_theo y_bound_prac];
end




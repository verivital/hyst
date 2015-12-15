function Y0 = Initial_output_bound(sys,lb,ub)
% This function is used to find the bound of initial output state
% It returns Y0 = [y0_min y0_max]
  % 1) y0_min : the lower bound vector of the initial state of the output
  % 2) y0_max : the uper bound vector of the initial state of the output
  
  % author: Hoang-Dung Tran
  
 C = sys.c; 
 [mC,nC] = size(C);
 y0_min = zeros(mC,1);
 y0_max = zeros(mC,1);
 
 for i = 1:mC
   C_temp = C(i,:);
   [x,y0_min(i)] = linprog(C_temp,[],[],[],[],lb,ub) ; 
   [x,y0_max(i)] = linprog(-C(i,:),[],[],[],[],lb,ub) ;
   y0_max(i) = -y0_max(i);      
 end
   Y0 = [y0_min y0_max];
end


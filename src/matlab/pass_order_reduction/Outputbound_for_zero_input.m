function y0_bound = Outputbound_for_zero_input( sys,lb,ub)
% This function calculate the bound of a system output for the case
% of zero input u = 0; dot(x) = Ax + Bu = Ax; y = Cx;  lb<= x0<= ub

% author: Hoang-Dung Tran

A = sys.a;
B = sys.b;
C = sys.c;
D = sys.d;
[mA,nA]= size(A);
[mC,nC]= size(C);
[mD,nD] = size(D);
D_new = zeros(1,nD);
y0_bound = zeros(mC,1);


for i = 1:mC
    C_new = sys.c(i,:);
    sys_new = ss(A,B,C_new,D_new); 
    y0_bound(i) = Outputbound_for_zero_input_base( sys_new,lb,ub);
end

end




function [P] = Get_Points_From_Lines(A,B)
% This function finds the vertices of a polygon

[mA,nA] = size(A); 
P = zeros(mA,2); 

for i = 1:mA-1
    A_temp = [A(i,1) A(i,2);A(i+1,1) A(i+1,2)];
    B_temp = [B(i);B(i+1)];
    x = -inv(A_temp)*B_temp;
    P(i,1) = x(1);
    P(i,2) = x(2);
end

    A_temp = [A(mA,1) A(mA,2);A(1,1) A(1,2)];
    B_temp = [B(mA);B(1)];
    x = -inv(A_temp)*B_temp;
    P(mA,1) = x(1);
    P(mA,2) = x(2);

end


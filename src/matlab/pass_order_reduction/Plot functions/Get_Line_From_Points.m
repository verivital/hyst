function [ A,B ] = Get_Line_From_Points(P)
% P is a vector of points P = [P1;P2;P3...;Pn] Pi = [xi yi]
% Function returns the line equation Ax+B = 0 for these points

[mP,nP] = size(P); 

A = zeros(mP,2);
B = zeros(mP,1); 

for i = 1:mP-1
    P1 = P(i,:);
    P2 = P(i+1,:);
    [a,b,c] = get_line_equation(P1,P2);
    A(i,1) = a;
    A(i,2) = b;
    B(i) = c;    
end

    P1 = P(mP,:);
    P2 = P(1,:);
    [a,b,c] = get_line_equation(P1,P2);
    A(mP,1) = a;
    A(mP,2) = b;
    B(mP) = c;  

end



function [ a,b,c ] = get_line_equation(P1,P2)
% Give the equation ax + by + c = 0 from two point P1 = (x1,y1) and  P2 = (x2,y2)

c = 1; 
A = [P1(1) P1(2); P2(1) P2(2)]; 
B = [-1;-1]; 
D = inv(A)*B;
a = D(1,1);
b = D(2,1);
end


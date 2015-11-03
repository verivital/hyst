function  Plot_Safety_Specification_Polygon(A,B,range,color)
% This function plots the safety specification of the original system, the
% transformed safety specification

% Inputs
% 1) A, B is coefficient matrices of the safety specification
% 2) range is the range for ploting : range = [x_min x_max;y_min y_max]
% 3) color is the color that we want to plot

[mA,nA] = size(A);

for i = 1:mA
fh = @(y_i,y_j) A(i,1)*y_i + A(i,2)*y_j + B(i);
h= ezplot(fh,[range(1,1),range(1,2),range(2,1),range(2,2)]);
set(h,'color',color);
hold on;
end

end
function  Plot_Safety_Specification_Ellipse(a,b,centre,range,color)
% This function plots the elliptical safety specification of the original
% system, a*y1^2 + b*y2^2 = 1

% inputs
% 1) a,b is positive constants
% 2) centre = [x0;y0] is the centre of the ellipse
% 3) range = [xmin xmax; ymin ymax] , the range of the plot

fh = @(y_i,y_j) a*(y_i-centre(1,1))^2 + b*(y_j-centre(2,1))^2 - 1;
h = ezplot(fh,[range(1,1),range(1,2),range(2,1),range(2,2)]);
set(h,'color',color);
end

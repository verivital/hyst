function  Plot_Safety_Specification_Circle(centre,R,range,color)
% This function plots the circular safety specification of the original system, 
% inputs
% 1) centre = [x0;y0] is the centre of the circle
% 2) R is the radius of the circle
% 3) range = [xmin xmax; ymin ymax] , the range of the plot

fh = @(y_i,y_j) (y_i-centre(1,1))^2 + (y_j-centre(2,1))^2 - R^2;
%fh = @(y_i,y_j) (y_i-centre(1,1))^2 + (y_j-centre(2,1))^2 - R^2;
h = ezplot(fh,[range(1,1),range(1,2),range(2,1),range(2,2)]);
set(h,'color',color);
end
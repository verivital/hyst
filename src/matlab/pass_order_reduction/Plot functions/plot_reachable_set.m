
%% this script is used to plot the reachable set of original model and reduced model for ISS Model
% It will get the data from file.gen in SpaceEx and then plot the data

% Author : Tran Hoang Dung
% Date : 21-5-2015
% Paper : Verification for large scalse system using order reduction method


% plot the reachable set of 10-order output abstraction of iss 

% plot reachable set of y1-y2
fig = figure ;  plot_2d_vertices 'issr-10-y1-y2.gen' 'b';
hold on;

A_SP = [1 1; 2 -1; -1 -2;-1 1];  % safety specification of the full order system
B_SP = [-0.0008;-0.0015; -0.001; -0.00075];
Plot_Safety_Specification_Polygon(A_SP,B_SP,[-0.0012 0.0012; -0.0012 0.0012],'g'); % plot the safety specification of the full order system
[P_Y1_Y2] = Get_Points_From_Lines(A_SP,B_SP);
delta = [e_10(1,2);e_10(2,2)];
[A1,B1,A2,B2,offset] = Safety_Transformation_Polygon( A_SP,B_SP,delta); % get the safety specification tranformation

[P1_Yr1_Yr2] = Get_Points_From_Lines(A1,B1);  % get point of the polygon
[P2_Yr1_Yr2] = Get_Points_From_Lines(A2,B2);

Plot_point_to_point(P_Y1_Y2,'k'); % plot the safety specification polygons
Plot_point_to_point(P1_Yr1_Yr2,'b');
Plot_point_to_point(P2_Yr1_Yr2,'r');

title('Reachable set y_{r_1} - y_{r_2}');
xlabel('y_{r_1}'); 
ylabel('y_{r_2}'); 
savefig('issr-10-yr1-yr2');
print(fig,'issr-10-yr1-yr2','-dpdf');

% plot reachable set of y2-y3 

centre = [0;0]; % the centre of the circle 
Rc = 0.00035;
plot_range = [-0.0006 0.0006; -0.0006 0.0006];

Delta_R = sqrt(e_10(2,2)^2 + e_10(3,2)^2); % calculate Delta_R
Rc1 = Rc-Delta_R;
Rc2 = Rc + Delta_R; 

fig = figure;
Plot_Safety_Specification_Circle(centre,Rc,plot_range,'k'); % plot the safety specification of the full order model
hold on;
Plot_Safety_Specification_Circle(centre,Rc1,plot_range,'b'); % plot the safety specification of 10-order output abstraction
hold on;
Plot_Safety_Specification_Circle(centre,Rc2,plot_range,'r'); % plot the unsafe specification of 10-order output abstraction
hold on; 
plot_2d_vertices 'issr-10-y2-y3.gen' 'b';

title('Reachable set y_{r_2} - y_{r_3}');
xlabel('y_{r_2}'); 
ylabel('y_{r_3}'); 
savefig('issr-10-yr2-yr3');
print(fig,'issr-10-yr2-yr3','-dpdf');

% plot reachable set of y1-y3 

centre = [-0.0003;0]; % the centre of the ellipse 
a = 4*10^7;
b = 8*10^7;
plot_range = [-0.0008 0.00035; -0.0005 0.0005];

Delta_E = sqrt(a*e_10(1,2)^2 + b*e_10(3,2)^2); % calculate Delta_E

a1 = a/(1-Delta_E)^2;
b1 = b/(1-Delta_E)^2;

a2 = a/(1+Delta_E)^2;
b2 = b/(1+Delta_E)^2;

fig = figure;
Plot_Safety_Specification_Ellipse(a,b,centre,plot_range,'k'); % plot the safety specification of the full order model
hold on;
Plot_Safety_Specification_Ellipse(a1,b1,centre,plot_range,'b'); % plot the safety specification of 10-order output abstraction
hold on;
Plot_Safety_Specification_Ellipse(a2,b2,centre,plot_range,'r'); % plot the unsafe specification of 10-order output abstraction
hold on; 
plot_2d_vertices 'issr-10-y1-y3.gen' 'b';

title('Reachable set y_{r_1} - y_{r_3}');
xlabel('y_{r_1}'); 
ylabel('y_{r_3}'); 
savefig('issr-10-yr1-yr3');
print(fig,'issr-10-yr1-yr3','-dpdf');


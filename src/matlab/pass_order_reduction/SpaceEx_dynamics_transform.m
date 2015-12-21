function [ x_dot, y_dot ] = SpaceEx_dynamics_transform(sys)
% This function is used to calculate the dynamics of a linear system
% author: Hoang-Dung Tran

[mA,nA] = size(sys.a);
[mB,nB] = size(sys.b); 

x = sym('x', [mA 1]); % create state variable 
u = sym('u',[nB 1]); % create control input variable
x_dot = sys.a*x + sys.b*u; % calculate x_dot 
digits(5); % the number of significant digits is 5
x_dot = vpa(x_dot); % convert the coefficients to numeric constants
y_dot = sys.c*x;  % *** this because we need to declare y as the invariant of the system in SpaceEx 
y_dot = vpa(y_dot);
end


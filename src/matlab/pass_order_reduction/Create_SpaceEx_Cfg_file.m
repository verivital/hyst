function Create_SpaceEx_Cfg_file(file_name,x0_lb,x0_ub,y0_lb,y0_ub,u,stoptime)
% This function creates the configuration file for SpaceEx format
% author : Hoang-Dung Tran
[mX,nX] = size(x0_lb);
[mY,nY] = size(y0_lb); 
[mU,nU] = size(u);
x_str = '';
for i=1:mX
x_spec = 'x%d >= %9.7f & x%d <= %9.7f & ';
x_str = [x_str sprintf(x_spec,i,x0_lb(i),i,x0_ub(i))]; % initial condition of state variables
end

y_str = '';
for i=1:mY
y_spec = 'y%d >= %9.7f & y%d <= %9.7f & ';
y_str = [y_str sprintf(y_spec,i,y0_lb(i),i,y0_ub(i))]; % initial condition of state variables
end

u_str = '';
for i=1:mU
u_spec = 'u%d == %2.1f &';
u_str = [u_str sprintf(u_spec,i,u(i))]; % initial condition of state variables
end

t_str = sprintf('t==0 & stoptime == %4.2f"\n',stoptime); % initial condition of time variable
init_str = ['initially = " ' x_str y_str u_str t_str];

% create configuration file
file_name = [file_name '.cfg'];
fid = fopen(file_name,'w');
fprintf(fid,'# analysis option \n');
fprintf(fid,'system = "sys"\n');
fprintf(fid,init_str);
fprintf(fid,'scenario = "supp"\n');
fprintf(fid,'directions = "box"\n');
fprintf(fid,'sampling-time = 0.001\n');
fprintf(fid,'time-horizon = %4.2f\n',stoptime);
fprintf(fid,'iter-max = 10\n');
fprintf(fid,'output-variables = "t,y"\n');
fprintf(fid,'output-format = "GEN"\n');
fprintf(fid,'rel-err = 1.0e-8\n');
fprintf(fid,'abs-err = 1.0e-12\n');

end


function Create_SpaceEx_xml_file(file_name,sys)
%This function is used to generate automatically the SpaceEx xml file for % Linear system
%The SpaceEx format is then used for reachability analysis
% author : Hoang-Dung

file_name = [file_name '.xml'];
fid = fopen(file_name,'w');
fprintf(fid,'<?xml version="1.0" encoding="iso-8859-1"?>\n');
fprintf(fid,'<sspaceex xmlns="http://www-verimag.imag.fr/xml-namespaces/sspaceex" version="0.2" math="SpaceEx">\n');
fprintf(fid,'  <component id="core_component">\n');

[mA,nA] = size(sys.a);
[mB,nB] = size(sys.b);
[mC,nC] = size(sys.c);

% declare x 
x_paras = cell(mA,1); 
x_names = cell(mA,1);
types = sprintf('type="real" ');
locals = sprintf('local="false" ');
d1s = sprintf('d1="1" ');
d2s = sprintf('d2="1" ');
dynamics = sprintf('dynamics="any" ');

for i=1:mA
    x_names{i} = sprintf('name="x%d" ',i); 
    x_paras{i} = ['    <param ' x_names{i} types locals d1s d2s dynamics '/>\n'];
end
% declare y 

y_paras = cell(mC,1); 
y_names   = cell(mC,1);

for i = 1:mC
    y_names{i} = sprintf('name="y%d" ',i); 
    y_paras{i} = ['    <param ' y_names{i} types locals d1s d2s dynamics '/>\n'];
end 

% declare t
t_name = sprintf('name="t" ');
t_para = ['    <param ' t_name types locals d1s d2s dynamics '/>\n'];

% declare u 

u_paras = cell(nB,1);
u_names  = cell(nB,1); 
u_dynamic = sprintf('dynamics="const" ');

for i = 1:nB
    u_names{i} = sprintf('name="u%d" ',i); 
    u_paras{i} = ['    <param ' u_names{i} types locals d1s d2s u_dynamic '/>\n'];
end 

% declare stoptime 
stoptime_name = sprintf('name="stoptime" ');
stoptime_dynamic = sprintf('dynamics="const" ');
stoptime_para = ['    <param ' stoptime_name types locals d1s d2s stoptime_dynamic '/>\n'];

% prinf parameter 

for i = 1:mA 
    fprintf(fid,x_paras{i});
end

for i = 1:mC
    fprintf(fid,y_paras{i});
end 

fprintf(fid,t_para);

for i = 1:nB
     fprintf(fid,u_paras{i});
end

fprintf(fid,stoptime_para);
fprintf(fid,'    <location id="1" name="Model" x="362.0" y="430.0" width="426.0" height="610.0">\n');


% print invariant

[x_dot,y_invariant] = SpaceEx_dynamics_transform(sys);
x_dots = cell(mA,1);
y_invars = cell(mC,1); 

fprintf(fid,'     <invariant>t &lt;= stoptime \n &amp;');
for i = 1:mC-1
    y_invars{i} = char(y_invariant(i));
    y_invars_tempt = sprintf('y%d == ',i);
    y_invars{i} = [y_invars_tempt y_invars{i} '\n &amp;'];
    fprintf(fid,y_invars{i});
end 
    y_invars{mC} = char(y_invariant(mC)); % prinf final line of invariant
    y_invars_tempt = sprintf('y%d == ',mC); 
    y_invars{mC} = [y_invars_tempt y_invars{mC} '</invariant>\n'];
fprintf(fid,y_invars{mC});

% prinf flows 

fprintf(fid, '     <flow> ');

for i = 1:mA
    x_dots{i} = char(x_dot(i));
    x_dots_tempt = sprintf('x%d'' == ',i);
    x_dots{i} = [x_dots_tempt x_dots{i} '\n &amp;'];
    fprintf(fid,x_dots{i});
end

fprintf(fid, 't'' == 1</flow>\n'); 

% print location
fprintf(fid, '     </location>\n');
fprintf(fid,'   </component>');
fprintf(fid, '     <component id="sys">\n');

for i = 1:mA 
     x_paras{i} = ['    <param ' x_names{i} types locals d1s d2s dynamics 'controlled="true" />\n'];
     fprintf(fid,x_paras{i});
end

t_para = ['    <param ' t_name types locals d1s d2s dynamics 'controlled="true" />\n'];
stoptime_para = ['    <param ' stoptime_name types locals d1s d2s stoptime_dynamic 'controlled="true" />\n'];
fprintf(fid,t_para);
fprintf(fid,stoptime_para);

for i = 1:mC 
     y_paras{i} = ['    <param ' y_names{i} types locals d1s d2s dynamics 'controlled="true" />\n'];
     fprintf(fid,y_paras{i});
end

for i = 1:nB 
     u_paras{i} = ['    <param ' u_names{i} types locals d1s d2s u_dynamic 'controlled="true" />\n'];
     fprintf(fid,u_paras{i});
end

% print bind 

fprintf(fid,'     <bind component="core_component" as="model">\n');

map_x = cell(mA,1);
for i = 1:mA
    map_x{i} = sprintf('       <map key="x%d">x%d</map>\n',i,i);
    fprintf(fid,map_x{i});
end
map_t = sprintf('       <map key="t">t</map>\n');
map_stoptime = sprintf('       <map key="stoptime">stoptime</map>\n');

fprintf(fid,map_t);
fprintf(fid,map_stoptime); 

map_y = cell(mC,1);
for i = 1:mC
    map_y{i} = sprintf('       <map key="y%d">y%d</map>\n',i,i);
    fprintf(fid,map_y{i});
end

map_u = cell(nB,1);
for i = 1:nB
    map_u{i} = sprintf('       <map key="u%d">u%d</map>\n',i,i);
    fprintf(fid,map_u{i});
end

fprintf(fid,'    </bind>\n');
fprintf(fid,'  </component>\n');
fprintf(fid,'</sspaceex>');

end


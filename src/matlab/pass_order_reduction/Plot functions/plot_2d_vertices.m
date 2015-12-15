function varargout=plot_2d_vertices(fname,varargin)
% plot_2d_vertices    Plot a sequence of polygons defined by their
%                     vertices.
%
% plot_2d_vertices(fname)
% Plots the contents of the file "fname", which must be a two-column
% list of vertices separated by empty lines:
%    x11 y11
%    x12 y12
%    ...
%    x1n1 y1n1
%
%    x21 y21
%    x22 y22
%    ...
%    x2n2 y2n2
%
%    ...
%
% Each sequence of vertices defines a polygon. When an empty line is 
% encountered, a new polygon is started.
% 
% plot_2d_vertices(fname,...)
% Passes "..." as options to the patch command that draws the polygons.
% E.g., to plot in red color:
%    plot_2d_vertices(fname,'r')
%
% H=plot_2d_vertices(...)
% Returns a vector of handles to the patch objects.
% Allows the manipulation of the patches, e.g., to remove the outline:
%    set(H,'LineStyle','none')
%

if nargin==0
    error('Filename not specified.');
end
if nargin>1 
    color=varargin{1:end};
else
    color='b';
end

fid=fopen(fname,'rt');
if fid~=-1
    X=[];
    Y=[];
    H=[];
    while ~feof(fid)
        tline = fgetl(fid);
        if (~strcmp(tline,'') & ~feof(fid))
            d = sscanf(tline,'%g');
            X=[X;d(1)];
            Y=[Y;d(2)];
        else
            %h=patch(X,Y,color);
            %h=patch(X,Y,color,'EdgeColor','none');
            h=patch(X,Y,color,'EdgeColor',color,'LineWidth',4);
            X=[];
            Y=[];
            H=[H;h];
        end 
    end
    fclose(fid);
    if nargout>0
        varargout(1)={H};
    end
else
    error('Error: Could not open file.')
end

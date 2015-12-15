function Plot_point_to_point(P,color)
% This function is used to plot the safety specification with polygonal
% shape

[mP,nP] = size(P); 
for i = 1:mP-1
    plot([P(i,1),P(i+1,1)],[P(i,2),P(i+1,2)],'color',color);
    hold on;
end
    plot([P(mP,1),P(1,1)],[P(mP,2),P(1,2)],'color',color);
    hold on;
end


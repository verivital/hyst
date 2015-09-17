function plotSimulationLoop(time, values, labels, plot_option, plot_phase, p, colors, i)

    % plot trajectories
        color = colors(1 + mod(i, length(colors)));
        if plot_option == plot_phase
            vec_il = values(2, :);
            vec_vc = values(3, :);
            hold on;
            %marker = '.';
            marker = 'o';
            marker_size = 15;
            scatter(vec_il,vec_vc,marker_size,marker,'filled', color);
            %scatter(vec_il,vec_vc,marker, color);
            %scatter(vec_il,vec_vc, color, 'filled');
        elseif (plot_option)   
            for j = 1 : size(labels, 2);
                marker = 'o';
                marker_size = 10;
                scatter(p(j), time', values(j, :)', marker_size,marker,'filled', color);
                scatter(p(j), time', values(j, :)', color);
            end
        else
            % plot all variables in one diagram
            plot(time', values', color);
            %scatter(time', values', color);
            %xlabel('time');
        end
        
% TODO: add this back as a plot option mode..., was plotOneVariable.m
%if opt_plot
%% plot location vs time
%        color = colors(1 + mod(i, length(colors)));
%        if (plot_option)           
%            % plot each variable in a single diagram
%            %hold on;
%            marker = 'x';
%            marker_size = 10;
%            %scatter(time', values(1, :)', marker_size,marker,'filled', color);          
%            %scatter(time', values(2, :)', color,'o');
%            %scatter( time', values(1, :)', color); 
%            %h.Marker = '+';
%            plot(time', values(2, :)', color);
%            xlabel('time');
%            ylabel(labels{2});
%        end
%        %plot_2d_vertices([labels{2},'.txt'], 'r');
%end

% TODO: add this back as an option, was plotLocation.m
%if opt_plot
% plot location vs time
%        color = colors(1 + mod(i, length(colors)));
%        if (plot_option)           
%            % plot each variable in a single diagram
%            %hold on;
%            %marker = 'x';
%            %marker_size = 10;
%            %scatter(time', values(1, :)', marker_size,marker,'filled', color);          
%            scatter(time', values(1, :)', color,'o');
%            %scatter( time', values(1, :)', color); 
%            %h.Marker = '+';
%            xlabel('time');
%            ylabel(labels{1});
%        end
%end
end
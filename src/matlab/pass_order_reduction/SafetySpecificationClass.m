classdef SafetySpecificationClass
    % This Class is used to build the Safety Specification data structure
    %   The Safety Specification may be a polygon or an ellipsoid 
    %   For polygonal Safety Specification, it will be defined by two matrix A and B that indicate Ax + B <= 0
    %   For ellipsoid Safety Specification, it will defined by the matrix P
    %   and the radius R that indicate : x'Px <= R^2 
    
    % author: Hoang-Dung Tran
    
    properties
        name; % safe or unsafe 
        type; % polygon or ellipsoid
        matrix_A ; % matrix A for polygonal SP
        matrix_B ; % matrix B for polygonal SP
        matrix_P ; % matrix P for ellipsoid SP
        radius   ; % radius R for ellipsoid SP
    end
    
    methods
              
    end
    
end


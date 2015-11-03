% This is the main function for safety specification transformation
% Inputs :
%    1) Safety specification object of the full-order system
%    2) err_bound : the vector error bound between the full-order system
%    and the output abstraction
% Outputs: 
%    1) SP : the safety specfication for the output abstraction 
%    2) UP : the unsafe specification for the output abstraction

% author : Hoang-Dung Tran 

function [SP,UP] = safety_transformation(obj,err_bound) 
       obj_temp = obj;
       err_temp = err_bound;
       SP = [];
       UP = [];
       % check type of safety specification : polygon or ellipsoid 
       if (strcmp(obj.type, 'polygon'))||(strcmp(obj.type, 'ellipsoid')) 
           OK_Flag1 = 1;
       else
           error('undefine the type of safety specification');
           OK_Flag1 = 0;
       end 
       
       % check name of safety specification : safe or unsafe     
       if (strcmp(obj.name, 'safe'))||(strcmp(obj.name, 'unsafe')) 
           OK_Flag2 = 1;
       else     
           error('undefine the name of safety specification');  
           OK_Flag2 = 0;
           
       end 
       
       %  Transformation for polygonal safety specification
       if (strcmp(obj.type, 'polygon'))&& (OK_Flag1) && (OK_Flag2)
           [SP,UP] = safe_transform_polygon(obj_temp,err_temp);
       end
       
       % Transformation for ellipsoid safety specification  
       if (strcmp(obj.type, 'ellipsoid'))&& (OK_Flag1 && OK_Flag2)
           [SP,UP] = safe_transform_ellipsoid(obj,err_bound);
       end
       
       end 
       
       %These are local functions that is used for safety specification transformation
       
       % this function is based on the Lemma 2 in the paper for HSCC2015
       function [SP,UP] = safe_transform_polygon(obj,err_bound)
       
            A = obj.matrix_A; % get matrix A for transformation
            B = obj.matrix_B; % get matrix B for transformation
            [mA,nA] = size(A);
            [mB,nB] = size(B); 
            [me,ne] = size(err_bound); 
            Delta = zeros(mA,1); % create the vector Delta
            B_safe = zeros(mB,1); % vector for new safety specification   SP
            B_unsafe = zeros(mB,1); % vector for new unsafe specification UP
            
            if (isempty(A)) % check empty matrix A
                error('invalid matrix. Matrix A is empty')
            elseif (isempty(B)) % check if matrix B is empty or has more than one column
                error('invalid matrix. Matrix B is empty');
            elseif (isempty(err_bound)) || (ne > 1) % check the error bound vector is empty or contain more than 1 column
                error('invalid error bound')
            elseif (nA~=me)||(mA~=mB)
                error('Matrix A, B and error bound vector are not consistent')
            else 
                for i = 1:mA
                    for j = 1:nA 
                      Delta(i) = Delta(i)+abs(A(i,j))*err_bound(j);  
                    end
                end
                
                for i = 1:mB                  
                    B_safe(i) = B(i)+Delta(i); % calculate the new matrix B_safe and B_unsafe
                    B_unsafe(i) = B(i) - Delta(i); 
                end
            end
            
            % return the transform safety and unsafe specification for the
            % abstraction
            if (strcmp(obj.name, 'safe'))
               obj.name = 'safe';
               obj.matrix_B = B_safe;
               SP = obj; 
               obj.name = 'unsafe';
               obj.matrix_B = B_unsafe; 
               UP = obj; 
            elseif (strcmp(obj.name, 'unsafe')) 
               obj.name = 'safe';
               obj.matrix_B = [];
               SP = obj; 
               obj.name = 'unsafe';
               obj.matrix_B = B_unsafe; 
               UP = obj; 
            end
  
       end
        
   % this function is based on the Lemma 3 in the paper for HSCC2015
       function [SP,UP] = safe_transform_ellipsoid(obj,err_bound)
    
            P = obj.matrix_P; % get matrix P for transformation
            [G,p] = chol(P); % check P is positive or not
            [U,S,V] = svd(P); % singular value decoposition for matrix P , note that P is symetric => U^T = V
            
            R = obj.radius; % get the radius R of the safety specification
            
            [mP,nP] = size(P);
            [me,ne] = size(err_bound);  
            Delta = zeros(mP,1);
            Delta_R = 0; % 

            
            if (~issymmetric(P)) % check empty matrix A
                error('invalid matrix. Matrix P is not symmetric ')
            elseif (p>0) 
                error('invalid matrix. Matrix P is not positive');
            elseif (isempty(err_bound)) || (ne > 1) % check the error bound vector is empty or contain more than 1 column
                error('invalid error bound')
            elseif (nP~=me)
                error('Matrix P and error bound vector are not consistent')
            else 
                for i = 1:mP
                    for j = 1:nP 
                      Delta(i) = Delta(i)+abs(U(i,j))*err_bound(j);  
                    end
                end
                
                for i = 1:mP                    
                    Delta_R = Delta_R + S(i,i)*Delta(i)^2;
                end
                
                    Delta_R = sqrt(Delta_R); % get the final value of Delta_R
                    
            end
            
            % return the transform safety and unsafe specification for the
            % abstraction
            if (strcmp(obj.name, 'safe'))
               obj.name = 'safe';
               obj.radius = R - Delta_R;
               SP = obj; 
               obj.name = 'unsafe';
               obj.radius = R + Delta_R; 
               UP = obj; 
            elseif (strcmp(obj.name, 'unsafe')) 
               obj.name = 'safe';
               obj.matrix_P = [];
               obj.radius = [];
               SP = obj; 
               obj.name = 'unsafe';
               obj.radius = R + Delta_R; 
               UP = obj;
            end
  
       end     
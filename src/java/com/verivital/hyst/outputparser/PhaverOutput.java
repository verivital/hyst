/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.verivital.hyst.outputparser;

import com.verivital.hyst.ir.AutomatonExportException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 *
 * @author Luan Nguyen
 */
public class PhaverOutput {
    
    /**
     * Parse reachable sets computed by Phaver into type <location, dynamics constraint>
     * @param path
     * @return reach
     */
    public LinkedHashMap <String, String> Reachset(String path) throws IOException, FileNotFoundException{
        
        LinkedHashMap <String, String> reach = new LinkedHashMap <String, String>();
        //ArrayList <String> reach = new ArrayList <String>();
        
        
        if (!new File(path).exists()) 
        throw new AutomatonExportException( path + " is not found.");
        
        File file = new File(path);
        StringBuilder fileContents = new StringBuilder((int)file.length());
        Scanner scanner = new Scanner(file);
        String lineSeparator = System.getProperty("line.separator");              
        try {
            while(scanner.hasNextLine()) {        
                fileContents.append(scanner.nextLine() + lineSeparator);
            }
            String reachset = fileContents.toString();
            
            int left = reachset.indexOf("{");
            int right = reachset.indexOf("}");

            // pull out the text inside the parens
            reachset = reachset.substring(left+1, right); 
            
            boolean first = true;
            String[] tokens = reachset.split("\\|");
            for(String s:tokens){
            //System.out.println(s);
                left = s.indexOf("(");
                right = s.lastIndexOf(")");
                s = s.substring(left+1, right+1);
                if (s.contains("loc")){
                    right = s.indexOf("&");
                    String key = s.substring(0, right);
                    String value = s.substring( right+2, s.length()-1);
                    if (first){
                        reach.put(key, value);
                        first = false;
                    }
                    else{
                        if (reach.keySet().contains(key)){
                            value = value + " | " + reach.get(key);
                            reach.put(key,value);
                        }
                        else{
                            reach.put(key, value);
                        }                       
                    }
                }
                else{
                    reach.put("true",s);
                }
        }  
        return reach;
        } finally {
            scanner.close();
        }   
    } 
    
    /**
     * Print reachable sets as a single string
     * @param reach
     * @return
     */
    public String toString(LinkedHashMap <String, String> reach){
        String rv = "reg = {\n";
        for (Entry<String, String> e : reach.entrySet())
            rv = rv + e.getKey() + " &\n" + e.getValue() + ",\n";  
        return rv + "}";
            
    }
    
    public static void main(String[] args) throws IOException, FileNotFoundException {
        PhaverOutput p = new PhaverOutput();
        
        System.out.println("Enter the path of an original phaver output : ");
        Scanner scanner = new Scanner(System.in);
        String path = scanner.nextLine();        
        //LinkedHashMap <String, String> reach = p.Reachset("C:/Users/Luan Nguyen/Downloads/heater_phaver_output.txt");
        LinkedHashMap <String, String> reach = p.Reachset(path);
        String reachset = p.toString(reach);
        System.out.println(reachset);
        // write parsed reachable sets to a file
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                  new FileOutputStream("phaver_out.txt")));
            writer.write(reachset);
        } catch (IOException ex) {
        } finally {
           try {writer.close();} catch (Exception ex) {}
        }
    }
}

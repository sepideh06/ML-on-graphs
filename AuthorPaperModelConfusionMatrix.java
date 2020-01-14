package example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bouncycastle.util.Arrays.Iterator;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.procedure.*;

import example.javaModelTrain.Output;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ml.dmlc.xgboost4j.java.DMatrix;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Values;
import static org.neo4j.driver.v1.Values.parameters;



public class AuthorPaperModelConfusionMatrix {
	
	
	
	public void createModelBasedOnFile(Boolean CommonNeighbours,Boolean ShortestPathInCoAuthorGraph,Boolean SecondDegreeShortestPathInCoAuthorGraph,
			Boolean ShortestPathInCitationGraph,Boolean SecondDegreeShortestPathInCitationGraph) throws XGBoostError, IOException 
	{
		
		//ReadTrainTestDataFromFile
		
		String csvFile = "C:\\Users\\IS PC\\Desktop\\neo4j-procedure-template-3.5\\FeatureCalculation\\Train.csv";
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        
        String label = "";
        String[] arrTest = new String[1000];
		String[] arrTrain = new String[1000];
		
		int numberOfParameters = 0;
		if(CommonNeighbours)
		{numberOfParameters = numberOfParameters + 1;}	
		if(ShortestPathInCoAuthorGraph)
		{numberOfParameters = numberOfParameters + 1;}
		if(SecondDegreeShortestPathInCoAuthorGraph)
		{numberOfParameters = numberOfParameters + 1;}
		if(ShortestPathInCitationGraph)
		{numberOfParameters = numberOfParameters + 1;}
		if(SecondDegreeShortestPathInCitationGraph)
		{numberOfParameters = numberOfParameters + 1;}
	
		 
		 

        try {

            br = new BufferedReader(new FileReader(csvFile));
            int iteration = 0;
            int h = 0;
            
             
			 float[] dataTrain = new float[(numberOfParameters+3)*(1000)];		 
			 float[] labelsTrain = new float[1000];
			 
            while ((line = br.readLine()) != null) {
            			
                  int i =0;	
                // use comma as separator
            	if(iteration == 0) {
                    iteration++;  
                    continue;
                }
            	else
            	{
	                String[] arrayTrain = line.split(cvsSplitBy);
	   	    		 
	   	         	//Fill final dataTest array for passing to ModelCreationFunction
	   	 		         if(arrayTrain[7].replace(" ", "").contains("nonexist"))
	   	 		       	 {
	   	 		       	     label = "0";
	   	 		       	     labelsTrain[i] = 0;
	   	 		       	 }
	   	 		       	 else
	   	 		       	 {
	   	 		       		 label = "1";
	   	 		       		 labelsTrain[i] = 1;
	   	 		       	 }
	   	 		         dataTrain[h] = Float.parseFloat(label);
	   	 		         h=h+1;
	   	 		        // dataTrain[h] = Float.parseFloat(arrayTrain[0]);
	   		             //h = h + 1;
	   		        	// dataTrain[h] = Float.parseFloat(arrayTrain[1]);
	   		        	// h = h + 1;
	   	        	 //Feature Calculation
	   	        	 
	   	        	if(CommonNeighbours)
	   	        	{   	        		
	   		        	 dataTrain[h] = Float.parseFloat(arrayTrain[2]);
	   		        	 h = h + 1;
	   	        	}
	   	        	
	   	        	if(ShortestPathInCoAuthorGraph)
	   	        	{	   	        		
	   	        	     dataTrain[h] = Float.parseFloat(arrayTrain[3]);
	   	        		 h = h + 1;
	   	        	}
	   	        	
	   	        	if(SecondDegreeShortestPathInCoAuthorGraph)
	   	        	{
	   	        		dataTrain[h] = Float.parseFloat(arrayTrain[4]);
	   	        		 h = h + 1;
	   	        	}
	   	        	
	   	        	if(ShortestPathInCitationGraph)
	   	        	{
	   	        		dataTrain[h] = Float.parseFloat(arrayTrain[5]);
	   	        		 h = h + 1;
	   	        	}
	   	        	
	   	        	if(SecondDegreeShortestPathInCitationGraph)
	   	        	{
	   	        		dataTrain[h] = Float.parseFloat(arrayTrain[6]);
	   	        		 h = h + 1;
	   	        	}
	   	        	        	 
	   	            //End of Calculation For Train Data
            	}

            }
            
            
            csvFile = "C:\\Users\\IS PC\\Desktop\\neo4j-procedure-template-3.5\\FeatureCalculation\\Test.csv";
            br = null;
            line = "";
            
            br = new BufferedReader(new FileReader(csvFile));
            iteration = 0;
            h = 0;
            
            float[] dataTest = new float[(numberOfParameters+3)*(1000)];
            float[] labelsTest = new float[1000];
            
            while ((line = br.readLine()) != null) {
    			
                int i = 0;	
              // use comma as separator
          	if(iteration == 0) {
                  iteration++;  
                  continue;
              }
          	else
          	{
	                String[] arrayTest = line.split(cvsSplitBy);
	   	    		 
	   	         	//Fill final dataTest array for passing to ModelCreationFunction
	   	 		         if(arrayTest[7].replace(" ", "").contains("nonexist"))
	   	 		       	 {
	   	 		       	     label = "0";
	   	 		       	     labelsTest[i] = 0;
	   	 		       	 }
	   	 		       	 else
	   	 		       	 {
	   	 		       		 label = "1";
	   	 		       		 labelsTest[i] = 1;
	   	 		       	 }
	   	 		         dataTest[h] = Float.parseFloat(label);
	   	 		         h=h+1;
	   	 		         //dataTest[h] = Float.parseFloat(arrayTest[0]);
	   		        	 //h = h + 1;
	   		        	// dataTest[h] = Float.parseFloat(arrayTest[1]);
	   		        	// h = h + 1;
	   	        	 //Feature Calculation
	   	        	 
	   	        	if(CommonNeighbours)
	   	        	{   	        		
	   		        	 dataTest[h] = Float.parseFloat(arrayTest[2]);
	   		        	 h = h + 1;
	   	        	}
	   	        	
	   	        	if(ShortestPathInCoAuthorGraph)
	   	        	{	   	        		
	   	        	     dataTest[h] = Float.parseFloat(arrayTest[3]);
	   	        		 h = h + 1;
	   	        	}
	   	        	
	   	        	if(SecondDegreeShortestPathInCoAuthorGraph)
	   	        	{
	   	        		dataTest[h] = Float.parseFloat(arrayTest[4]);
	   	        		 h = h + 1;
	   	        	}
	   	        	
	   	        	if(ShortestPathInCitationGraph)
	   	        	{
	   	        		dataTest[h] = Float.parseFloat(arrayTest[5]);
	   	        		 h = h + 1;
	   	        	}
	   	        	
	   	        	if(SecondDegreeShortestPathInCitationGraph)
	   	        	{
	   	        		dataTest[h] = Float.parseFloat(arrayTest[6]);
	   	        		 h = h + 1;
	   	        	}
	   	        	        	 
	   	            //End of Calculation For Test Data
          	}
          

          }
            
        	
            //Model Creation
  	         int numColumn = numberOfParameters + 1;
  	         //DMatrix testMat = new DMatrix(rowHeadersTest, colIndexTest, dataTest, DMatrix.SparseType.CSR, numColumn);
  	         //DMatrix trainMat = new DMatrix(rowHeadersTrain, colIndexTrain, dataTrain, DMatrix.SparseType.CSR, numColumn);
  	         DMatrix testMat = new DMatrix(dataTest,1000, numColumn);
  	         DMatrix trainMat = new DMatrix(dataTrain,1000 , numColumn);
  	         testMat.setLabel(labelsTest);
  	         trainMat.setLabel(labelsTrain);
  	      
  	 		Map<String, Object> params = new HashMap<String, Object>() {
  	 			  {
  	 			    put("eta", 0.01);
  	 			    put("max_depth", 12);
  	 			    //put("silent", 1);
  	 			    put("objective" , "multi:softprob"); 
  	 			    //put("objective", "binary:logistic");
  	 			    //put("eval_metric", "logloss");
  	 			    put("eval_metric", "mlogloss");
  	 			    //put("eval_metric", "merror");
  	 			    put("num_class", 2);
  	 			  }
  	 			};
  	 			
  	 			
  	 			Map<String, DMatrix> watches = new HashMap<String, DMatrix>() {
  	 				  {
  	 				    put("train", trainMat);
  	 				    put("test", testMat);
  	 				  }
  	 				};
  	 				int nround = 300;
  	 				Booster booster = XGBoost.train(trainMat, params, nround, watches, null, null);
  	 				booster.saveModel("model.bin");
  	 				//booster = XGBoost.loadModel("model.bin");
  	 				float[][] predicts = booster.predict(testMat);
  	 				System.out.println("predicts " + predicts[0][0] + " for class 1 and " + predicts[0][1] + " for class 0..." +  "The real label was:"+testMat.getLabel()[0]);
  	 				System.out.println("predicts " + predicts[1][0] + " for class 1 and " + predicts[1][1] + " for class 0..." + "The real label was:"+testMat.getLabel()[1]);
  	 				System.out.println("predicts " + predicts[800][0] + " for class 1 and " + predicts[800][1] + " for class 0..." +  "The real label was:"+testMat.getLabel()[800]);
  	 				
  	 				float[] statisticInfo = confusionMatrix(testMat,predicts);
  	 				System.out.println("accuracy:"+statisticInfo[0] + "...."+
  	 						"precision:"+statisticInfo[1]+"....."+
  	 						"errorRate:"+statisticInfo[2]+"...."+
  	 						"recall:"+statisticInfo[3]+"....."+
  	 						"FScore:"+statisticInfo[4]);
  	         
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }
	}     
 //-----------------------------------------------------------------------------------------------------------
        
public float[] confusionMatrix(DMatrix testMat,float[][] predicts) throws XGBoostError
{
	float[] statisticInfo= new float[5];
	float noNo = 0; float noYes = 0; float yesNo = 0; float yesYes = 0;
	float predictedLabel = 2;
	for(int i = 0;i < predicts.length;i++)
	{
		if(predicts[i][0] > predicts[i][1])
			predictedLabel = 1;
		else
			predictedLabel = 0;
	    if(testMat.getLabel()[i] == 1 && predictedLabel == 1)
	    	yesYes =+ 1;
	    else if(testMat.getLabel()[i] == 0 && predictedLabel == 0)
	    	noNo =+ 1;
	    else if(testMat.getLabel()[i] == 0 && predictedLabel == 1)
	    	noYes =+ 1;
	    else if(testMat.getLabel()[i] == 1 && predictedLabel == 0)
	    	yesNo =+ 1;		    
	}
	//(TP+TN)/total 
	float accuracy = (noNo + yesYes) /(noNo+yesYes+yesNo+noYes);
	// TP/predicted yes
	float precision = yesYes / (noYes + yesYes);
	float errorRate = 1-accuracy;
	// TP/actual yes 
	float recall = yesYes / (yesYes + yesNo);
	float FScore = 2*((precision*recall)/(precision+recall));
	
	statisticInfo[0] = accuracy;
	statisticInfo[1] = precision;
	statisticInfo[2] = errorRate;
	statisticInfo[3] = recall;
	statisticInfo[4] = FScore;
	return statisticInfo;
}
  //------------------------------------------------------------------------------------------------------------
		

}

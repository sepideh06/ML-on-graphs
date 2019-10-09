package example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.javatuples.Pair;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Values;
import org.neo4j.procedure.Name;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

public class OutDBSplit {
	
	Map<String, String> dicPageIDName = new HashMap<String, String>(); 
    

public void OutDataBaseSplitAndModelCreation(double cutoff2,String tag) throws XGBoostError, IOException {
	
		 float cutoff = (float)cutoff2;
		 //This should return the id of train node, the id of test node
		 Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
		 List<String> train = new ArrayList<>();
		 List<String> test = new ArrayList<>();
		 ArrayList<Integer> pageIDs = new ArrayList<>();
		 List<Pair<Integer, Integer>> linkedPageIDs = new ArrayList<Pair<Integer, Integer>>();
		 long startTime = 0;
		 long stopTime = 0;
		  
		 Integer skip=0;

			 Random random = new Random();
			 startTime = System.currentTimeMillis();
			 
			 try (Session session = driver.session()) {
				 Integer numEdges = Integer.parseInt(session.run("MATCH (a:page)-[r:LINKS_TO]->() RETURN COUNT(r)").next().get("COUNT(r)").toString());
				 //System.out.println("num" + " " + numEdges.toString());
				 while (skip<numEdges){
				 //StatementResult listOfIds = session.run("MATCH (a:page)-[r:LINKS_TO]->(b:page) RETURN distinct a.PageID,b.PageID SKIP {skip} LIMIT 1000; ", Values.parameters("skip",skip));
				 StatementResult listOfIds = session.run("MATCH (a:page)-[r:LINKS_TO]->(b:page) RETURN distinct a.PageID,b.PageID,a.Embedding,b.Embedding SKIP {skip} LIMIT 1000;", Values.parameters("skip",skip));
				 listOfIds.stream().forEach(cand->{
					 
					 
					 if(pageIDs.contains(cand.get("a.PageID"))== false)
						 pageIDs.add(Integer.parseInt(cand.get("a.PageID").toString().replace("\"","")));
					 
					 if(pageIDs.contains(cand.get("b.PageID"))== false)
						 pageIDs.add(Integer.parseInt(cand.get("b.PageID").toString().replace("\"","")));
						 
					 linkedPageIDs.add(new Pair<Integer, Integer>(Integer.parseInt(cand.get("a.PageID").toString().replace("\"","")), Integer.parseInt(cand.get("b.PageID").toString().replace("\"","")))); 
					 
					 String pairToInsert=cand.get("a.PageID").toString().replace("\"","");
					 
					 pairToInsert+=","+cand.get("b.PageID").toString().replace("\"","");
					 pairToInsert+=","+cand.get("a.Embedding").toString().replace("\"","");
					 pairToInsert+=","+cand.get("b.Embedding").toString().replace("\"","")+","+"1";
					 
					 
					 if (random.nextFloat()<=cutoff){
						 train.add(pairToInsert);
					 }
					 else{
						 test.add(pairToInsert);
					 }
					 });
				 //System.out.println(skip);
				 //System.out.println("Enter");
				  skip+=1000;
				 }
				 session.close();
			 }
			 driver.close();
			 
			 //Create Cartesian Product
			 List<Pair<Integer, Integer>> cartesianProductList = new ArrayList<Pair<Integer, Integer>>();
			 cartesianProductList = cartesian(pageIDs);
			 
			 
			//CommunityID & list of pages assigned to it
			 Map<String, List<String>> dicPageCommunity = new HashMap<String, List<String>>();
			 
			 dicPageCommunity = GetCommunityIds(pageIDs); 
			
			 //System.out.println("dicPageCommunity..."+dicPageCommunity);
			
			
			 //Non-Exist links
			 boolean sameCommunityflag=false;
			
			 List<Pair<Integer, Integer>> lstNonExistPagelinks = new ArrayList<Pair<Integer, Integer>>();
			 
			 for (Pair<Integer, Integer> pairId : cartesianProductList)
			{
				 //sameCommunityflag = ComparingPageCommunityId(dicPageCommunity, Integer.parseInt(pairId.getValue(0).toString().replace("\"","")), Integer.parseInt(pairId.getValue(1).toString().replace("\"","")));
			        if (pairId.getValue(0) != pairId.getValue(1) && linkedPageIDs.contains(pairId) == false && lstNonExistPagelinks.size() <= linkedPageIDs.size() /*&& sameCommunityflag == true*/)
			        	lstNonExistPagelinks.add(new Pair<Integer, Integer>(Integer.parseInt(pairId.getValue(0).toString().replace("\"","")), Integer.parseInt(pairId.getValue(1).toString().replace("\"",""))));
			}
			        
			 //Split non-exist links
					Integer trainSize = (int)(lstNonExistPagelinks.size() * cutoff);
					List<Pair<Integer, Integer>> trainNonExist = new ArrayList<Pair<Integer, Integer>>();
					List<Pair<Integer, Integer>> testNonExist = new ArrayList<Pair<Integer, Integer>>();
					testNonExist = lstNonExistPagelinks;
					while(trainNonExist.size() < trainSize) {
						Integer index = random.nextInt(testNonExist.size());
						trainNonExist.add(testNonExist.get(index));
						testNonExist.remove(testNonExist.get(index));
					}
														
			
					 List<Pair<Integer, String>> pageIDEmbeddingList = new ArrayList<Pair<Integer, String>>();
					 driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
					 skip = 0;
					 try (Session session = driver.session()) {
						 Integer numEdges = Integer.parseInt(session.run("MATCH (a:page)-[r:LINKS_TO]->() RETURN COUNT(r)").next().get("COUNT(r)").toString());
						 //System.out.println("num" + " " + numEdges.toString());
						 while (skip<numEdges){
						 //StatementResult listOfIds = session.run("MATCH (a:page)-[r:LINKS_TO]->(b:page) RETURN distinct a.PageID,b.PageID SKIP {skip} LIMIT 1000; ", Values.parameters("skip",skip));
						 StatementResult listOfEmbeddings = session.run("MATCH (n:page) RETURN n.Embedding ,n.PageID SKIP {skip} LIMIT 1000;", Values.parameters("skip",skip));
						 listOfEmbeddings.stream().forEach(cand->{
							 
							 pageIDEmbeddingList.add(new Pair<Integer, String>(Integer.parseInt(cand.get("n.PageID").toString().replace("\"","")), cand.get("n.Embedding").toString().replace("\"",""))); 
							 
						 });
						  skip+=1000;
						 }
						 session.close();
					 }
					 driver.close();
					 
					//Combine non-exist & exist into trainSet & test Set
					 for (Pair<Integer, Integer> pairId : trainNonExist)
						{
						  String Embedding1 = "";
						  String Embedding2 = "";
						 	  for(Pair<Integer,String> pair : pageIDEmbeddingList)
						 	  {
						 		 if (pair.getValue(0)== pairId.getValue(0))
						 		 {
				 			  		Embedding1= pair.getValue(1).toString();
						 		 }	
						 		 if (pair.getValue(0)== pairId.getValue(1))
						 		 {
				 			  		Embedding2= pair.getValue(1).toString();
						 		 }	
						 	  }
						 	  						 	  
					          train.add(pairId.getValue(0).toString()+","+pairId.getValue(1).toString()+","+Embedding1+"," + Embedding2+","+"0");
						}
	
					 for (Pair<Integer, Integer> pairId : testNonExist)
						{
						   String Embedding1 = "";
						   String Embedding2 = "";
						 	  for(Pair<Integer,String> pair : pageIDEmbeddingList)
						 	  {
						 		 if (pair.getValue(0)== pairId.getValue(0))
						 		 {
				 			  		Embedding1= pair.getValue(1).toString();
						 		 }	
						 		 if (pair.getValue(0)== pairId.getValue(1))
						 		 {
				 			  		Embedding2= pair.getValue(1).toString();
						 		 }	
						 	  }
						 	  
					          test.add(pairId.getValue(0).toString()+","+pairId.getValue(1).toString()+","+Embedding1+"," + Embedding2+","+"0");
						}
					 
					 stopTime = System.currentTimeMillis();
					 System.out.println("Train and Test DataSplit takes..." + (stopTime - startTime) + "ms");
					 
					 List<String> liness = test;
		             Path files = Paths.get("test0.txt");
		             Files.write(files, liness, StandardCharsets.UTF_8);
		             
		             liness = train;
		             files = Paths.get("train0.txt");
		             Files.write(files, liness, StandardCharsets.UTF_8);
					 
					 
					 float[] dataTest = new float[603*test.size()];
					 float[] dataTrain = new float[603*train.size()];
					 float[] labelsTest = new float[test.size()]; 
					 float[] labelsTrain = new float[train.size()];
					 
					//DataPreparation To run classifier
					 
					 String label = "";
					 int h = 0;
			         for(int i= 0;i < test.size() ;i++)
			         {
			        	 
			        	 String[] str = test.get(i).replace("\"", "").replace("[", "").replace("]", "").split(",");
			        	 if(str[str.length-1].replace(" ", "").contains("0"))
			        	 {
			        	     label = "0";
			        	     labelsTest[i] = 0;
			        	 }
			        	 else
			        	 {
			        		 label = "1";
			        		 labelsTest[i] = 1;
			        	 }
			        	 
			        	 String[] testArray = Arrays.copyOf(str, 603);
			        	 for(int k= str.length-1;k<603;k++)
			        	 {
			        		 testArray[k]="1";
			        	 }
			        	 
			        	 
			        	 dataTest[h] = Float.parseFloat(label);
			        	 h = h + 1;			        	 
			        	 for(int k=0;k<602;k++)
			        	 {
				        	 if(testArray[k] == null || testArray[k].isEmpty() || testArray[k].replace("-","").isEmpty())
				        		 testArray[k] = "1";
			        		 dataTest[h] =  Float.parseFloat(testArray[k]);
			        		 h = h + 1;	
			        	 }
			         }
			        
			         h = 0;
			         label = "";
			         for(int i= 0;i < train.size() ;i++)
			         {
			        	 
			        	 String[] str = train.get(i).replace("\"", "").replace("[", "").replace("]", "").split(",");
			        	 
			        	 if(str[4].replace(" ", "").contains("0"))
			        	 {
			        	     label = "0";
			        	     labelsTrain[i] = 0;
			        	 }
			        	 else
			        	 {
			        		 label = "1";
			        		 labelsTrain[i] = 1;
			        	 }
			        	 
			        	 String[] trainArray = Arrays.copyOf(str, 603);
			        	 for(int k= str.length-1;k<603;k++)
			        	 {
			        		 trainArray[k]="1";
			        	 }
			        	 
			        	 dataTrain[h] = Float.parseFloat(label);
			        	 h = h + 1;			        	 
			        	 for(int k=0;k<602;k++)
			        	 {
			        		 if(trainArray[k] == null || trainArray[k].isEmpty() || trainArray[k].replace("-","").isEmpty())
			        			 trainArray[k] = "1";
			        		 dataTrain[h] =  Float.parseFloat(trainArray[k]);
			        		 h = h + 1;	
			        	 }
			         }
					 
			         
			         /*for(int u = 0 ;u < test.size();u++)
			         {
			        	 System.out.println(test.get(u));
			         }
			         System.out.println("test.length..."+test.size());*/
			         
					 int numColumn = 603;
			         //DMatrix testMat = new DMatrix(rowHeadersTest, colIndexTest, dataTest, DMatrix.SparseType.CSR, numColumn);
			         //DMatrix trainMat = new DMatrix(rowHeadersTrain, colIndexTrain, dataTrain, DMatrix.SparseType.CSR, numColumn);
			         DMatrix testMat = new DMatrix(dataTest,test.size(), numColumn);
			         DMatrix trainMat = new DMatrix(dataTrain,train.size() , numColumn);
			         testMat.setLabel(labelsTest);
			         trainMat.setLabel(labelsTrain);
			      
			 		Map<String, Object> params = new HashMap<String, Object>() {
			 			  {
			 			    put("eta", 1.0);
			 			    put("max_depth", 2);
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
			 				int nround = 2;
			 				Booster booster = XGBoost.train(trainMat, params, nround, watches, null, null);
			 				booster.saveModel("model.bin");
			 				// dump with feature map
			 				//String[] model_dump_with_feature_map = booster.getModelDump("featureMap.txt", false);
			 			       //booster = XGBoost.loadModel("model.bin");
			 			    
			 				float[][] predicts = booster.predict(testMat);
			 				System.out.println("predicts..." + predicts[0][0] + " for class 1 and "+predicts[0][1]+"for class 0... The real label was:"+testMat.getLabel()[0]);
			 				//System.out.println(exmp.checkPredicts(predicts, predicts));
			 				// predict leaf
			 				//float[][] leafPredicts = booster.predictLeaf(testMat, 0);
			 				
			 				float[] statisticInfo = confusionMatrix(testMat,predicts);
			 				System.out.println("accuracy:"+statisticInfo[0] + "...."+
			 						"precision:"+statisticInfo[1]+"....."+
			 						"errorRate:"+statisticInfo[2]+"...."+
			 						"recall:"+statisticInfo[3]+"....."+
			 						"FScore:"+statisticInfo[4]);
					 
					 				 
					 	
}
//-------------------------------------------------------------------------------------------------------
public float[] confusionMatrix(DMatrix testMat,float[][] predicts) throws XGBoostError
{
			float[] statisticInfo= new float[5];
			float noNo = 0; float noYes = 0; float yesNo = 0; float yesYes = 0;
			float predictedValue = 2;
			for(int i = 0;i < predicts.length;i++)
			{
				if(predicts[i][0] > predicts[i][1])
					predictedValue = 1;
				else
					predictedValue = 0;
			    if(testMat.getLabel()[i] == 1 && predictedValue == 1)
			    	yesYes =+ 1;
			    else if(testMat.getLabel()[i] == 0 && predictedValue == 0)
			    	noNo =+ 1;
			    else if(testMat.getLabel()[i] == 0 && predictedValue == 1)
			    	noYes =+ 1;
			    else if(testMat.getLabel()[i] == 1 && predictedValue == 0)
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
//-------------------------------------------------------------------------------------------------------

public Map<String, List<String>> GetCommunityIds(ArrayList<Integer> PageIDs)
{
		Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"));
		Integer skip=0;
		Map<String, List<String>> dicPageCommunity = new HashMap<String, List<String>>();
		
		try (Session session = driver.session()) {
		 Integer numEdges = Integer.parseInt(session.run("MATCH (a:page)-[r:LINKS_TO]->() RETURN COUNT(r)").next().get("COUNT(r)").toString().replace("\"",""));
		 System.out.println(numEdges);
		 while (skip<numEdges){
		 StatementResult listOfIds = session.run("MATCH (a:page) RETURN a.PageID,a.CommunityID SKIP {skip} LIMIT 1000; ", Values.parameters("skip",skip));
		 listOfIds.stream().forEach(cand->{
			 
            if (PageIDs.contains(Integer.parseInt(cand.get("a.PageID").toString().replace("\"",""))))
            		{
		                List<String> pageIdList = new ArrayList<String>();
		                if (dicPageCommunity.get(cand.get("a.CommunityID").toString()) == null)
		                {
		                    pageIdList.add(cand.get("a.PageID").toString());
		                }
		                else
		                {
		                    pageIdList = dicPageCommunity.get(cand.get("a.CommunityID").toString());
		                    pageIdList.add(cand.get("a.PageID").toString());				                    		
		                }
		                dicPageCommunity.put(cand.get("a.CommunityID").toString(),pageIdList);
            		}
                    		
	 
		 });
		 
		 skip+=1000;
		 }
		 session.close();
		}
		driver.close();
		
		return dicPageCommunity;
}
//-----------------------------------------------------------------------------------------------------

public List<Pair<Integer, Integer>> cartesian(ArrayList<Integer> array1) {

		List<Pair<Integer, Integer>> partnerPlatformPairList = new ArrayList<Pair<Integer, Integer>>();
		
		for (int i = 0; i < array1.size(); i++)
		for (int j = 0; j < array1.size(); j++)
			if(partnerPlatformPairList.contains(new Pair(array1.get(i), array1.get(j))) == false)
		       partnerPlatformPairList.add(new Pair<Integer, Integer>(array1.get(i), array1.get(j)));
		
		return partnerPlatformPairList;

}

//------------------------------------------------------------------------------------------------------

public boolean ComparingPageCommunityId(Map<String,List<String>> pageCommunityDictionary,Integer pageId1, Integer pageId2)
{
		String communityId1 = "";
		String communityId2 = "";
			for (Map.Entry<String,List<String>> entry : pageCommunityDictionary.entrySet())  
			{
				        if(entry.getValue().contains(pageId1.toString()))
				            communityId1 = entry.getKey();
				        if(entry.getValue().contains(pageId2.toString()))
				        	communityId2 = entry.getKey();
			}
			
			if(communityId1 == communityId2)
		        return true;
		    else
		    	return false;
}
//----------------------------------------------------------------------------------------------------------

}

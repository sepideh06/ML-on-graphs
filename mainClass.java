package example;

import java.io.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import org.bouncycastle.util.Arrays.Iterator;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Values;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.procedure.PerformsWrites;
import org.neo4j.procedure.UserFunction;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import ml.dmlc.xgboost4j.java.DMatrix;

import org.javatuples.Pair;

import ml.dmlc.xgboost4j.java.XGBoostError;

public class mainClass {

	
	public static void main(String[] args) throws XGBoostError, IOException {
		// TODO Auto-generated method stub
		example.javaModelTrain instance = new example.javaModelTrain();	
		example.OutDBSplit instance1 = new example.OutDBSplit();
		example.AuthorPaperModel authorPaperModel = new example.AuthorPaperModel();
		example.OutDBAuthorPaperModel OutDBAuthorPaperModel = new example.OutDBAuthorPaperModel();
		example.FeatureDistribution featureDistribution = new FeatureDistribution();
		example.AuthorPaperModelConfusionMatrix authorpaperModelConfusionMatrix = new example.AuthorPaperModelConfusionMatrix();
		//instance.CreateModel();
		 //instance.CreateSecondModel("modelTag");
		 //instance.ModelInference("modelTag", "1,2,3,4,5,....");
		 // instance.splitData(0.67f,"Tag");
		//instance1.OutDataBaseSplitAndModelCreation(0.67f, "Tag");
		 // authorPaperModel.splitAuthorData(1978, 2010,"Tag");
		// authorPaperModel.CreateAuthorPaperModel("smellsliketeenspirit",true, true, true, true, true);
		//authorPaperModel.AuthorPaperModelInference("modelTag", "1,6,10,2005,1,1,1,2");
		//OutDBAuthorPaperModel.OutDBAuthorPaperModelCreation("smellsliketeenspirit", true, true, true, true, true);
		//List<String> dataList = new ArrayList<String>();
		//dataList.add("0,2222865828,2635296556,0,-1,0,-1,0");
		//dataList.add("0,2222865828,2635296556,0,8,0,4,0");
		 OutDBAuthorPaperModel.OutDBAuthorPaperModelInference();
		//featureDistribution.FeatureDistributionCalculation("smellsliketeenspirit");
		//featureDistribution.FeatureDistributionCalculation("Tag");
		//authorpaperModelConfusionMatrix.createModelBasedOnFile(true, true, true, true, true);
	}
	
	

}

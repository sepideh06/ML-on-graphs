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
		//instance.CreateModel();
		//instance.CreateSecondModel("modelTag");
		 instance.ModelInference("modelTag", "1,2,3,4,5,....");
		//instance.splitData(0.67f,"Tag");
	}
	/*
		public static void main(String[] args) throws IOException, XGBoostError {
	    // load file from text file, also binary buffer generated by xgboost4j
	    DMatrix trainMat = new DMatrix("train.txt.train");
	    DMatrix testMat = new DMatrix("test.txt.test");

	    HashMap<String, Object> params = new HashMap<String, Object>();
	    params.put("eta", 1.0);
	    params.put("max_depth", 2);
	    params.put("silent", 1);
	    params.put("objective", "binary:logistic");


	    HashMap<String, DMatrix> watches = new HashMap<String, DMatrix>();
	    watches.put("train", trainMat);
	    watches.put("test", testMat);

	    //set round
	    int round = 2;

	    //train a boost model
	    Booster booster = XGBoost.train(trainMat, params, round, watches, null, null);

	    //predict
	    float[][] predicts = booster.predict(testMat);

	    //save model to modelPath
	    File file = new File("./model");
	    if (!file.exists()) {
	      file.mkdirs();
	    }

	    String modelPath = "./model/xgb.model";
	    booster.saveModel(modelPath);

	    //dump model with feature map
	    String[] modelInfos = booster.getModelDump("C:\\Users\\IS PC\\Desktop\\neo4j-procedure-template-3.5\\featmap.txt", false);
	    exmp.saveDumpModel("./model/dump.raw.txt", modelInfos);

	    //save dmatrix into binary buffer
	    testMat.saveBinary("./model/dtest.buffer");

	    //reload model and data
	    Booster booster2 = XGBoost.loadModel("./model/xgb.model");
	    DMatrix testMat2 = new DMatrix("./model/dtest.buffer");
	    float[][] predicts2 = booster2.predict(testMat2);


	    //check the two predicts
	    System.out.println(exmp.checkPredicts(predicts, predicts2));

	    System.out.println("start build dmatrix from csr sparse data ...");
	    //build dmatrix from CSR Sparse Matrix
	    DataLoader.CSRSparseData spData = DataLoader.loadSVMFile("C:\\Users\\IS PC\\Desktop\\neo4j-procedure-template-3.5\\train.txt.train");

	    DMatrix trainMat2 = new DMatrix(spData.rowHeaders, spData.colIndex, spData.data,
	            DMatrix.SparseType.CSR);
	    trainMat2.setLabel(spData.labels);

	    //specify watchList
	    HashMap<String, DMatrix> watches2 = new HashMap<String, DMatrix>();
	    watches2.put("train", trainMat2);
	    watches2.put("test", testMat2);
	    Booster booster3 = XGBoost.train(trainMat2, params, round, watches2, null, null);
	    float[][] predicts3 = booster3.predict(testMat2);

	    //check predicts
	    System.out.println(exmp.checkPredicts(predicts, predicts3));
	}	
*/
}

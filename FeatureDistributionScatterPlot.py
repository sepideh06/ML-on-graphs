import csv
import matplotlib.pyplot as plt





def CreatingDistributionGraphForTestExistAndNonExist():

    x = []
    y = []
    #----------------------------Common Neighbour-----------------------------------------------------------------
    with open("FeatureCalculation1\TestExistCommonNeighbour.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.scatter(x, y, c='r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TestNonExistCommonNeighbour.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.scatter(x, y, c = 'g',label='Non-Exists')


    plt.title('Common Neighbour-Test Data-Exist&NonExist')
    plt.legend()
    plt.show()


           #-------------------------------------------Shortest Path in Co-Author Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TestExistShortestPathAuthorGraph.csv", 'r') as csvfile:
     plots = csv.reader(csvfile, delimiter=',')
     for row in plots:
        x.append(float(row[0]))
        y.append(int(row[1]))

    plt.clf()
    plt.scatter(x, y, c ='r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TestNonExistShortestPathAuthorGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.scatter(x, y,c = 'g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('ShortestPath in Co-Author Graph-Test Data-Exist&NonExist')
    plt.legend()
    plt.show()

    # -------------------------------------------Second Shortest Path in Co-Author Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TestExistSecondShortestPathAuthorGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.clf()
    plt.scatter(x, y, c ='r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TestNonExistSecondShortestPathAuthorGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.scatter(x, y, c ='g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('Second ShortestPath in Co-Author Graph-Test Data-Exist&NonExist')
    plt.legend()
    plt.show()

    # -------------------------------------------Shortest Path in Citation Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TestExistShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.clf()
    plt.scatter(x, y,c = 'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TestNonExistShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.scatter(x, y, c ='g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('ShortestPath in Citation Graph-Test Data-Exist&NonExist')
    plt.legend()
    plt.show()

    # -------------------------------------------Second Shortest Path in Citation Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TestExistSecondShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.clf()
    plt.scatter(x, y,c = 'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TestNonExistSecondShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.scatter(x, y, c ='g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('Second ShortestPath in Citation Graph-Test Data-Exist&NonExist')
    plt.legend()
    plt.show()

#---------------------------------------------------------------------------------------------------------------------------------------
def CreatingDistributionGraphForTrainExistAndNonExist():

    x = []
    y = []
    #----------------------------Common Neighbour-----------------------------------------------------------------
    with open("FeatureCalculation1\TrainExistCommonNeighbour.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.scatter(x, y,c ='r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TrainNonExistCommonNeighbour.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.scatter(x, y, c ='g',label='Non-Exists')

    plt.title('Common Neighbour-Train Data-Exist&NonExist')
    plt.legend()
    plt.show()


           #-------------------------------------------Shortest Path in Co-Author Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TrainExistShortestPathAuthorGraph.csv", 'r') as csvfile:
     plots = csv.reader(csvfile, delimiter=',')
     for row in plots:
        x.append(float(row[0]))
        y.append(int(row[1]))

    plt.clf()
    plt.scatter(x, y,c = 'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TrainNonExistShortestPathAuthorGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.scatter(x, y,c = 'g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('ShortestPath in Co-Author Graph-Train Data-Exist&NonExist')
    plt.legend()
    plt.show()

    # -------------------------------------------Second Shortest Path in Co-Author Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TrainExistSecondShortestPathAuthorGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.clf()
    plt.scatter(x, y, c ='r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TrainNonExistSecondShortestPathAuthorGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.scatter(x, y, c ='g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('Second ShortestPath in Co-Author Graph-Train Data-Exist&NonExist')
    plt.legend()
    plt.show()

    # -------------------------------------------Shortest Path in Citation Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TrainExistShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.clf()
    plt.scatter(x, y,c = 'r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TrainNonExistShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.scatter(x, y,c = 'g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('ShortestPath in Citation Graph-Train Data-Exist&NonExist')
    plt.legend()
    plt.show()

    # -------------------------------------------Second Shortest Path in Citation Graph----------------------------------------------------------------
    x = []
    y = []
    with  open("FeatureCalculation1\TrainExistSecondShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.clf()
    plt.scatter(x, y, c ='r', label='Exists')

    x = []
    y = []
    with open("FeatureCalculation1\TrainNonExistSecondShortestPathCitationGraph.csv", 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        for row in plots:
            x.append(float(row[0]))
            y.append(int(row[1]))

    plt.scatter(x, y,c = 'g', label='Non-Exists')
    plt.ylabel('count')
    plt.xlabel('TotalCost')

    plt.title('Second ShortestPath in Citation Graph-Train Data-Exist&NonExist')
    plt.legend()
    plt.show()

#---------------------------------------------------------------------------------------------------------------------------------------
#SplitTestExistNonExist()
#SplitTrainExistNonExist()
CreatingDistributionGraphForTestExistAndNonExist()
CreatingDistributionGraphForTrainExistAndNonExist()
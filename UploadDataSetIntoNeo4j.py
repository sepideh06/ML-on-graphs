from timeit import default_timer as timer
from neo4j.v1 import GraphDatabase, basic_auth


driver = GraphDatabase.driver('bolt://localhost:7687',auth=basic_auth("neo4j", "123"))


def main():
    with driver.session() as session:

        Q = "USING PERIODIC COMMIT 10000 LOAD CSV WITH HEADERS FROM 'file:///nodes_embedded.csv' AS line MERGE  (p:page {PageID: toInteger(line.PageID), Name: line.Name,CommunityID: line.CommunityID,Embedding: line.Embedding,LABEL: line.LABEL})"
        results = session.run(Q)

        #Q = "USING PERIODIC COMMIT 500 LOAD CSV WITH HEADERS FROM 'file:///relationship.csv' AS csvLine MATCH (page1: page {PageID: toInteger(csvLine.StartID)}), (page2: page {PageID: toInteger(csvLine.EndID)}) CREATE UNIQUE (page1)- [:LINKS_TO {TYPE: csvLine.TYPE}]-> (page2)"
        #results = session.run(Q)

        #Q = 'MATCH (p:page) RETURN p'
        #Q = 'MATCH p=()-[r:LINKS_TO]->() RETURN p'
        #results = session.run(Q)
        #for r in results:
            #print(r)

if __name__ == '__main__':
    start = timer()
    main()
    end = timer() - start
    print("Time to complete:", end)

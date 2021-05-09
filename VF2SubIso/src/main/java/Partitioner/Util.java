package Partitioner;

import AmazonStorage.S3Storage;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import infra.*;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import util.ConfigParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Util {

    public static void savePartitionMapping(String path, HashMap<String,Integer> mapping)
    {
        String sb= mapping.keySet().stream().map(key -> key + "\t" + mapping.get(key) + "\n").collect(Collectors.joining());
        try
        {
            if (ConfigParser.Amazon)
            {
                //TODO: Need to check if the path is correct (should be in the form of bucketName/Key )
                String bucketName = path.substring(0, path.lastIndexOf("/"));
                String key = path.substring(path.lastIndexOf("/") + 1);

                S3Storage.upload(bucketName,key,sb);
            }
            else
            {
                FileWriter writer = new FileWriter(path);
                writer.write(sb);
                writer.flush();
                writer.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    public static HashMap<String,Integer> loadPartitionMapping(String path)
    {
        HashMap<String,Integer> mapping=new HashMap<>();
        S3Object fullObject = null;
        BufferedReader br;
        try
        {
            if(ConfigParser.Amazon)
            {
                AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                        .withRegion(ConfigParser.region)
                        .build();
                //TODO: Need to check if the path is correct (should be in the form of bucketName/Key )
                String bucketName=path.substring(0,path.lastIndexOf("/"));
                String key=path.substring(path.lastIndexOf("/")+1);
                System.out.println("Downloading the object from Amazon S3 - Bucket name: " + bucketName +" - Key: " + key);
                fullObject = s3Client.getObject(new GetObjectRequest(bucketName, key));

                br = new BufferedReader(new InputStreamReader(fullObject.getObjectContent()));
            }
            else
            {
                br = new BufferedReader(new FileReader(path));
            }
            String line= br.readLine();
            while (line!=null) {
                String[] temp = line.split("\t");
                if (temp.length == 2) {
                    mapping.put(temp[0],Integer.parseInt(temp[1]));
                }
                line=br.readLine();
            }

            if(fullObject!=null)
                fullObject.close();
            br.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return mapping;
    }

    public static VF2DataGraph getSubgraphToSendToOtherNodes(VF2DataGraph dataGraph, List<TGFD> tgfds)
    {
        Graph<Vertex, RelationshipEdge> subgraph = new DefaultDirectedGraph<>(RelationshipEdge.class);
        List<Vertex> vertices=new ArrayList<>();
        List<String> validTypes=new ArrayList<>();
        tgfds.forEach(tgfd -> tgfd.getPattern().getPattern().vertexSet().stream().map(Vertex::getTypes).forEach(validTypes::addAll));

        for (Vertex v:dataGraph.getGraph().vertexSet()) {
            if(!Collections.disjoint(v.getTypes(),validTypes))
                vertices.add(v);
        }

        for (Vertex source:vertices) {
            for (RelationshipEdge e:dataGraph.getGraph().outgoingEdgesOf(source)) {
                if(vertices.contains(e.getTarget()))
                    subgraph.addEdge(e.getSource(),e.getTarget(),e);
            }
        }

        return new VF2DataGraph(subgraph);
    }

    public static void mergeGraphs(VF2DataGraph base, VF2DataGraph inputGraph)
    {
        inputGraph.getGraph()
                .vertexSet()
                .stream()
                .map(v -> (DataVertex) v)
                .forEach(v -> {
                    DataVertex currentVertex = (DataVertex) base.getNode(v.getVertexURI());
                    if (currentVertex == null) {
                        base.addVertex(v);
                    } else {
                        currentVertex.deleteAllAttributes();
                        currentVertex.setAllAttributes(v.getAllAttributesList());
                        v.getTypes().forEach(currentVertex::addType);
                    }
                });
        inputGraph.getGraph()
                .edgeSet()
                .forEach(e -> {
                    DataVertex src = (DataVertex) e.getSource();
                    DataVertex dst = (DataVertex) e.getTarget();
                    boolean exist = base.getGraph()
                        .outgoingEdgesOf(e.getSource())
                        .stream()
                        .anyMatch(edge -> edge.getLabel().equals(e.getLabel()) &&
                            ((DataVertex) edge.getTarget()).getVertexURI().equals(dst.getVertexURI()));
                if (!exist)
                    base.addEdge(src, dst, e);
                });
    }

}

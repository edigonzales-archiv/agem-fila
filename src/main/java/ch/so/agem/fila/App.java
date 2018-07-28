package ch.so.agem.fila;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.graph.build.feature.FeatureGraphGenerator;
import org.geotools.graph.build.line.BasicLineGraphGenerator;
import org.geotools.graph.build.line.LineGraphGenerator;
import org.geotools.graph.build.line.LineStringGraphGenerator;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.structure.basic.BasicNode;
import org.geotools.graph.structure.line.BasicXYNode;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class App {
	
	public static void main(String[] args) {
		final Logger log = LoggerFactory.getLogger(App.class);
		
        Map<String, Object> params = new HashMap<>();
        params.put("dbtype", "postgis");
        params.put("host", "192.168.50.6");
        params.put("port", 5432);
        params.put("database", "pub");
        params.put("schema", "agi_mopublic_pub");
        params.put("user", "ddluser");
        params.put("passwd", "ddluser");

        try {
			DataStore dataStore = DataStoreFinder.getDataStore(params);
			log.info(dataStore.toString());
			
			FeatureSource featureSource = dataStore.getFeatureSource("mopublic_strassenachse");
//			FeatureCollection featureCollection = featureSource.getFeatures();
			
			// 2544 = Feldbrunnen	        
			Filter filter = CQL.toFilter("bfs_nr = 2544");
			FeatureCollection fc = featureSource.getFeatures(filter);

			LineStringGraphGenerator lineStringGen = new LineStringGraphGenerator();
			FeatureGraphGenerator featureGen = new FeatureGraphGenerator(lineStringGen);

			// build graph
			FeatureIterator iter = fc.features();
			try {
				while (iter.hasNext()) {
					Feature feature = iter.next();
					featureGen.add( feature );
				}
			} finally {
				iter.close();
			}
			Graph graph = featureGen.getGraph();
			
			
//			Collection<BasicXYNode> nodes = graph.getNodes();
//			for (Iterator<BasicXYNode> n = nodes.iterator(); n.hasNext();) {
//				BasicXYNode node = n.next();
//
////				BasicXYNode nodeXY =  new BasicXYNode();
////				nodeXY.setObject(node.getObject());
//			
//				System.out.println(node.getEdges());
////				node.getID()
//			}
			
			
			// shortest path
			Node start = (Node)graph.getNodes().toArray()[0]; 
			log.info(String.valueOf(start.getID()));

//			BasicXYNode start = new BasicXYNode();
//			start.setCoordinate(new Coordinate(2606928.042, 1232310.864));

			EdgeWeighter weighter = new DijkstraIterator.EdgeWeighter() {
				public double getWeight(Edge e) {
					SimpleFeature feature = (SimpleFeature) e.getObject();
					Geometry geometry = (Geometry) feature.getDefaultGeometry();
					return geometry.getLength();
				}
			};
			
			DijkstraShortestPathFinder pf = new DijkstraShortestPathFinder(graph, start, weighter);
			pf.calculate();

			List<Node> destinations = new ArrayList<Node>();
			Node destination = (Node)graph.getNodes().toArray()[4]; 
			log.info(String.valueOf(destination.getID()));

//			dest.setCoordinate(new Coordinate(2607138.093, 1231354.206));
			destinations.add(destination);

	        File newFile = new File("/Users/stefan/tmp/fubar.shp");

	        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
	        Map<String, Serializable> shpParams = new HashMap<>();
	        shpParams.put("url", newFile.toURI().toURL());
	        shpParams.put("create spatial index", Boolean.TRUE);

	        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(shpParams);
	        newDataStore.createSchema((SimpleFeatureType) fc.getSchema());
	        String typeName = newDataStore.getTypeNames()[0];
	        SimpleFeatureSource shpFeatureSource = newDataStore.getFeatureSource(typeName);
	        SimpleFeatureStore shpFeatureStore = (SimpleFeatureStore) shpFeatureSource;
	        
	        
	        ArrayList features = new ArrayList();
			//calculate the paths
			for (Iterator itd = destinations.iterator(); itd.hasNext(); ) {
				Node dest = (Node) itd.next();
				System.out.println(dest.toString());
				Path path = pf.getPath(dest);
				System.out.println(path);
				System.out.println(path.getEdges());
				
				List<Edge> edges = path.getEdges();
				for(Iterator ite = edges.iterator(); ite.hasNext();) {
					Edge edge = (Edge) ite.next();
					SimpleFeature feature = (SimpleFeature) edge.getObject();
					log.info(feature.getDefaultGeometry().toString());
					features.add(feature);
				}

			  //do something with the path
			}
	        SimpleFeatureCollection collection = new ListFeatureCollection(newDataStore.getSchema(), features);
	        Transaction transaction = new DefaultTransaction("create");			       
	        shpFeatureStore.setTransaction(transaction);
	        shpFeatureStore.addFeatures(collection);
	        transaction.commit();
			
		} catch (IOException | CQLException e) {
			e.printStackTrace();
		}

		
		
		
		
		log.info("Hallo Welt.");
	}

}

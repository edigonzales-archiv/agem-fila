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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultRepository;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.feature.type.BasicFeatureTypes;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
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
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import ch.so.agem.fila.service.RoadAxisService;
import ch.so.agem.fila.service.ShortestPathService;

public class App {
	
	public static void main(String[] args) {
		final Logger log = LoggerFactory.getLogger(App.class);

		String dbhost;
		String dbport;
		String dbdatabase;
		String dbschema;
		String dbusr;
		String dbpwd;
		String bfsnr;
		Map<String,String> config;

		Options options = new Options();
		Option dbhostOpt = Option.builder().required(true).longOpt("dbhost").hasArg().build();		
		Option dbportOpt = Option.builder().required(false).longOpt("dbport").hasArg().build();		
		Option dbdatabaseOpt = Option.builder().required(true).longOpt("dbdatabase").hasArg().build();		
		Option dbschemaOpt = Option.builder().required(true).longOpt("dbschema").hasArg().build();		
		Option dbusrOpt = Option.builder().required(true).longOpt("dbusr").hasArg().build();		
		Option dbpwdOpt = Option.builder().required(true).longOpt("dbpwd").hasArg().build();		
		Option bfsnrOpt = Option.builder().required(true).longOpt("bfsnr").hasArg().build();
		
		options.addOption(dbhostOpt);
		options.addOption(dbportOpt);
		options.addOption(dbdatabaseOpt);
		options.addOption(dbschemaOpt);
		options.addOption(dbusrOpt);
		options.addOption(dbpwdOpt);
		options.addOption(bfsnrOpt);
		
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse( options, args );
			dbhost = line.getOptionValue("dbhost");
			dbport = line.hasOption("dbport") ? line.getOptionValue("dbport") : "5432";
			dbdatabase = line.getOptionValue("dbdatabase");
			dbschema = line.getOptionValue("dbschema");
			dbusr = line.getOptionValue("dbusr");
			dbpwd = line.getOptionValue("dbpwd");
			bfsnr = line.getOptionValue("bfsnr");
			
			// Create DataStore Repository.
			DefaultRepository dataStoreRepo = new DefaultRepository();

	        Map<String, Object> postgresParams = new HashMap<>();
	        postgresParams.put("dbtype", "postgis");
	        postgresParams.put("host", dbhost);
	        postgresParams.put("port", Integer.valueOf(dbport));
	        postgresParams.put("database", dbdatabase);
//	        params.put("schema", dbschema);
	        postgresParams.put("user", dbusr);
	        postgresParams.put("passwd", dbpwd);

	        DataStore postgresDataStore= DataStoreFinder.getDataStore(postgresParams);
	        dataStoreRepo.register("PostGIS", postgresDataStore);
	        
	        Map<String, Object> shpParams = new HashMap<>();
	        File shpFile =  new File("/Users/stefan/tmp/shortest_path.shp");
	        shpParams.put("url", shpFile.toURI().toURL());
	        shpParams.put("create spatial index", Boolean.TRUE);

	        DataStore shpDataStore = DataStoreFinder.getDataStore(shpParams);
	        dataStoreRepo.register("ShortestPath", shpDataStore);

			// Create list with line segments of the road axis converted from linestrings.
			RoadAxisService roadAxisService = new RoadAxisService();
			List<LineSegment> segments = roadAxisService.getRoadAxisLineSegmentsByBfsNr(dataStoreRepo, bfsnr, true);
			
			// Calculate shortest path.
			ShortestPathService shortestPathService =  new ShortestPathService(segments);
			FeatureCollection fc = shortestPathService.getShortestPath(new Coordinate(2609479.200, 1229755.719), 
					new Coordinate(2608638.754, 1230122.502));
			
			// funktioniert
//			FeatureCollection fc = shortestPathService.getShortestPath(new Coordinate(2608865.137, 1230676.868), 
//					new Coordinate(2609097.942, 1230223.469));

			// funktioniert nicht mehr -> ausserhalb Gemeinde...
//			FeatureCollection fc = shortestPathService.getShortestPath(new Coordinate(2608865.137, 1230676.868), 
//					new Coordinate(2609098.793, 1230211.395));
			
			// Add shortest path feature collection to shapefile.
			shpDataStore.createSchema((SimpleFeatureType) fc.getSchema());
			
			Transaction transaction = new DefaultTransaction("create");

	        String typeName = shpDataStore.getTypeNames()[0];
	        SimpleFeatureSource featureSource = shpDataStore.getFeatureSource(typeName);
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                        
	        featureStore.setTransaction(transaction);
	        try {
                featureStore.addFeatures(fc);
                transaction.commit();

            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                transaction.rollback();
            } finally {
                transaction.close();
            }
		} catch (ParseException e) {
			e.printStackTrace();
			log.error(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			log.error(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		log.info("Hallo Welt.");
	}
}

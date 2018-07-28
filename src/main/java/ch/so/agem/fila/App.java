package ch.so.agem.fila;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.graph.build.line.BasicLineGraphGenerator;
import org.geotools.graph.build.line.LineGraphGenerator;
import org.geotools.graph.structure.Graph;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


			
			
		} catch (IOException | CQLException e) {
			e.printStackTrace();
		}

		
		
		
		
		log.info("Hallo Welt.");
	}

}

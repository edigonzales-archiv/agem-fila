package ch.so.agem.fila.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DefaultRepository;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

public class RoadAxisService {
	final Logger log = LoggerFactory.getLogger(this.getClass());
		
//	RoadAxisRepository roadAxisRepo = new RoadAxisRepository();
	
	public List<LineSegment> getRoadAxisLineSegmentsByBfsNr(DefaultRepository dataStoreRepo, String bfsnr, boolean unique) throws Exception {
		Map<LineSegment,LineSegment> segments = new HashMap<LineSegment,LineSegment>();
		
		DataStore dataStore = dataStoreRepo.dataStore("PostGIS");
		
		String sql = "SELECT t_id, bfs_nr, geometrie FROM agi_mopublic_pub.mopublic_strassenachse";
		VirtualTable vt = new VirtualTable("vt_mopublic_strassenachse", sql);
		((JDBCDataStore) dataStore).createVirtualTable(vt);
		
		FeatureSource featureSource = dataStore.getFeatureSource("vt_mopublic_strassenachse");
		Filter filter = CQL.toFilter("bfs_nr = " + bfsnr);
		FeatureCollection fc = featureSource.getFeatures(filter);
		
		FeatureIterator it = fc.features();
		try {
			while (it.hasNext()) {
				Feature feature = it.next();
				LineString line = (LineString) ((GeometryAttribute)feature.getDefaultGeometryProperty()).getValue();
				
				Coordinate[] coords = line.getCoordinates();
				for (int i=0; i<coords.length-1; i++) {
//					log.info("**");
//					log.info(coords[i].toString());
//					log.info(coords[i+1].toString());
//					log.info("--");
					
					LineSegment seg = new LineSegment(coords[i], coords[i+1]);
					seg.normalize();
					if (!segments.containsKey(seg)) {
						segments.put(seg, seg);
					}
				}
			}
		} finally {
			it.close();
			dataStore.dispose();
		}
		log.info("Number of segments: " + segments.size());
		return new ArrayList<LineSegment>(segments.values());
	}

}

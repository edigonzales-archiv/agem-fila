package ch.so.agem.fila.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.BasicFeatureTypes;
import org.geotools.graph.build.line.BasicLineGraphGenerator;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.structure.basic.BasicNode;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;

public class ShortestPathService {
	final Logger log = LoggerFactory.getLogger(this.getClass());

	private List<LineSegment> segments;
	BasicLineGraphGenerator basicLineGenerator = new BasicLineGraphGenerator();
	Graph graph;
	Map<Coordinate, BasicNode> nodes = new HashMap<Coordinate, BasicNode>();
	EdgeWeighter weighter;
	DijkstraShortestPathFinder pf;
	
	SimpleFeatureType OUTPUT_TYPE;
	
	public ShortestPathService(List<LineSegment> segments) throws NoSuchAuthorityCodeException, FactoryException {
		this.segments = segments;
		this.initGraph();
		this.createFeatureType();
	}
	
	private void createFeatureType() throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("LineSegment");
		builder.add("node_a", Integer.class);
		builder.add("node_b", Integer.class);
		
		CRSAuthorityFactory factory = CRS.getAuthorityFactory(true);
		CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("EPSG:2056");
		builder.setCRS(crs);
		builder.add(BasicFeatureTypes.GEOMETRY_ATTRIBUTE_NAME, LineString.class );

		OUTPUT_TYPE = builder.buildFeatureType();
	}
	
	private void initGraph() {
		for (LineSegment segment : segments) {
			basicLineGenerator.add(segment);
		}
		
		graph = basicLineGenerator.getGraph();
		
		Iterator it = graph.getNodes().iterator();
		while (it.hasNext()) {
			BasicNode node = (BasicNode) it.next();
			nodes.put((Coordinate) node.getObject(), node);
		}
		
		weighter = new DijkstraIterator.EdgeWeighter() {
			public double getWeight(Edge e) {
				LineSegment segment = (LineSegment) e.getObject();
				return segment.getLength();
			}
		};
	}
	
	// TODO: Doof, da "pf" immer wieder neu berechnet werden muss. Kommt sowieso darauf an, was eigentlich genau 
	// berechnet werden soll: 
	// - Mittelpunkt im Baugebiet?
	// - Nächster Punkt auf Kantonsstrasse?
	// - ...
	public FeatureCollection getShortestPath(Coordinate startCoord, Coordinate destinationCoord) {
		log.info("Start at: " + startCoord.toString());
		log.info("Destination at: " + destinationCoord.toString());
		
		// Find corresponding nodes.
		Node start = nodes.get(startCoord);
		Node destination = nodes.get(destinationCoord);
		
		log.info("Start edges: " + start.getEdges().toString());
		List<Edge> startEdges = start.getEdges();
		for (Edge e : startEdges) {
			log.info(((LineSegment)e.getObject()).toString());
		}
		
		log.info("Destination edges: " + destination.getEdges().toString());
		List<Edge> Destination = destination.getEdges();
		for (Edge e : Destination) {
			log.info(((LineSegment)e.getObject()).toString());
		}

		// Calculate ALL shortest paths to/from start.
		pf = new DijkstraShortestPathFinder(graph, start, weighter);
		pf.calculate();
		
		log.info("Cost: " + pf.getCost(destination));
		
		// Get shortest path start - destination.
		Path path = pf.getPath(destination);
		log.info(path.toString());
		
		// Create feature collection with the linesegments of the shortest path.
		DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
		List<Edge> edges = path.getEdges(); 
		for (Edge e : edges) {
//			log.info(((LineSegment)e.getObject()).toString());
//			log.info(e.getNodeA().toString());
//			log.info(e.getNodeB().toString());
			
			SimpleFeatureBuilder builder = new SimpleFeatureBuilder(OUTPUT_TYPE);
			builder.set("node_a", e.getNodeA().getID());
			builder.set("node_b", e.getNodeB().getID());
			GeometryFactory factory = new GeometryFactory();
			LineString line = ((LineSegment)e.getObject()).toGeometry(factory);
			builder.set(BasicFeatureTypes.GEOMETRY_ATTRIBUTE_NAME, line);
			
			SimpleFeature feature = builder.buildFeature(null);
//			log.info(feature.toString());
			featureCollection.add(feature);
		}
		return featureCollection;
	}
	
	
}

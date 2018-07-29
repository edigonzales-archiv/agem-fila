package ch.so.agem.fila.service;

import java.util.List;

import org.geotools.graph.build.line.BasicLineGraphGenerator;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.LineSegment;

public class ShortestPathService {
	final Logger log = LoggerFactory.getLogger(this.getClass());

	private List<LineSegment> segments;
	BasicLineGraphGenerator basicLineGenerator = new BasicLineGraphGenerator();
	Graph graph;
	EdgeWeighter weighter;
	DijkstraShortestPathFinder pf;
	
	public ShortestPathService(List<LineSegment> segments) {
		this.segments = segments;
		this.initGraph();
	}
	
	private void initGraph() {
		for (LineSegment segment : segments) {
			basicLineGenerator.add(segment);
		}
		
		graph = basicLineGenerator.getGraph();
		
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
	public void getShortestPath(Node start, Node destination) {
		pf = new DijkstraShortestPathFinder(graph, start, weighter);
		pf.calculate();
		
		Path path = pf.getPath(destination);

	}
	
	
}

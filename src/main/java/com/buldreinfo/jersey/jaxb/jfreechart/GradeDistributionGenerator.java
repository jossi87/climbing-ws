package com.buldreinfo.jersey.jaxb.jfreechart;

import java.awt.Color;
import java.awt.SystemColor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.DefaultCategoryDataset;

import com.buldreinfo.jersey.jaxb.model.GradeDistribution;

public class GradeDistributionGenerator {
	public static byte[] write(Collection<GradeDistribution> gradeDistribution) throws IOException {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (GradeDistribution x : gradeDistribution) {
			dataset.addValue(x.getNum(), "row", x.getGrade());
		}
		
		JFreeChart chart = ChartFactory.createBarChart("Distribution", // chart title
                null, // domain axis label
                null, // range axis label
                dataset, // data
                PlotOrientation.VERTICAL, // orientation
                false, // include legend
                false, // tooltips?
                false // URLs?
                ); 
		chart.setBackgroundPaint(Color.WHITE);
		chart.setBorderVisible(false);
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setBackgroundPaint(SystemColor.inactiveCaption);//change background color
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setBarPainter(new StandardBarPainter());
        for (int i = 0; i < gradeDistribution.size(); i++) {
        	renderer.setSeriesPaint(i, Color.WHITE);
        }
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        ItemLabelPosition position = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.TOP_CENTER);
        renderer.setDefaultPositiveItemLabelPosition(position); 
        
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ChartUtils.writeChartAsPNG(baos, chart, 800, 300);
			return baos.toByteArray();
		}
	}
}

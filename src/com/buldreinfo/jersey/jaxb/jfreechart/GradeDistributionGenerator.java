package com.buldreinfo.jersey.jaxb.jfreechart;

import java.awt.Color;
import java.awt.SystemColor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class GradeDistributionGenerator {
	public static void main(String[] args) throws Exception {
		Gson gson = new Gson();
		URL obj = new URL("https://brattelinjer.no/com.buldreinfo.jersey.jaxb/v2/grade/distribution?idArea=2754&idSector=0");
		HttpURLConnection con = (HttpURLConnection)obj.openConnection();
		con.setRequestMethod("GET");
		List<GradeDistribution> gradeDistribution = gson.fromJson(new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")), new TypeToken<ArrayList<GradeDistribution>>(){}.getType());
		Path dst = Paths.get("c:/users/jostein/desktop/test.png");
		write(dst, gradeDistribution);
	}

	public static void write(Path dst, Collection<GradeDistribution> gradeDistribution) throws IOException {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for (GradeDistribution x : gradeDistribution) {
			dataset.addValue(x.getNum(), x.getGrade(), x.getGrade());
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
        renderer.setItemMargin(-6);
        renderer.setBarPainter(new StandardBarPainter());
        for (int i = 0; i < gradeDistribution.size(); i++) {
        	renderer.setSeriesPaint(i, Color.WHITE);
        }
        
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        ItemLabelPosition position = new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.TOP_CENTER);
        renderer.setDefaultPositiveItemLabelPosition(position); 
        
		try (OutputStream out = new FileOutputStream(dst.toFile())) {
			ChartUtils.writeChartAsPNG(out, chart, 800, 300);
		}
	}
}

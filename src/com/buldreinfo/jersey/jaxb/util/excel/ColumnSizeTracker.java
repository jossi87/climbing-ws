/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package com.buldreinfo.jersey.jaxb.util.excel;

import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.SheetUtil;

import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport.SheetFormula;
import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport.SheetHyperlink;
import com.google.common.collect.Maps;

/**
 * A fast alternative to the native auto-sizing of columns in Apache POI.
 * <p/>
 * Note that we completely disregard merged cells, as well as characters of different widths (or even negative width).
 * We also don't consider the possibility that the largest cell might be overwritten with a shorter value.
 * <p/>
 * This method decreased the time to write an Excel report from 3 minutes and 14 seconds, down to 7 seconds (27x faster)!
 */
public class ColumnSizeTracker {	
	private static final Logger logger = LogManager.getLogger();
	
    private static final char defaultChar = '0';
    private static final double fontHeightMultiple = 2.0;
    
    // Current context
    private static final FontRenderContext fontRenderContext = new FontRenderContext(null, true, true);
    
	private Map<CellStyle, String> deferredComputation = Maps.newHashMap();
	
	private final Supplier<FormulaEvaluator> evaluatorSupplier;

	private boolean disableEvaluation;
	private double minWidth = 0;
	
	/**
	 * Construct a column size tracker that uses a given evaluator to compute the cell values.
	 * @param evaluator the evaluator.
	 */
	public ColumnSizeTracker(Supplier<FormulaEvaluator> evaluatorSupplier) {
		// May be NULL
		this.evaluatorSupplier = evaluatorSupplier;
	}
	
	/**
	 * Register a cell and its style.
	 * @param cell the cell.
	 * @param style the cell style.
	 * @param value the value of the cell.
	 */
	public void registerCell(Cell cell, CellStyle style, Object value) {
		String oldValue = deferredComputation.get(style);
		
		// See if the existing value is already larger
		if (oldValue != null && value instanceof String && oldValue.length() >= ((String)value).length()) {
			return;
		}
		String newValue = getLongestLine(cell, value);
		
		if (oldValue == null || (newValue != null && newValue.length() > oldValue.length())) {
			deferredComputation.put(style, newValue);
		}
	}
	
	/**
	 * Set the minimum width this column tracker can return.
	 * @param minWidth the minimum width.
	 */
	public void setMinWidth(double minWidth) {
		this.minWidth = minWidth;
	}
	
	/**
	 * Retrieve the minimum width this column tracker can return.
	 * @return The minimum width.
	 */
	public double getMinWidth() {
		return minWidth;
	}
	
	/**
	 * Retrieve the longest line.
	 * @param cell the cell.
	 * @param value the value.
	 * @return Longest line in the value.
	 */
	protected String getLongestLine(Cell cell, Object value) {
		// Some values will never have multiple lines
		if (value instanceof Boolean) {
			return value.toString().toUpperCase(Locale.ROOT);
		} else if (value instanceof Number) {
			return value.toString();
		} else if (value instanceof SheetHyperlink) {
			return ((SheetHyperlink) value).getLabel();			
		} else if (value == null) {
			return "";
		} else if (value instanceof SheetFormula) {
			FormulaEvaluator formulaEvaluator = evaluatorSupplier != null ? evaluatorSupplier.get() : null;
			
			// Determine if evaluator is available
			if (formulaEvaluator != null) {
				try {
					// Note that the cell content has already been set
					if (!disableEvaluation) {
						CellValue cellValue = formulaEvaluator.evaluate(cell);
						return getLongestLine(cellValue != null ? cellValue.formatAsString() : null);
					}
				} catch (NotImplementedException e) {
					disableEvaluation = true;
					logger.debug("Unable to compute formula result.", e);
				}
			}
		} else {
			return getLongestLine(value.toString());	
		}
		
		// Unable to get the longest line
		return "";
	}
	
	private String getLongestLine(String multiLine) {
		int bestStart = 0; 	  // Inclusive
		int bestEnd = -1;     // Exclusive
		int bestLength = 0;
		int prevEnd = -1;
		
		// Count the last character too
		for (int i = 0; i <= multiLine.length(); i++) {
			if (i == multiLine.length() || multiLine.charAt(i) == '\n') {
				// [prevEnd]  [prevEnd+1]    [prevEnd+2]    i
				//    \n		   A			  B		   \n
				int currentLength = i - prevEnd - 1;
				
				if (currentLength > bestLength) {
					bestStart = prevEnd + 1;
					bestEnd = i;
					bestLength = currentLength;
				}
				prevEnd = i;
			}
		}

		if (bestLength > 0) {
			String result = multiLine.substring(bestStart, bestEnd);
			return result;
		} else {
			return "";
		}
	}
	 
	/**
	 * Retrieve the number of tracked styles.
	 * @return The style count.
	 */
	public int getStyles() {
		return deferredComputation.size();
	}
	
	/**
	 * Compute the maximum width given all the cells registered.
	 * @return The maximum width in character units.
	 */
	public double computeMaxWidth(Workbook workbook) {
		double width = -1;
		float defaultCharWidth = SheetUtil.getDefaultCharWidthAsFloat(workbook);
		
		for (CellStyle style : deferredComputation.keySet()) {
			String longestLine = deferredComputation.get(style);
			
			// Ignore empty lines
			if (longestLine == null) {
				continue;
			}
			// Fetch default style
			if (style == null) {
				style = workbook.getCellStyleAt(0);
			}
            String txt = longestLine + defaultChar;
            AttributedString str = new AttributedString(txt);
            
            Font font = workbook.getFontAt(style.getFontIndex());
            copyAttributes(font, str, 0, txt.length());
            
            // Compute width
            width = getCellWidth(defaultCharWidth, 1, style, width, str);
		}
		return Math.min(255, width);
	}

    /**
     * Calculate the best-fit width for a cell.
     * @param defaultCharWidth the width of a character using the default font in a workbook
     * @param colspan the number of columns that is spanned by the cell (1 if the cell is not part of a merged region)
     * @param style the cell style, which contains text rotation and indention information needed to compute the cell width
     * @param width the minimum best-fit width. This algorithm will only return values greater than or equal to the minimum width.
     * @param str the text contained in the cell
     * @return the best fit cell width
     */
    private static double getCellWidth(float defaultCharWidth, int colspan,
            CellStyle style, double minWidth, AttributedString str) {
        TextLayout layout = new TextLayout(str.getIterator(), fontRenderContext);
        final Rectangle2D bounds;
        if(style.getRotation() != 0){
            /*
             * Transform the text using a scale so that it's height is increased by a multiple of the leading,
             * and then rotate the text before computing the bounds. The scale results in some whitespace around
             * the unrotated top and bottom of the text that normally wouldn't be present if unscaled, but
             * is added by the standard Excel autosize.
             */
            AffineTransform trans = new AffineTransform();
            trans.concatenate(AffineTransform.getRotateInstance(style.getRotation()*2.0*Math.PI/360.0));
            trans.concatenate(
            AffineTransform.getScaleInstance(1, fontHeightMultiple)
            );
            bounds = layout.getOutline(trans).getBounds();
        } else {
            bounds = layout.getBounds();
        }
        // frameWidth accounts for leading spaces which is excluded from bounds.getWidth()
        final double frameWidth = bounds.getX() + bounds.getWidth();
        final double width = Math.max(minWidth, ((frameWidth / colspan) / defaultCharWidth) + style.getIndention());
        return width;
    }
	
    /**
     * Copy text attributes from the supplied Font to Java2D AttributedString
     */
    private static void copyAttributes(Font font, AttributedString str, int startIdx, int endIdx) {
        str.addAttribute(TextAttribute.FAMILY, font.getFontName(), startIdx, endIdx);
        str.addAttribute(TextAttribute.SIZE, (float)font.getFontHeightInPoints());
        
        if (font.getBold()) {
        	str.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, startIdx, endIdx);
        }
        if (font.getItalic()) {
        	str.addAttribute(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE, startIdx, endIdx);
        }
        if (font.getUnderline() == Font.U_SINGLE) { 
        	str.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, startIdx, endIdx);
        }
    }
}
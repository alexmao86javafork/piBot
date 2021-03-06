package au.com.rsutton.calabrate;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.stat.regression.SimpleRegression;

public class LineHelper
{

	public LineHelper() throws IOException, InterruptedException, BrokenBarrierException
	{

	}

	enum LinePointAnalysis
	{
		TO_SHORT, IS_A_LINE, TOO_FEW_POINTS, NOT_A_LINE
	}

	static final int MIN_POINTS = 4;
	static final int ACCURACY_MULTIPIER = 5;

	public List<List<Vector3D>> scanForAndfindLines(List<Vector3D> points) throws InterruptedException,
			BrokenBarrierException, IOException
	{

		// find crisp edges in the point scan
		List<Integer> edges = findEdges(points, 30);

		// break the list of points up into segments divided by the crisp edges
		// to aid in accurately finding the ends of the lines
		List<List<Vector3D>> segments = new LinkedList<>();

		int start = 0;
		for (int i : edges)
		{
			segments.add(points.subList(start, i));
			start = i;
		}
		if (start < points.size() - 1)
		{
			segments.add(points.subList(start, points.size() - 1));
		}

		// iterate segements looking for lines
		List<List<Vector3D>> lines = new LinkedList<>();
		for (List<Vector3D> segment : segments)
		{
			lines.addAll(findLines(segment, 7));
		}

		return lines;
	}

	private List<Integer> findEdges(List<Vector3D> points, int edgeStepSize) throws InterruptedException,
			BrokenBarrierException, IOException
	{
		List<Integer> edges = new LinkedList<>();
		List<Integer> values = new LinkedList<>();
		int i = 0;
		for (Vector3D point : points)
		{
			int value = (int) Vector3D.distance(new Vector3D(0, 0, 0), point);
			values.add(value);
			if (values.size() > 6)
			{
				int av1 = (values.get(0) + values.get(1) + values.get(2)) / 3;
				int av2 = (values.get(3) + values.get(4) + values.get(5)) / 3;
				if (Math.abs(av1 - values.get(0)) < av1 * 0.1 && av2 > av1 + edgeStepSize)
				{
					System.out.println("Edge at " + i + " dist " + values.get(2));
					edges.add(i - 3);
				}

				values.remove(0);
			}
			i++;
		}
		return edges;

	}

	/**
	 * finds lines within the list of points
	 * 
	 * @param points
	 * @param accuracy
	 * @return
	 */
	List<List<Vector3D>> findLines(List<Vector3D> points, double accuracy)
	{

		List<List<Vector3D>> lines = new LinkedList<>();
		int start = 0;
		int end = start + MIN_POINTS;

		LinePointAnalysis previousState = LinePointAnalysis.NOT_A_LINE;
		while (start < points.size() && end < points.size())
		{
			List<Vector3D> subList = points.subList(start, end);
			LinePointAnalysis analysis = isALine(subList, accuracy);
			end++;

			// ignore TOO_FEW_POINTS and TO_SHORT, as we have increased the end
			// already
			if (analysis == LinePointAnalysis.NOT_A_LINE || analysis == LinePointAnalysis.IS_A_LINE)
			{
				if (analysis == LinePointAnalysis.NOT_A_LINE)
				{
					// not a line, doh
					if (previousState == LinePointAnalysis.IS_A_LINE)
					{
						// but it was a line without the last point, yay lets
						// record
						// the details of the line we found
						end -= 2;
						List<Vector3D> linePoints = new LinkedList<>();
						linePoints.addAll(points.subList(start, end + 1));
						lines.add(linePoints);

						// start looking for a new line at the end of this one
						start = end;

					} else
					{
						// it's not a line, it can't be a line even if we add
						// more points... so move the start point up one and
						// start over
						start++;
					}
					end = start + MIN_POINTS;
				}

				previousState = analysis;
			}

		}
		return lines;

	}

	/**
	 * determines if the list of points constitutes a line
	 * 
	 * @param points
	 * @param accuracy
	 * @return
	 */
	LinePointAnalysis isALine(List<Vector3D> points, double accuracy)
	{
		if (points.size() < MIN_POINTS)
		{
			return LinePointAnalysis.TOO_FEW_POINTS;
		}
		double length = Vector3D.distance(points.get(0), points.get(points.size() - 1));
		if (length < accuracy * ACCURACY_MULTIPIER)
		{
			return LinePointAnalysis.TO_SHORT;
		}

		SimpleRegression regression = new SimpleRegression();
		for (Vector3D point : points)
		{
			regression.addData(point.getX(), point.getY());

		}
		if (regression.getInterceptStdErr() < 1.0 && regression.getSlopeStdErr() < 1.0)
		{
			return LinePointAnalysis.IS_A_LINE;
		}
		return LinePointAnalysis.NOT_A_LINE;

	}

}

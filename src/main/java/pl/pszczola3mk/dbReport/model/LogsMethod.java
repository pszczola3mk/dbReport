package pl.pszczola3mk.dbReport.model;

import java.io.Serializable;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class LogsMethod implements Serializable {

	private static final long serialVersionUID = -2336629737993396472L;

	public LogsMethod(String methodName, int durationInMilis) {
		this.methodName = methodName;
		this.maxDuration = durationInMilis;
		this.avgDuration = durationInMilis;
		this.minDuration = durationInMilis;
		this.invokeCount = 1;
		this.summaryTime = durationInMilis;
	}
	private String methodName;
	private int maxDuration;
	private double avgDuration;
	private int minDuration;
	private int invokeCount;
	private long summaryTime;

	public void increase(int durationInMilis) {
		this.invokeCount = this.invokeCount + 1;
		this.maxDuration = Math.max(maxDuration, durationInMilis);
		this.minDuration = Math.min(minDuration, durationInMilis);
		this.summaryTime = this.summaryTime + durationInMilis;
		this.avgDuration = (double) this.summaryTime / (double) this.invokeCount;
	}
}

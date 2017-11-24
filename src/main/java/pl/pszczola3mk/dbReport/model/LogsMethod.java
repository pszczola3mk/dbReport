package pl.pszczola3mk.dbReport.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "invokeList")
public class LogsMethod implements Serializable {

	private static final long serialVersionUID = -2336629737993396472L;

	public LogsMethod(String beanName, String methodName, String personId, String params, Date invokeDate) {
		this.methodName = methodName;
		this.beanName = beanName;
		if (personId != null) {
			invokeList.add(new MethodInvoke(beanName, methodName, personId, params, -1,invokeDate));
		}
	}
	private String beanName;
	private String methodName;
	private List<MethodInvoke> invokeList = new ArrayList<>();
	private int maxDuration = 0;
	private double avgDuration = 0.0;
	private int minDuration = 0;
	private int invokeCount = 0;
	private long summaryTime = 0L;

	public void increase(int durationInMilis, String personId) {
		if (this.invokeCount == 0) {
			this.maxDuration = durationInMilis;
			this.avgDuration = durationInMilis;
			this.minDuration = durationInMilis;
			this.summaryTime = durationInMilis;
			this.invokeCount = 1;
		} else {
			this.invokeCount = this.invokeCount + 1;
			this.maxDuration = Math.max(maxDuration, durationInMilis);
			this.minDuration = Math.min(minDuration, durationInMilis);
			this.summaryTime = this.summaryTime + durationInMilis;
			this.avgDuration = (double) this.summaryTime / (double) this.invokeCount;
		}
		if (personId != null) {
			this.invokeList.stream().filter(m -> m.getPersonId().equals(personId) && m.getMethodName().equals(this.methodName) && m.getBeanName().equals(this.beanName) && m.getDurationInMilis() == -1)
					.forEach(m -> m.setDurationInMilis(durationInMilis));
		}
	}
}

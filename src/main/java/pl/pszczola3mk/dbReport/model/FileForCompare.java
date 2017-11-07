package pl.pszczola3mk.dbReport.model;

import java.io.Serializable;
import lombok.Data;

@Data
public class FileForCompare implements Serializable {

	private static final long serialVersionUID = -7586596457041062526L;
	private String fileName;
	private LogsMethod logsMethod;
	private String beanName;
	private String methodName;
	private int maxDuration;
	private double avgDuration;
	private int minDuration;
	private int invokeCount;
	private long summaryTime;

	public void setLogsMethod(LogsMethod logsMethod) {
		this.logsMethod = logsMethod;
		if (logsMethod != null) {
			this.beanName = logsMethod.getBeanName();
			this.methodName = logsMethod.getMethodName();
			this.maxDuration = logsMethod.getMaxDuration();
			this.avgDuration = logsMethod.getAvgDuration();
			this.minDuration = logsMethod.getMinDuration();
			this.invokeCount = logsMethod.getInvokeCount();
			this.summaryTime = logsMethod.getSummaryTime();
		}
	}
}

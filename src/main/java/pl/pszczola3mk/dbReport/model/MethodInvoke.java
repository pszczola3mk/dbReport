package pl.pszczola3mk.dbReport.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MethodInvoke implements Serializable {

	private static final long serialVersionUID = -8348418565740618043L;

	public MethodInvoke(String beanName, String methodName, String personId, String params, int durationInMilis, Date invokeDate) {
		this.methodName = methodName;
		this.beanName = beanName;
		this.personId = personId;
		this.params = params;
		this.durationInMilis = durationInMilis;
		this.invokeDate = invokeDate;
	}
	private String methodName;
	private String personId;
	private String params;
	private int durationInMilis;
	private String beanName;
	private Date invokeDate;

	public String getInvokeDateFormated() {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss,SSS yyyy.MM.dd");
		return sdf.format(this.invokeDate);
	}
}

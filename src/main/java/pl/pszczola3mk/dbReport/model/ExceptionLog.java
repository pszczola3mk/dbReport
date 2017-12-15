package pl.pszczola3mk.dbReport.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class ExceptionLog implements Serializable {

	private static final long serialVersionUID = -8682403975359804121L;
	private List<Date> invokeDates = new ArrayList<>();
	private String shortMessage;
	private String message;
	private int invokeCount = 0;
	private String level;

	public Date getFirstInvokeDate() {
		Date minDate = null;
		if (this.invokeDates != null && this.invokeDates.size() > 0) {
			minDate = this.invokeDates.stream().min(Date::compareTo).get();
		}
		return minDate;
	}
}

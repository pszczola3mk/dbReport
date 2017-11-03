package pl.pszczola3mk.dbReport.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class FileForCompare implements Serializable {

	private static final long serialVersionUID = -7586596457041062526L;
	private String fileName;
	private LogsMethod logsMethod;
}

package pl.pszczola3mk.dbReport.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Entity implements Serializable {

	private static final long serialVersionUID = 5771225154717283722L;
	private String name;
	private List<String> columns = new ArrayList<>();

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getColumns() {
		return this.columns;
	}

	public void setColumns(List<String> columns) {
		this.columns = columns;
	}
}

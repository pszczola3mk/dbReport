package pl.pszczola3mk.dbReport.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportCreatorDBService {

	private static final Logger log = LoggerFactory.getLogger(ReportCreatorDBService.class);
	@Autowired
	private JdbcTemplate template;

	public void checkConnection(String url, String userName, String password) {
		org.apache.tomcat.jdbc.pool.DataSource dataSource = (org.apache.tomcat.jdbc.pool.DataSource) template.getDataSource();
		dataSource.setPassword(password);
		dataSource.setUrl("jdbc:postgresql://" + url);
		dataSource.setUsername(userName);
		dataSource.close();
		log.info("checkConnection - start");
		template.query("SELECT 1", (rs, rowNum) -> rs.getInt(1)).forEach(check -> log.info("Db connection: " + check.toString()));
		log.info("checkConnection - stop");
	}
}

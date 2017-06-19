package pl.pszczola3mk.dbReport.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import pl.pszczola3mk.dbReport.Application;

@RunWith(SpringRunner.class)
@ComponentScan(basePackageClasses = { ReportCreatorDBService.class })
@TestPropertySource(locations = "classpath:application_test.properties")
@ContextConfiguration(classes=Application.class)
public class ReportCreatorDBServiceTest {
	private static final Logger log = LoggerFactory.getLogger(ReportCreatorDBServiceTest.class);

	@Autowired
	private ReportCreatorDBService service;

	@Test
	public void checkConnectionTest() {
		log.info("ReportCreatorDBServiceTest checkConnectionTest - start");
		this.service.checkConnection();
		log.info("ReportCreatorDBServiceTest checkConnectionTest - stop");
	}
}

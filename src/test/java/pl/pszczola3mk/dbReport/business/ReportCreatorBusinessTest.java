package pl.pszczola3mk.dbReport.business;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pszczola3mk.dbReport.service.ReportCreatorDBService;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ReportCreatorBusinessTest {

	private static final Logger log = LoggerFactory.getLogger(ReportCreatorBusinessTest.class);
	@InjectMocks
	private ReportCreatorBusiness business;
	@Mock
	private ReportCreatorDBService service;

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void checkConnectionTest() {
		log.info("ReportCreatorBusinessTest checkConnectionTest - start");
		this.business.checkConnection();
		verify(service, times(1)).checkConnection();
		log.info("ReportCreatorBusinessTest checkConnectionTest - stop");
	}
}

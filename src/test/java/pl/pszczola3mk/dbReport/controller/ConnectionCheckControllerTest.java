package pl.pszczola3mk.dbReport.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import pl.pszczola3mk.dbReport.business.ReportCreatorBusiness;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application_test.properties")
@RunWith(SpringRunner.class)
public class ConnectionCheckControllerTest {

	private static final Logger log = LoggerFactory.getLogger(ConnectionCheckControllerTest.class);
	@Autowired
	@InjectMocks
	protected TestRestTemplate restTemplate;
	@MockBean
	private ReportCreatorBusiness business;

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void checkConnectionTest() {
		log.info("ReportCreatorBusinessTest checkConnectionTest - start");
		String body = this.restTemplate.getForObject("/api/checkConnectionDB?url=test&userName=pszczola&password=psz", String.class);
		verify(business, times(1)).checkConnection("test","pszczola","psz");
		assertThat(body).isEqualTo("DB Success");
		log.info("ReportCreatorBusinessTest checkConnectionTest - stop");
	}
}

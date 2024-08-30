package io.bpoole6.accumulator;

import io.bpoole6.accumulator.controller.MetricsController;
import io.bpoole6.accumulator.controller.response.ServiceDiscovery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TestControllers extends BasicTest {

  @Autowired
  private MetricsController controller;

  @Test
  public void testServiceDiscovery(){
    ResponseEntity<List<ServiceDiscovery>> entity = controller.serviceDiscovery();
    List<ServiceDiscovery> sds = entity.getBody();
    Assertions.assertTrue(sds.size() == 1);
    Assertions.assertEquals(3, sds.get(0).getLabels().size());
    Assertions.assertEquals("qa", sds.get(0).getLabels().get("env"));
    Assertions.assertEquals("v45", sds.get(0).getLabels().get("version"));
  }


}

package io.bpoole6.accumulator.service.metricgroup;

import java.util.Map;
import lombok.Data;

@Data
public class Root {
  private Global global;
  private Map<String, Group> metricGroups;

}

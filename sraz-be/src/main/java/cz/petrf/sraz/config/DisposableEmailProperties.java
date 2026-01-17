package cz.petrf.sraz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.disposable")
@Data
public class DisposableEmailProperties {
  private List<String> blocklistUrls;
  private long updateIntervalHours;
}
package rebar.graph.core;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class AbstractGraphModuleTest {

	@Test
	public void testFullScanEnabled() {
		AbstractGraphModule m = new AbstractGraphModule() {
			
			@Override
			public void run() {
				
				
			}
		};
		
		Assertions.assertThat(m.isFullScanEnabled(Optional.empty())).isTrue();
		Assertions.assertThat(m.isFullScanEnabled(Optional.of(""))).isTrue();
		Assertions.assertThat(m.isFullScanEnabled(Optional.of("5 "))).isTrue();
		Assertions.assertThat(m.isFullScanEnabled(Optional.of("500"))).isTrue();
		
		Assertions.assertThat(m.isFullScanEnabled(Optional.of("-1"))).isFalse();
		Assertions.assertThat(m.isFullScanEnabled(Optional.of("disabled"))).isFalse();
		Assertions.assertThat(m.isFullScanEnabled(Optional.of(" -1 "))).isFalse();
		Assertions.assertThat(m.isFullScanEnabled(Optional.of(" -1"))).isFalse();
		Assertions.assertThat(m.isFullScanEnabled(Optional.of("0 "))).isFalse();
	}
	
	@Test
	public void testFullScanSecs() {
		AbstractGraphModule m = new AbstractGraphModule() {
			
			@Override
			public void run() {
				
				
			}
		};
		
		Assertions.assertThat(m.getFullScanInterval(null)).isEqualTo(300);
		Assertions.assertThat(m.getFullScanInterval(Optional.ofNullable(null))).isEqualTo(300);
		Assertions.assertThat(m.getFullScanInterval(Optional.of(""))).isEqualTo(300);
		Assertions.assertThat(m.getFullScanInterval(Optional.of("  "))).isEqualTo(300);
		Assertions.assertThat(m.getFullScanInterval(Optional.of(" 62 "))).isEqualTo(62);
		
		Assertions.assertThat(m.getFullScanInterval(Optional.of(" 30 "))).isEqualTo(60);
		Assertions.assertThat(m.getFullScanInterval(Optional.of("diabled"))).isEqualTo(300L);
	}

}

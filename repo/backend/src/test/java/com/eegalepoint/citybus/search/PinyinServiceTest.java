package com.eegalepoint.citybus.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PinyinServiceTest {

  private final PinyinService pinyinService = new PinyinService();

  @Test
  void toPinyin_mapsCommonTransitCharacters() {
    assertThat(pinyinService.toPinyin("\u7AD9")).isEqualTo("zhan"); // 站
    assertThat(pinyinService.toPinyin("\u8DEF")).isEqualTo("lu"); // 路
  }

  @Test
  void toInitialsSimple_mapsLeadingCjkCharacters() {
    assertThat(pinyinService.toInitialsSimple("A\u7AD9B")).isNotBlank();
  }
}

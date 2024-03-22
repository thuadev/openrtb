/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.youdao.openrtb.snippet;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableList;
import com.youdao.openrtb.OpenRtb.BidRequest;
import com.youdao.openrtb.OpenRtb.BidRequest.Imp;
import com.youdao.openrtb.OpenRtb.BidResponse;
import com.youdao.openrtb.OpenRtb.BidResponse.SeatBid;
import com.youdao.openrtb.OpenRtb.BidResponse.SeatBid.Bid;
import com.youdao.openrtb.TestUtil;

import org.junit.Test;

import java.util.List;

/**
 * Tests for {@link SnippetProcessor}.
 */
public class SnippetProcessorTest {
  private static BidRequest req = BidRequest.newBuilder()
      .setId("1")
      .build();
  private static Bid.Builder bid = Bid.newBuilder()
      .setId("1")
      .setImpid("1")
      .setPrice(10000);
  private static BidResponse.Builder resp = BidResponse.newBuilder()
      .setId("1")
      .addSeatbid(SeatBid.newBuilder()
          .addBid(bid));
  private final SnippetProcessor processor = new SnippetProcessor() {
    @Override protected List<SnippetMacroType> registerMacros() {
      return ImmutableList.<SnippetMacroType>builder()
          .addAll(super.registerMacros())
          .addAll(asList(TestMacros.values()))
          .build();
    }

    @Override protected void processMacroAt(
        SnippetProcessorContext ctx, StringBuilder sb, SnippetMacroType macroDef) {
      sb.append("#");
    }
  };

  @Test
  public void testContext() {
    SnippetProcessorContext ctx = new SnippetProcessorContext(req, resp);
    ctx.setBid(bid);
    TestUtil.testCommonMethods(ctx);
    assertThat(ctx.request()).isSameAs(req);
    assertThat(ctx.response()).isSameAs(resp);
    assertThat(ctx.getBid()).isSameAs(bid);
  }

  @Test
  public void testNullProcessor() {
    SnippetProcessorContext ctx = new SnippetProcessorContext(req, resp);
    ctx.setBid(bid);
    String snippet = OpenRtbMacros.AUCTION_ID.key();
    assertThat(SnippetProcessor.NULL.process(ctx, snippet)).isSameAs(snippet);
  }

  @Test
  public void testUndefinedMacro1() {
    UndefinedMacroException e = new UndefinedMacroException(TestMacros.TEST);
    assertThat(e.key()).isSameAs(TestMacros.TEST);
  }

  @Test
  public void testUndefinedMacro2() {
    UndefinedMacroException e = new UndefinedMacroException(TestMacros.TEST, "msg");
    assertThat(e.key()).isSameAs(TestMacros.TEST);
  }

  @Test
  public void testUrlEncoding() {
    assertThat(process("")).isEqualTo("");
    assertThat(process("{!+/}")).isEqualTo("{!+/}");
    assertThat(process("%!+/%")).isEqualTo("%!+/%");
    assertThat(process("%{aaa}%")).isEqualTo(esc("aaa"));
    assertThat(process("%{!+/}%")).isEqualTo(esc("!+/"));
    assertThat(process("%{!+/}%%{aaa}%")).isEqualTo(esc("!+/") + esc("aaa"));
    assertThat(process("%{%{!+/}%aaa}%")).isEqualTo(esc2("!+/") + esc("aaa"));
    assertThat(process("%{%{%{%{%{%{%{%{%{%{!}%}%}%}%}%}%}%}%}%}%"))
        .isEqualTo(esc2(esc2(esc2(esc2(esc2("!"))))));
  }

  @Test
  public void testUrlEncodingBad() {
    assertThat(process("bad!}%")).isEqualTo("bad!}%");
    assertThat(process("bad!}%%{+}%")).isEqualTo("bad!}%" + esc("+"));
    assertThat(process("bad!%{")).isEqualTo("bad!");
    assertThat(process("%{bad!")).isEqualTo("bad!");
    assertThat(process("%{good!}%{bad!}%")).isEqualTo(esc("good!") + "{bad!}%");
  }

  @Test
  public void testMacro() {
    assertThat(processor.toString()).isNotNull();

    assertThat(process("${UNKNOWN_MACRO}")).isEqualTo("${UNKNOWN_MACRO}");
    assertThat(process(TestMacros.TEST.key())).isEqualTo("#");
    assertThat(process("%{" + TestMacros.TEST.key() + "}%")).isEqualTo(esc("#"));
  }

  private String process(String snippet) {
    return process(snippet, true);
  }

  private String process(String snippet, boolean full) {
    BidRequest request = BidRequest.newBuilder()
        .setId("1")
        .addImp(Imp.newBuilder()
            .setId("1")).build();
    BidResponse.Builder response = createBidResponse(snippet, full);
    SnippetProcessorContext ctx = new SnippetProcessorContext(request, response);
    ctx.setBid(response.getSeatbidBuilder(0).getBidBuilder(0));
    return processor.process(ctx, snippet);
  }

  private static BidResponse.Builder createBidResponse(String snippet, boolean full) {
    Bid.Builder bid = Bid.newBuilder()
        .setId("bid1")
        .setImpid("1")
        .setPrice(1000);
    if (full) {
      bid
          .setAdid("ad1")
          .setAdm(snippet)
          .setIurl("https://mycontent.com/creative.png");
    }
    return BidResponse.newBuilder()
        .setId("1")
        .addSeatbid(SeatBid.newBuilder()
            .setSeat("seat1")
            .addBid(bid));
  }

  private static String esc(String s) {
    return SnippetProcessor.getEscaper().escape(s);
  }

  private static String esc2(String s) {
    return esc(esc(s));
  }

  static enum TestMacros implements SnippetMacroType {
    TEST;

    @Override public String key() {
      return "${TEST}";
    }
  }
}

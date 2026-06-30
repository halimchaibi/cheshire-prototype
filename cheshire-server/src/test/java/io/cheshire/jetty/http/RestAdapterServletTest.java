/*-
 * #%L
 * Cheshire :: Servers
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.jetty.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class RestAdapterServletTest {

  @Test
  void sendErrorEscapesJsonErrorMessages() throws Exception {
    final var body = new StringWriter();
    final var response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(new PrintWriter(body));

    final var servlet = new RestAdapterServlet(new RestRequestHandler(request -> null));
    final Method sendError =
        RestAdapterServlet.class.getDeclaredMethod(
            "sendError", HttpServletResponse.class, int.class, String.class);
    sendError.setAccessible(true);

    sendError.invoke(servlet, response, 500, "bad \"quote\"\nand newline");

    final var json = new ObjectMapper().readTree(body.toString());
    assertThat(json.get("error").asText()).isEqualTo("bad \"quote\"\nand newline");
  }
}

// src/main/java/com/polarisoffice/secuone/support/GlobalModelAdvice.java
package com.polarisoffice.secuone.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Component
@ControllerAdvice
public class GlobalModelAdvice {

  private final DomainCustomerResolver resolver;

  public GlobalModelAdvice(DomainCustomerResolver resolver) {
    this.resolver = resolver;
  }

  @ModelAttribute
  public void exposeCustomerCode(HttpServletRequest req, Model model) {
    String code = resolver.resolveCustomerCode(req);
    if (code != null && !code.isBlank()) {
      model.addAttribute("domainCustomerCode", code);
    }
  }
}

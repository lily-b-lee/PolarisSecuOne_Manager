package com.polarisoffice.secuone.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {

  private final DomainCustomerResolver resolver;

  public GlobalModelAdvice(DomainCustomerResolver resolver) {
    this.resolver = resolver;
  }

  /**
   * 모든 뷰 모델에 customerCode 노출
   * - 우선순위: 쿼리파라미터(customerCode) > 도메인 유추 > "mg"
   */
  @ModelAttribute
  public void exposeCustomerCode(HttpServletRequest req, Model model) {
    String explicit = req.getParameter("customerCode");
    String code = resolver.resolveCustomerCode(req, explicit);
    model.addAttribute("customerCode", code);
  }
}
